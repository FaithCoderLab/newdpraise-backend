package faithcoderlab.newdpraise.domain.song;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import faithcoderlab.newdpraise.domain.song.SongAnalysisService.MusicAnalysisResult;
import faithcoderlab.newdpraise.global.exception.SongAnalysisException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SongAnalysisServiceTest {

  @Mock
  private YoutubeDownloader youtubeDownloader;

  @Mock
  private Response<VideoInfo> videoInfoResponse;

  @Mock
  private Response<File> fileResponse;

  @Mock
  private VideoInfo videoInfo;

  @Mock
  private AudioFormat audioFormat;

//  @Mock
//  private Extension mockExtension;

  private SongAnalysisService songAnalysisService;
  private File testAudioFile;

  private static final String VALID_YOUTUBE_URL_1 = "https://youtu.be/R9tUikvBv5M?si=_Njl-T7VYU7c1hvb";
  private static final String VALID_YOUTUBE_URL_2 = "https://youtu.be/TH4xfC3Ft4A?si=-PXDUf0ySTQb9QcJ";
  private static final String VALID_YOUTUBE_URL_3 = "https://youtu.be/HiM5ABvHCuo?si=xpqV4zaYrVKOTbb4";

  @BeforeEach
  void setUp() throws IOException {
    songAnalysisService = new SongAnalysisService(youtubeDownloader);

    testAudioFile = File.createTempFile("test-audio", ".mp3");
    testAudioFile.deleteOnExit();
  }

  @Test
  @DisplayName("유효한 유튜브 URL에서 음악 분석 성공")
  void analyzeMusicFromValidYoutubeUrl() throws Exception {
    // given
    setupMocksForSuccessfulDownload();

    SongAnalysisService spyService = spy(songAnalysisService);
    doReturn("G").when(spyService).detectKey(any(File.class));
    doReturn(120).when(spyService).detectBPM(any(File.class));

    // when
    MusicAnalysisResult result = spyService.analyzeMusic(VALID_YOUTUBE_URL_1);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo("G");
    assertThat(result.getBpm()).isEqualTo(120);
  }

  @Test
  @DisplayName("여러 유효한 유튜브 URL 테스트")
  void testMultipleValidYoutubeUrls() {
    // given
    setupMocksForSuccessfulDownload();

    SongAnalysisService spyService = spy(songAnalysisService);
    doReturn("G").when(spyService).detectKey(any(File.class));
    doReturn(120).when(spyService).detectBPM(any(File.class));

    String[] urls = {VALID_YOUTUBE_URL_1, VALID_YOUTUBE_URL_2, VALID_YOUTUBE_URL_3};

    for (String url : urls) {
      // when
      MusicAnalysisResult result = spyService.analyzeMusic(url);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getKey()).isEqualTo("G");
      assertThat(result.getBpm()).isEqualTo(120);
    }
  }

  @Test
  @DisplayName("유효하지 않은 유튜브 URL 처리")
  void handleInvalidYoutubeUrl() {
    // given
    String invalidUrl = "https://example.com/not-youtube";

    // when & then
    assertThatThrownBy(() -> songAnalysisService.analyzeMusic(invalidUrl))
        .isInstanceOf(SongAnalysisException.class)
        .hasMessageContaining("유효하지 않은 유튜브 URL입니다");
  }

  @Test
  @DisplayName("비디오 정보를 가져올 수 없는 경우 처리")
  void handleFailedVideoInfoRetrieval() {
    // given
    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    when(videoInfoResponse.data()).thenReturn(null);

    // when & then
    assertThatThrownBy(() -> songAnalysisService.analyzeMusic(VALID_YOUTUBE_URL_1))
        .isInstanceOf(SongAnalysisException.class)
        .hasMessageContaining("비디오 정보를 가져올 수 없습니다");
  }

  @Test
  @DisplayName("오디오 형식을 찾을 수 없는 경우 처리")
  void handleNoAudioFormats() {
    // given
    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    when(videoInfoResponse.data()).thenReturn(videoInfo);
    when(videoInfo.audioFormats()).thenReturn(Collections.emptyList());

    // when & then
    assertThatThrownBy(() -> songAnalysisService.analyzeMusic(VALID_YOUTUBE_URL_2))
        .isInstanceOf(SongAnalysisException.class)
        .hasMessageContaining("사용 가능한 오디오 형식이 없습니다");
  }

  @Test
  @DisplayName("파일 다운로드 실패 처리")
  void handleFailedFileDownload() {
    // given
    setupMocksForFailedDownload();

    // when & then
    assertThatThrownBy(() -> songAnalysisService.analyzeMusic(VALID_YOUTUBE_URL_3))
        .isInstanceOf(SongAnalysisException.class)
        .hasMessageContaining("오디오 파일 다운로드에 실패했습니다");
  }

  @Test
  @DisplayName("파일 다운로드 예외 처리")
  void handleExceptionDuringFileDownload() {
    // given
    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenThrow(
        new RuntimeException("다운로드 실패"));

    // when & then
    assertThatThrownBy(() -> songAnalysisService.analyzeMusic(VALID_YOUTUBE_URL_1))
        .isInstanceOf(SongAnalysisException.class)
        .hasMessageContaining("예기치 않은 오류 발생");
  }

  @Test
  @DisplayName("유튜브 비디오 ID 추출 테스트")
  void testExtractVideoId() {
    // given
    String fullUrl = "https://www.youtube.com/watch?v=R9tUikvBv5M&si=_Njl-T7VYU7c1hvb";
    String shortUrl = "https://youtu.be/R9tUikvBv5M?si=_Njl-T7VYU7c1hvb";
    String invalidUrl = "https://example.com/not-youtube";

    // when
    String fullUrlId = songAnalysisService.extractVideoId(fullUrl);
    String shortUrlId = songAnalysisService.extractVideoId(shortUrl);
    String invalidUrlId = songAnalysisService.extractVideoId(invalidUrl);

    // then
    assertThat(fullUrlId).isEqualTo("R9tUikvBv5M");
    assertThat(shortUrlId).isEqualTo("R9tUikvBv5M");
    assertThat(invalidUrlId).isNull();
  }

  private void setupMocksForSuccessfulDownload() {
    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    when(videoInfoResponse.data()).thenReturn(videoInfo);

    List<AudioFormat> audioFormats = Collections.singletonList(audioFormat);
    when(videoInfo.audioFormats()).thenReturn(audioFormats);

//    when(mockExtension.value()).thenReturn("mp3");
//    when(audioFormat.extension()).thenReturn(mockExtension);

    when(youtubeDownloader.downloadVideoFile(any(RequestVideoFileDownload.class))).thenReturn(fileResponse);
    when(fileResponse.data()).thenReturn(testAudioFile);
  }

  private void setupMocksForFailedDownload() {
    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    when(videoInfoResponse.data()).thenReturn(videoInfo);

    List<AudioFormat> audioFormats = Collections.singletonList(audioFormat);
    when(videoInfo.audioFormats()).thenReturn(audioFormats);

//    when(mockExtension.value()).thenReturn("mp3");
//    when(audioFormat.extension()).thenReturn(mockExtension);

    when(youtubeDownloader.downloadVideoFile(any(RequestVideoFileDownload.class))).thenReturn(fileResponse);
    when(fileResponse.data()).thenReturn(null);
  }
}
