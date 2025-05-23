package faithcoderlab.newdpraise.domain.song.service;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import faithcoderlab.newdpraise.config.AppConfig;
import faithcoderlab.newdpraise.domain.song.dto.AudioDownloadResult;
import faithcoderlab.newdpraise.domain.song.dto.YoutubeVideoInfo;
import faithcoderlab.newdpraise.global.exception.YoutubeDownloadException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class YoutubeDownloadService {

  private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
      "(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})(?:[?&][^\\s]*)?");

  private final YoutubeDownloader youtubeDownloader;
  private final AppConfig appConfig;

  public YoutubeDownloadService(YoutubeDownloader youtubeDownloader, AppConfig appConfig) {
    this.youtubeDownloader = youtubeDownloader;
    this.appConfig = appConfig;
  }

  public boolean isValidYoutubeUrl(String url) {
    if (!StringUtils.hasText(url)) {
      return false;
    }
    return YOUTUBE_URL_PATTERN.matcher(url).matches();
  }

  public String extractVideoId(String url) {
    if (!StringUtils.hasText(url)) {
      return null;
    }
    Matcher matcher = YOUTUBE_URL_PATTERN.matcher(url);
    return matcher.find() ? matcher.group(1) : null;
  }

  public YoutubeVideoInfo getVideoInfo(String youtubeUrl) {
    String videoId = extractVideoId(youtubeUrl);
    if (videoId == null) {
      throw new YoutubeDownloadException("유효하지 않은 YouTube URL입니다: " + youtubeUrl);
    }

    try {
      RequestVideoInfo request = new RequestVideoInfo(videoId);
      Response<VideoInfo> response = youtubeDownloader.getVideoInfo(request);
      VideoInfo videoInfo = response.data();

      if (videoInfo == null) {
        throw new YoutubeDownloadException("비디오 정보를 가져올 수 없습니다: " + videoId);
      }

      return YoutubeVideoInfo.builder()
          .videoId(videoId)
          .title(videoInfo.details().title())
          .author(videoInfo.details().author())
          .lengthSeconds(Long.valueOf(videoInfo.details().lengthSeconds()))
          .thumbnailUrl(videoInfo.details().thumbnails().get(0))
          .viewCount(videoInfo.details().viewCount())
          .hasAudioFormats(!videoInfo.audioFormats().isEmpty())
          .build();
    } catch (Exception e) {
      throw new YoutubeDownloadException("YouTube 비디오 정보 가져오기 실패: " + e.getMessage(), e);
    }
  }

  public AudioDownloadResult downloadAudio(String youtubeUrl) {
    String videoId = extractVideoId(youtubeUrl);
    if (videoId == null) {
      throw new YoutubeDownloadException("유효하지 않은 YouTube URL입니다: " + youtubeUrl);
    }

    try {
      RequestVideoInfo infoRequest = new RequestVideoInfo(videoId);
      Response<VideoInfo> infoResponse = youtubeDownloader.getVideoInfo(infoRequest);
      VideoInfo videoInfo = infoResponse.data();

      if (videoInfo == null) {
        throw new YoutubeDownloadException("비디오 정보를 가져올 수 없습니다: " + videoId);
      }

      List<AudioFormat> audioFormats = videoInfo.audioFormats();
      if (audioFormats.isEmpty()) {
        throw new YoutubeDownloadException("사용 가능한 오디오 형식이 없습니다: " + videoId);
      }

      AudioFormat bestAudioFormat = audioFormats.stream()
          .max(Comparator.comparing(AudioFormat::averageBitrate))
          .orElse(audioFormats.get(0));

      if (bestAudioFormat.extension() == null) {
        throw new YoutubeDownloadException("오디오 파일 다운로드 실패: 지원되지 않는 오디오 형식입니다");
      }

      String downloadDir = appConfig.getFileUploadDir() + "/audio";
      Path downloadPath = Paths.get(downloadDir);
      if (!Files.exists(downloadPath)) {
        Files.createDirectories(downloadPath);
      }

      RequestVideoFileDownload downloadRequest = new RequestVideoFileDownload(bestAudioFormat)
          .saveTo(downloadPath.toFile())
          .renameTo(videoId + "." + bestAudioFormat.extension().value());

      Response<File> downloadResponse = youtubeDownloader.downloadVideoFile(downloadRequest);
      File downloadFile = downloadResponse.data();

      if (downloadFile == null || !downloadFile.exists()) {
        throw new YoutubeDownloadException("오디오 파일 다운로드 실패: " + videoId);
      }

      return AudioDownloadResult.builder()
          .videoId(videoId)
          .title(videoInfo.details().title())
          .artist(videoInfo.details().author())
          .filePath(downloadFile.getAbsolutePath())
          .fileName(downloadFile.getName())
          .fileSize(downloadFile.length())
          .mimeType("audio/" + bestAudioFormat.extension().value())
          .extension(bestAudioFormat.extension().value())
          .bitrate(bestAudioFormat.averageBitrate())
          .durationSeconds(videoInfo.details().lengthSeconds())
          .thumbnailUrl(videoInfo.details().thumbnails().get(0))
          .build();
    } catch (IOException e) {
      throw new YoutubeDownloadException("오디오 파일 저장 중 오류 발생: " + e.getMessage(), e);
    } catch (NullPointerException e) {
      throw new YoutubeDownloadException("오디오 파일 다운로드 실패: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new YoutubeDownloadException("YouTube 오디오 다운로드 실패: " + e.getMessage(), e);
    }
  }

  public List<String> getDownloadedAudioFiles() {
    String downloadDir = appConfig.getFileUploadDir() + "/audio";
    Path downloadPath = Paths.get(downloadDir);
    List<String> files = new ArrayList<>();

    try {
      if (Files.exists(downloadPath)) {
        Files.list(downloadPath)
            .filter(path -> !Files.isDirectory(path))
            .forEach(path -> files.add(path.getFileName().toString()));
      }
    } catch (IOException e) {
      throw new YoutubeDownloadException("다운로드된 파일 목록을 가져오는데 실패했습니다: " + e.getMessage(), e);
    }

    return files;
  }

  public boolean isAudioFileExists(String videoId) {
    if (!StringUtils.hasText(videoId)) {
      return false;
    }

    String downloadDir = appConfig.getFileUploadDir() + "/audio";
    Path downloadPath = Paths.get(downloadDir);

    try {
      if (Files.exists(downloadPath)) {
        return Files.list(downloadPath)
            .anyMatch(path -> path.getFileName().toString().startsWith(videoId + "."));
      }
    } catch (IOException e) {
      throw new YoutubeDownloadException("파일 확인 중 오류 발생: " + e.getMessage(), e);
    }

    return false;
  }

  public boolean deleteAudioFile(String videoId) {
    if (!StringUtils.hasText(videoId)) {
      return false;
    }

    String downloadDir = appConfig.getFileUploadDir() + "/audio";
    Path downloadPath = Paths.get(downloadDir);

    try {
      if (Files.exists(downloadPath)) {
        return Files.list(downloadPath)
            .filter(path -> path.getFileName().toString().startsWith(videoId + "."))
            .findFirst()
            .map(path -> {
              try {
                return Files.deleteIfExists(path);
              } catch (IOException e) {
                throw new YoutubeDownloadException("파일 삭제 중 오류 발생: " + e.getMessage(), e);
              }
            })
            .orElse(false);
      }
    } catch (IOException e) {
      throw new YoutubeDownloadException("파일 삭제 중 오류 발생: " + e.getMessage(), e);
    }

    return false;
  }
}
