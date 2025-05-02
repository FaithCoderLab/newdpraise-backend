package faithcoderlab.newdpraise.domain.song.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import faithcoderlab.newdpraise.config.AppConfig;
import faithcoderlab.newdpraise.domain.song.dto.AudioDownloadResult;
import faithcoderlab.newdpraise.domain.song.dto.YoutubeVideoInfo;
import faithcoderlab.newdpraise.global.exception.YoutubeDownloadException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YoutubeDownloadServiceTest {

  @Mock
  private YoutubeDownloader youtubeDownloader;

  @Mock
  private AppConfig appConfig;

  @Mock
  private Response<VideoInfo> videoInfoResponse;

  @Mock
  private Response<File> fileResponse;

  @Mock
  private VideoInfo videoInfo;

  @Mock
  private VideoDetails videoDetails;

  @Mock
  private AudioFormat audioFormat;

  @InjectMocks
  private YoutubeDownloadService youtubeDownloadService;

  @ParameterizedTest
  @DisplayName("유효한 유튜브 URL 검증 - 성공 케이스")
  @ValueSource(strings = {
      "https://www.youtube.com/watch?v=R9tUikvBv5M",
      "https://youtube.com/watch?v=R9tUikvBv5M",
      "https://youtu.be/R9tUikvBv5M",
      "https://www.youtube.com/watch?v=R9tUikvBv5M&feature=youtu.be",
      "https://youtu.be/R9tUikvBv5M?t=10"
  })
  void isValidYoutubeUrl_ValidURLs_ReturnsTrue(String url) {
    boolean result = youtubeDownloadService.isValidYoutubeUrl(url);
    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @DisplayName("유효하지 않은 유튜브 URL 검증 - 실패 케이스")
  @ValueSource(strings = {
      "",
      "https://www.google.com",
      "https://www.youtube.com",
      "https://youtube.com/channel/123456",
      "https://youtu.be/"
  })
  void isValidYoutubeUrl_InvalidURLs_ReturnsFalse(String url) {
    boolean result = youtubeDownloadService.isValidYoutubeUrl(url);
    assertThat(result).isFalse();
  }

  @ParameterizedTest
  @DisplayName("유튜브 URL에서 비디오 ID 추출 - 성공 케이스")
  @CsvSource({
      "https://www.youtube.com/watch?v=R9tUikvBv5M,R9tUikvBv5M",
      "https://youtube.com/watch?v=R9tUikvBv5M,R9tUikvBv5M",
      "https://youtu.be/R9tUikvBv5M,R9tUikvBv5M",
      "https://www.youtube.com/watch?v=R9tUikvBv5M&feature=youtu.be,R9tUikvBv5M",
      "https://youtu.be/R9tUikvBv5M?t=10,R9tUikvBv5M"
  })
  void extractVideoId_ValidURLs_ReturnsTrue(String url, String expectedId) {
    String result = youtubeDownloadService.extractVideoId(url);
    assertThat(result).isEqualTo(expectedId);
  }

  @ParameterizedTest
  @DisplayName("유튜브 URL에서 비디오 ID 추출 - 실패 케이스")
  @ValueSource(strings = {
      "",
      "https://www.google.com",
      "https://www.youtube.com",
      "https://youtube.com/channel/123456",
      "https://youtu.be/"
  })
  void extractVideoId_InvalidURLs_ReturnsFalse(String url) {
    String result = youtubeDownloadService.extractVideoId(url);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("유튜브 비디오 정보 조회 - 성공")
  void getVideoInfo_Success() {
    // given
    String videoUrl = "https://www.youtube.com/watch?v=R9tUikvBv5M";

    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    when(videoInfoResponse.data()).thenReturn(videoInfo);
    when(videoInfo.details()).thenReturn(videoDetails);

    List<String> thumbnails = new ArrayList<>();
    thumbnails.add("https://example.com/thumbnail.jpg");
    when(videoDetails.thumbnails()).thenReturn(thumbnails);

    when(videoDetails.title()).thenReturn("Test Video");
    when(videoDetails.author()).thenReturn("Test Author");
    when(videoDetails.lengthSeconds()).thenReturn(300);
    when(videoDetails.viewCount()).thenReturn(1000L);

    List<AudioFormat> audioFormats = new ArrayList<>();
    audioFormats.add(audioFormat);
    when(videoInfo.audioFormats()).thenReturn(audioFormats);

    // when
    YoutubeVideoInfo result = youtubeDownloadService.getVideoInfo(videoUrl);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getVideoId()).isEqualTo("R9tUikvBv5M");
    assertThat(result.getTitle()).isEqualTo("Test Video");
    assertThat(result.getAuthor()).isEqualTo("Test Author");
    assertThat(result.getLengthSeconds()).isEqualTo(300);
    assertThat(result.getViewCount()).isEqualTo(1000L);
    assertThat(result.isHasAudioFormats()).isTrue();

    verify(youtubeDownloader).getVideoInfo(any(RequestVideoInfo.class));
  }

  @Test
  @DisplayName("유튜브 비디오 정보 조회 - 유효하지 않은 URL")
  void getVideoInfo_InvalidURL_ThrowsException() {
    // given
    String invalidUrl = "https://www.google.com";

    // when & then
    assertThatThrownBy(() -> youtubeDownloadService.getVideoInfo(invalidUrl))
        .isInstanceOf(YoutubeDownloadException.class)
        .hasMessageContaining("유효하지 않은 YouTube URL입니다");

    verify(youtubeDownloader, never()).getVideoInfo(any(RequestVideoInfo.class));
  }

  @Test
  @DisplayName("유튜브 비디오 정보 조회 - 비디오 정보 없음")
  void getVideoInfo_NoVideoInfo_ThrowsException() {
    // given
    String videoUrl = "https://www.youtube.com/watch?v=R9tUikvBv5M";

    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    when(videoInfoResponse.data()).thenReturn(null);

    // when & then
    assertThatThrownBy(() -> youtubeDownloadService.getVideoInfo(videoUrl))
        .isInstanceOf(YoutubeDownloadException.class)
        .hasMessageContaining("비디오 정보를 가져올 수 없습니다");

    verify(youtubeDownloader).getVideoInfo(any(RequestVideoInfo.class));
  }

  @Test
  @DisplayName("오디오 다운로드 - 성공")
  void downloadAudio_Success() {
    // given
    String videoUrl = "https://www.youtube.com/watch?v=R9tUikvBv5M";
    File mockFile = mock(File.class);

    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    when(videoInfoResponse.data()).thenReturn(videoInfo);
    when(videoInfo.details()).thenReturn(videoDetails);

    when(videoDetails.title()).thenReturn("Test Video");
    when(videoDetails.author()).thenReturn("Test Author");

    List<String> thumbnails = new ArrayList<>();
    thumbnails.add("https://example.com/thumbnail.jpg");
    when(videoDetails.thumbnails()).thenReturn(thumbnails);

    when(videoDetails.lengthSeconds()).thenReturn(300);

    List<AudioFormat> audioFormats = new ArrayList<>();
    audioFormats.add(audioFormat);
    when(videoInfo.audioFormats()).thenReturn(audioFormats);

    Extension mockExtension = mock(Extension.class);
    when(mockExtension.value()).thenReturn("m4a");
    when(audioFormat.extension()).thenReturn(mockExtension);

    when(youtubeDownloader.downloadVideoFile(any(RequestVideoFileDownload.class))).thenReturn(
        fileResponse);
    when(fileResponse.data()).thenReturn(mockFile);
    when(mockFile.exists()).thenReturn(true);
    when(mockFile.getAbsolutePath()).thenReturn("/test/path/R9tUikvBv5M.m4a");
    when(mockFile.getName()).thenReturn("R9tUikvBv5M.m4a");
    when(mockFile.length()).thenReturn(1024L);

    // when
    AudioDownloadResult result = youtubeDownloadService.downloadAudio(videoUrl);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getVideoId()).isEqualTo("R9tUikvBv5M");
    assertThat(result.getTitle()).isEqualTo("Test Video");
    assertThat(result.getArtist()).isEqualTo("Test Author");
    assertThat(result.getFilePath()).isEqualTo("/test/path/R9tUikvBv5M.m4a");
    assertThat(result.getFileName()).isEqualTo("R9tUikvBv5M.m4a");
    assertThat(result.getFileSize()).isEqualTo(1024L);
    assertThat(result.getMimeType()).contains("audio/");
    assertThat(result.getDurationSeconds()).isEqualTo(300);

    verify(youtubeDownloader).getVideoInfo(any(RequestVideoInfo.class));
    verify(youtubeDownloader).downloadVideoFile(any(RequestVideoFileDownload.class));
  }

  @Test
  @DisplayName("오디오 다운로드 - 다운로드 실패")
  void downloadAudio_DownloadFailed_ThrowsException() {
    // given
    String videoUrl = "https://www.youtube.com/watch?v=R9tUikvBv5M";

    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    when(videoInfoResponse.data()).thenReturn(videoInfo);
    when(videoInfo.audioFormats()).thenReturn(Collections.singletonList(audioFormat));

    Extension mockExtension = mock(Extension.class);
    when(mockExtension.value()).thenReturn("m4a");

    List<AudioFormat> audioFormats = new ArrayList<>();
    audioFormats.add(audioFormat);
    when(videoInfo.audioFormats()).thenReturn(audioFormats);
    when(audioFormat.extension()).thenReturn(mockExtension);

    when(youtubeDownloader.downloadVideoFile(any(RequestVideoFileDownload.class))).thenReturn(
        fileResponse);
    when(fileResponse.data()).thenReturn(null);

    // when & then
    assertThatThrownBy(() -> youtubeDownloadService.downloadAudio(videoUrl))
        .isInstanceOf(YoutubeDownloadException.class)
        .hasMessageContaining("오디오 파일 다운로드 실패");

    verify(youtubeDownloader).getVideoInfo(any(RequestVideoInfo.class));
    verify(youtubeDownloader).downloadVideoFile(any(RequestVideoFileDownload.class));
  }

  @Test
  @DisplayName("오디오 다운로드 - 오디오 형식 없음")
  void downloadAudio_NoAudioFormats_ThrowsException() {
    // given
    String videoUrl = "https://www.youtube.com/watch?v=R9tUikvBv5M";

    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    when(videoInfoResponse.data()).thenReturn(videoInfo);
    when(videoInfo.audioFormats()).thenReturn(Collections.emptyList());

    // when & then
    assertThatThrownBy(() -> youtubeDownloadService.downloadAudio(videoUrl))
        .isInstanceOf(YoutubeDownloadException.class)
        .hasMessageContaining("사용 가능한 오디오 형식이 없습니다");

    verify(youtubeDownloader).getVideoInfo(any(RequestVideoInfo.class));
    verify(youtubeDownloader, never()).downloadVideoFile(any(RequestVideoFileDownload.class));
  }
}
