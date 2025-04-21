package faithcoderlab.newdpraise.domain.song;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ResourceUtils;

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

  @Mock
  private Extension mockExtension;

  private SongAnalysisService songAnalysisService;
  private File testAudioFile;

  private static final String VALID_YOUTUBE_URL_1 = "https://youtu.be/R9tUikvBv5M?si=_Njl-T7VYU7c1hvb";
  private static final String VALID_YOUTUBE_URL_2 = "https://youtu.be/TH4xfC3Ft4A?si=-PXDUf0ySTQb9QcJ";
  private static final String VALID_YOUTUBE_URL_3 = "https://youtu.be/HiM5ABvHCuo?si=xpqV4zaYrVKOTbb4";

  @BeforeEach
  void setUp() throws IOException {
    songAnalysisService = spy(new SongAnalysisService(youtubeDownloader));

    testAudioFile = File.createTempFile("test-audio", ".mp3");
    testAudioFile.deleteOnExit();

    lenient().doReturn("G").when(songAnalysisService).detectKey(any(File.class));
    lenient().doReturn(120).when(songAnalysisService).detectBPM(any(File.class));
  }

  @Test
  @DisplayName("유효한 유튜브 URL에서 음악 분석 성공")
  void analyzeMusicFromValidYoutubeUrl() throws Exception {
    // given
    lenient().when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    lenient().when(videoInfoResponse.data()).thenReturn(videoInfo);

    List<AudioFormat> audioFormats = Collections.singletonList(audioFormat);
    lenient().when(videoInfo.audioFormats()).thenReturn(audioFormats);

    lenient().when(mockExtension.value()).thenReturn("mp3");
    lenient().when(audioFormat.extension()).thenReturn(mockExtension);

    lenient().when(youtubeDownloader.downloadVideoFile(any(RequestVideoFileDownload.class))).thenReturn(fileResponse);
    lenient().when(fileResponse.data()).thenReturn(testAudioFile);

    // when
    MusicAnalysisResult result = songAnalysisService.analyzeMusic(VALID_YOUTUBE_URL_1);

    // then
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("여러 유효한 유튜브 URL 테스트")
  void testMultipleValidYoutubeUrls() {
    lenient().when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    lenient().when(videoInfoResponse.data()).thenReturn(videoInfo);

    List<AudioFormat> audioFormats = Collections.singletonList(audioFormat);
    lenient().when(videoInfo.audioFormats()).thenReturn(audioFormats);

    lenient().when(mockExtension.value()).thenReturn("mp3");
    lenient().when(audioFormat.extension()).thenReturn(mockExtension);

    lenient().when(youtubeDownloader.downloadVideoFile(any(RequestVideoFileDownload.class))).thenReturn(fileResponse);
    lenient().when(fileResponse.data()).thenReturn(testAudioFile);

    String[] urls = {VALID_YOUTUBE_URL_1, VALID_YOUTUBE_URL_2, VALID_YOUTUBE_URL_3};

    for (String url : urls) {
      // when
      MusicAnalysisResult result = songAnalysisService.analyzeMusic(url);

      // then
      assertThat(result).isNotNull();
      assertThat(songAnalysisService.extractVideoId(url)).isNotNull();
    }
  }
  @Test
  @DisplayName("유효하지 않은 유튜브 URL 처리")
  void handleInvalidYoutubeUrl() {
    // given
    String invalidUrl = "https://example.com/not-youtube";

    // when
    MusicAnalysisResult result = songAnalysisService.analyzeMusic(invalidUrl);

    // then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("비디오 정보를 가져올 수 없는 경우 처리")
  void handleFailedVideoInfoRetrieval() {
    // given
    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    when(videoInfoResponse.data()).thenReturn(null);

    // when
    MusicAnalysisResult result = songAnalysisService.analyzeMusic(VALID_YOUTUBE_URL_1);

    // then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("오디오 형식을 찾을 수 없는 경우 처리")
  void handleNoAudioFormats() {
    // given
    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    when(videoInfoResponse.data()).thenReturn(videoInfo);
    when(videoInfo.audioFormats()).thenReturn(Collections.emptyList());

    // when
    MusicAnalysisResult result = songAnalysisService.analyzeMusic(VALID_YOUTUBE_URL_2);

    // then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("파일 다운로드 실패 처리")
  void handleFailedFileDownload() {
    // given
    lenient().when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenReturn(videoInfoResponse);
    lenient().when(videoInfoResponse.data()).thenReturn(videoInfo);

    List<AudioFormat> audioFormats = Collections.singletonList(audioFormat);
    lenient().when(videoInfo.audioFormats()).thenReturn(audioFormats);

    lenient().when(mockExtension.value()).thenReturn("mp3");
    lenient().when(audioFormat.extension()).thenReturn(mockExtension);

    lenient().when(youtubeDownloader.downloadVideoFile(any(RequestVideoFileDownload.class))).thenReturn(fileResponse);
    lenient().when(fileResponse.data()).thenReturn(null);

    // when
    MusicAnalysisResult result = songAnalysisService.analyzeMusic(VALID_YOUTUBE_URL_3);

    // then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("파일 다운로드 예외 처리")
  void handleExceptionDuringFileDownload() {
    // given
    when(youtubeDownloader.getVideoInfo(any(RequestVideoInfo.class))).thenThrow(
        new RuntimeException("다운로드 실패"));

    // when
    MusicAnalysisResult result = songAnalysisService.analyzeMusic(VALID_YOUTUBE_URL_1);

    // then
    assertThat(result).isNull();
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
}
