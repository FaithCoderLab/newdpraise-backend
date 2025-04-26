package faithcoderlab.newdpraise.domain.song.service;

import faithcoderlab.newdpraise.domain.song.AudioFile;
import faithcoderlab.newdpraise.domain.song.AudioFileRepository;
import faithcoderlab.newdpraise.domain.song.dto.AudioDownloadResult;
import faithcoderlab.newdpraise.domain.song.dto.AudioFileDto;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.global.exception.ResourceAlreadyExistsException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AudioFileService {

  private final AudioFileRepository audioFileRepository;
  private final YoutubeDownloadService youtubeDownloadService;

  @Transactional
  public AudioFile saveAudioFile(AudioDownloadResult downloadResult, User uploader,
      String originalUrl) {
    if (audioFileRepository.existsByVideoId(downloadResult.getVideoId())) {
      throw new ResourceAlreadyExistsException("이미 다운로드된 오디오 파일입니다: " + downloadResult.getVideoId());
    }

    AudioFile audioFile = AudioFile.builder()
        .videoId(downloadResult.getVideoId())
        .title(downloadResult.getTitle())
        .artist(downloadResult.getArtist())
        .filePath(downloadResult.getFilePath())
        .fileName(downloadResult.getFileName())
        .fileSize(downloadResult.getFileSize())
        .mimeType(downloadResult.getMimeType())
        .extension(downloadResult.getExtension())
        .bitrate(downloadResult.getBitrate())
        .durationSeconds(downloadResult.getDurationSeconds())
        .thumbnailUrl(downloadResult.getThumbnailUrl())
        .originalUrl(originalUrl)
        .uploader(uploader)
        .build();

    return audioFileRepository.save(audioFile);
  }

  public AudioFile getAudioFileByVideoId(String videoId) {
    return audioFileRepository.findByVideoId(videoId)
        .orElseThrow(() -> new ResourceNotFoundException("오디오 파일을 찾을 수 없습니다: " + videoId));
  }

  public List<AudioFileDto> getUserAudioFiles(User uploader) {
    List<AudioFile> audioFiles = audioFileRepository.findByUploader(uploader);
    return audioFiles.stream()
        .map(this::mapToDto)
        .collect(Collectors.toList());
  }

  public Page<AudioFileDto> getUserAudioFiles(User uploader, Pageable pageable) {
    Page<AudioFile> audioFilePage = audioFileRepository.findByUploader(uploader, pageable);
    return audioFilePage.map(this::mapToDto);
  }

  public Page<AudioFileDto> searchAudioFiles(String keyword, Pageable pageable) {
    Page<AudioFile> audioFilePage = audioFileRepository.searchByKeyword(keyword, pageable);
    return audioFilePage.map(this::mapToDto);
  }

  @Transactional
  public boolean deleteAudioFile(String videoId, User uploader) {
    AudioFile audioFile = audioFileRepository.findByVideoIdAndUploaderId(videoId, uploader.getId())
        .orElseThrow(() -> new ResourceNotFoundException("오디오 파일을 찾을 수 없습니다: " + videoId));

    audioFileRepository.delete(audioFile);

    File file = new File(audioFile.getFilePath());
    return file.delete();
  }

  @Transactional
  public AudioFileDto updateAudioFileMetadata(String videoId, String title, String artist, User uploader) {
    AudioFile audioFile = audioFileRepository.findByVideoIdAndUploaderId(videoId, uploader.getId())
        .orElseThrow(() -> new ResourceNotFoundException("오디오 파일을 찾을 수 없습니다: " + videoId));

    if (title != null && !title.isBlank()) {
      audioFile.setTitle(title);
    }

    if (artist != null && !artist.isBlank()) {
      audioFile.setArtist(artist);
    }

    AudioFile updateFile = audioFileRepository.save(audioFile);
    return mapToDto(updateFile);
  }

  private AudioFileDto mapToDto(AudioFile audioFile) {
    return AudioFileDto.builder()
        .id(audioFile.getId())
        .videoId(audioFile.getVideoId())
        .title(audioFile.getTitle())
        .artist(audioFile.getArtist())
        .fileName(audioFile.getFileName())
        .fileSize(audioFile.getFileSize())
        .mimeType(audioFile.getMimeType())
        .extension(audioFile.getExtension())
        .bitrate(audioFile.getBitrate())
        .durationSeconds(audioFile.getDurationSeconds())
        .thumbnailUrl(audioFile.getThumbnailUrl())
        .downloadUrl(audioFile.getDownloadUrl())
        .uploaderId(audioFile.getUploader()!= null ? audioFile.getUploader().getId() : null)
        .createdAt(audioFile.getCreatedAt())
        .updatedAt(audioFile.getUpdatedAt())
        .build();
  }
}
