package faithcoderlab.newdpraise.domain.song.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import faithcoderlab.newdpraise.domain.song.dto.AudioDownloadResult;
import faithcoderlab.newdpraise.domain.song.dto.YoutubeDownloadRequest;
import faithcoderlab.newdpraise.domain.song.dto.YoutubeVideoInfo;
import faithcoderlab.newdpraise.domain.song.service.YoutubeDownloadService;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import faithcoderlab.newdpraise.global.exception.YoutubeDownloadException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class YoutubeDownloadControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private YoutubeDownloadService youtubeDownloadService;

  @MockBean
  private UserRepository userRepository;

  private User testUser;
  private YoutubeVideoInfo testVideoInfo;
  private AudioDownloadResult testDownloadResult;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .email("test@example.com")
        .name("Test User")
        .role(Role.USER)
        .build();

    testVideoInfo = YoutubeVideoInfo.builder()
        .videoId("R9tUikvBv5M")
        .title("Test Video")
        .author("Test Author")
        .lengthSeconds(300L)
        .thumbnailUrl("https://example.com/thumbnail.jpg")
        .viewCount(1000L)
        .hasAudioFormats(true)
        .build();

    testDownloadResult = AudioDownloadResult.builder()
        .videoId("R9tUikvBv5M")
        .title("Test Audio")
        .artist("Test Artist")
        .filePath("/test/path/R9tUikvBv5M.mp3")
        .fileName("R9tUikvBv5M.mp3")
        .fileSize(1024L)
        .mimeType("audio/mp3")
        .extension("mp3")
        .bitrate(128)
        .durationSeconds(300L)
        .thumbnailUrl("https://example.com/thumbnail.jpg")
        .build();

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
  }

  @Test
  @DisplayName("유튜브 URL 유효성 검사 - 성공")
  @WithMockUser(username = "test@example.com")
  void validateYoutubeUrl_ValidUrl_Success() throws Exception {
    // given
    String url = "https://www.youtube.com/watch?v=R9tUikvBv5M";
    when(youtubeDownloadService.isValidYoutubeUrl(url)).thenReturn(true);

    // when & then
    mockMvc.perform(get("/api/songs/youtube/validate")
            .param("url", url))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().string("true"));
  }

  @Test
  @DisplayName("유튜브 URL 유효성 검사 - 실패")
  @WithMockUser(username = "test@example.com")
  void validateYoutubeUrl_InvalidUrl_ReturnFalse() throws Exception {
    // given
    String url = "https://www.invalid-url.com";
    when(youtubeDownloadService.isValidYoutubeUrl(url)).thenReturn(false);

    mockMvc.perform(get("/api/songs/youtube/validate")
            .param("url", url))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().string("false"));
  }

  @Test
  @DisplayName("유튜브 비디오 정보 조회 - 성공")
  @WithMockUser(username = "test@example.com")
  void getVideoInfo_Success() throws Exception {
    // given
    String url = "https://www.youtube.com/watch?v=R9tUikvBv5M";
    when(youtubeDownloadService.getVideoInfo(url)).thenReturn(testVideoInfo);

    mockMvc.perform(get("/api/songs/youtube/info")
            .param("url", url))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.videoId").value(testVideoInfo.getVideoId()))
        .andExpect(jsonPath("$.title").value(testVideoInfo.getTitle()))
        .andExpect(jsonPath("$.author").value(testVideoInfo.getAuthor()))
        .andExpect(jsonPath("$.lengthSeconds").value(testVideoInfo.getLengthSeconds()))
        .andExpect(jsonPath("$.viewCount").value(testVideoInfo.getViewCount()))
        .andExpect(jsonPath("$.hasAudioFormats").value(testVideoInfo.isHasAudioFormats()));
  }

  @Test
  @DisplayName("유튜브 비디오 정보 조회 - 에러")
  @WithMockUser(username = "test@example.com")
  void getVideoInfo_Error_ReturnsBadRequest() throws Exception {
    // given
    String url = "https://www.youtube.com/watch?v=invalid";
    when(youtubeDownloadService.getVideoInfo(url))
        .thenThrow(new YoutubeDownloadException("비디오 정보를 가져올 수 없습니다."));

    // when & then
    mockMvc.perform(get("/api/songs/youtube/info")
            .param("url", url))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("비디오 정보를 가져올 수 없습니다."));
  }

  @Test
  @DisplayName("유튜브 오디오 다운로드 - 성공")
  @WithMockUser(username = "test@example.com")
  void downloadAudio_Success() throws Exception {
    // given
    YoutubeDownloadRequest request = new YoutubeDownloadRequest();
    request.setUrl("https://www.youtube.com/watch?v=R9tUikvBv5M");

    when(youtubeDownloadService.downloadAudio(request.getUrl()))
        .thenReturn(testDownloadResult);

    // when & then
    mockMvc.perform(post("/api/songs/youtube/download")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.videoId").value(testDownloadResult.getVideoId()))
        .andExpect(jsonPath("$.title").value(testDownloadResult.getTitle()))
        .andExpect(jsonPath("$.artist").value(testDownloadResult.getArtist()))
        .andExpect(jsonPath("$.fileName").value(testDownloadResult.getFileName()))
        .andExpect(jsonPath("$.fileSize").value(testDownloadResult.getFileSize()));
  }

  @Test
  @DisplayName("유튜브 오디오 다운로드 - 에러")
  @WithMockUser(username = "test@example.com")
  void downloadAudio_Error_ReturnsBadRequest() throws Exception {
    // given
    YoutubeDownloadRequest request = new YoutubeDownloadRequest();
    request.setUrl("https://www.youtube.com/watch?v=invalid");

    when(youtubeDownloadService.downloadAudio(request.getUrl()))
        .thenThrow(new YoutubeDownloadException("오디오 다운로드에 실패했습니다."));

    // when & then
    mockMvc.perform(post("/api/songs/youtube/download")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("오디오 다운로드에 실패했습니다."));
  }

  @Test
  @DisplayName("유튜브 오디오 다운로드 - 유효성 검사 실패")
  @WithMockUser(username = "test@example.com")
  void downloadVideo_ValidationFailed_ReturnsBadRequest() throws Exception {
    // given
    YoutubeDownloadRequest request = new YoutubeDownloadRequest();
    request.setUrl("");

    // when & then
    mockMvc.perform(post("/api/songs/youtube/download")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("유효성 검증에 실패했습니다."));
  }

  @Test
  @DisplayName("다운로드된 오디오 파일 목록 조회 - 성공")
  @WithMockUser(username = "test@example.com")
  void getDownloadFiles_Success() throws Exception {
    // given
    when(youtubeDownloadService.getDownloadedAudioFiles())
        .thenReturn(List.of("file1.mp3", "file2.mp3"));

    // when & then
    mockMvc.perform(get("/api/songs/youtube/files"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0]").value("file1.mp3"))
        .andExpect(jsonPath("$[1]").value("file2.mp3"));
  }

  @Test
  @DisplayName("다운로드된 오디오 파일 삭제 - 성공")
  @WithMockUser(username = "test@example.com")
  void deleteAudioFile_Success() throws Exception {
    // given
    String videoId = "R9tUikvBv5M";
    when(youtubeDownloadService.deleteAudioFile(videoId)).thenReturn(true);

    mockMvc.perform(delete("/api/songs/youtube/{videoId}", videoId))
        .andDo(print())
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("다운로드된 오디오 파일 삭제 - 파일 없음")
  @WithMockUser(username = "test@example.com")
  void deleteAudioFile_NotFound_ReturnsNotFound() throws Exception {
    // given
    String videoId = "nonexistent";
    when(youtubeDownloadService.deleteAudioFile(videoId)).thenReturn(false);

    // when & then
    mockMvc.perform(delete("/api/songs/youtube/{videoId}", videoId))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("인증되지 않은 사용자 접근 - 실패")
  void unauthenticatedAccess_ReturnsUnauthorized() throws Exception {
    // when & then
    mockMvc.perform(get("/api/songs/youtube/files"))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }
}
