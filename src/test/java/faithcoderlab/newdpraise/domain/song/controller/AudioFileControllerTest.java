package faithcoderlab.newdpraise.domain.song.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import faithcoderlab.newdpraise.config.TestConfig;
import faithcoderlab.newdpraise.config.TestDatabaseConfig;
import faithcoderlab.newdpraise.config.TestSecurityConfig;
import faithcoderlab.newdpraise.domain.song.AudioFile;
import faithcoderlab.newdpraise.domain.song.dto.AudioDownloadResult;
import faithcoderlab.newdpraise.domain.song.dto.AudioFileDto;
import faithcoderlab.newdpraise.domain.song.dto.AudioMetadataUpdateRequest;
import faithcoderlab.newdpraise.domain.song.dto.YoutubeDownloadRequest;
import faithcoderlab.newdpraise.domain.song.service.AudioFileService;
import faithcoderlab.newdpraise.domain.song.service.YoutubeDownloadService;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import faithcoderlab.newdpraise.global.exception.ResourceAlreadyExistsException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestSecurityConfig.class, TestDatabaseConfig.class})
class AudioFileControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private AudioFileService audioFileService;

  @MockBean
  private YoutubeDownloadService youtubeDownloadService;

  @MockBean
  private UserRepository userRepository;

  private User testUser;
  private AudioFile testAudioFile;
  private AudioFileDto testAudioFileDto;
  private AudioDownloadResult testDownloadResult;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .email("test@example.com")
        .name("Test User")
        .role(Role.USER)
        .build();

    testAudioFile = AudioFile.builder()
        .id(1L)
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
        .originalUrl("https://www.youtube.com/watch?v=R9tUikvBv5M")
        .uploader(testUser)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    testAudioFileDto = AudioFileDto.builder()
        .id(1L)
        .videoId("R9tUikvBv5M")
        .title("Test Audio")
        .artist("Test Artist")
        .fileName("R9tUikvBv5M.mp3")
        .fileSize(1024L)
        .mimeType("audio/mp3")
        .extension("mp3")
        .bitrate(128)
        .durationSeconds(300L)
        .thumbnailUrl("https://example.com/thumbnail.jpg")
        .downloadUrl("/api/songs/youtube/stream/R9tUikvBv5M")
        .uploaderId(1L)
        .uploaderName("Test User")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
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
  @DisplayName("유튜브 오디오 다운로드 및 저장 - 성공")
  @WithMockUser("test@example.com")
  void downloadAndSaveAudio_Success() throws Exception {
    // given
    YoutubeDownloadRequest request = new YoutubeDownloadRequest();
    request.setUrl("https://www.youtube.com/watch?v=R9tUikvBv5M");

    when(youtubeDownloadService.isValidYoutubeUrl(request.getUrl())).thenReturn(true);
    when(youtubeDownloadService.downloadAudio(request.getUrl())).thenReturn(testDownloadResult);
    when(audioFileService.saveAudioFile(eq(testDownloadResult), eq(testUser), anyString()))
        .thenReturn(testAudioFile);

    // when & then
    mockMvc.perform(post("/api/songs/audio/download")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.videoId").value(testAudioFile.getVideoId()))
        .andExpect(jsonPath("$.title").value(testAudioFile.getTitle()))
        .andExpect(jsonPath("$.artist").value(testAudioFile.getArtist()))
        .andExpect(jsonPath("$.fileName").value(testAudioFile.getFileName()))
        .andExpect(jsonPath("$.fileSize").value(testAudioFile.getFileSize()));
  }

  @Test
  @DisplayName("유튜브 오디오 다운로드 및 저장 - 유효하지 않은 URL")
  @WithMockUser(username = "test@example.com")
  void downloadAndSaveAudio_InvalidUrl_ReturnsBadRequest() throws Exception {
    // given
    YoutubeDownloadRequest request = new YoutubeDownloadRequest();
    request.setUrl("https://www.invalid-url.com");

    when(youtubeDownloadService.isValidYoutubeUrl(request.getUrl())).thenReturn(false);

    // when & then
    mockMvc.perform(post("/api/songs/audio/download")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("유튜브 오디오 다운로드 및 저장 - 이미 존재함")
  @WithMockUser("test@example.com")
  void downloadAndSaveAudio_AlreadyExists_ReturnsConflict() throws Exception {
    // given
    YoutubeDownloadRequest request = new YoutubeDownloadRequest();
    request.setUrl("https://www.youtube.com/watch?v=R9tUikvBv5M");

    when(youtubeDownloadService.isValidYoutubeUrl(request.getUrl())).thenReturn(true);
    when(youtubeDownloadService.downloadAudio(request.getUrl())).thenReturn(testDownloadResult);
    when(audioFileService.saveAudioFile(eq(testDownloadResult), eq(testUser), anyString()))
        .thenThrow(new ResourceAlreadyExistsException("이미 다운로드된 오디오 파일입니다: R9tUikvBv5M"));

    // when & then
    mockMvc.perform(post("/api/songs/audio/download")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("이미 다운로드된 오디오 파일입니다: R9tUikvBv5M"));
  }

  @Test
  @DisplayName("사용자의 오디오 파일 목록 조회 - 성공")
  @WithMockUser("test@example.com")
  void getMyAudioFiles_Success() throws Exception {
    // given
    List<AudioFileDto> audioFiles = Collections.singletonList(testAudioFileDto);
    when(audioFileService.getUserAudioFiles(testUser)).thenReturn(audioFiles);

    // when & then
    mockMvc.perform(get("/api/songs/audio/my"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].videoId").value(testAudioFile.getVideoId()))
        .andExpect(jsonPath("$[0].title").value(testAudioFile.getTitle()))
        .andExpect(jsonPath("$[0].artist").value(testAudioFile.getArtist()));
  }

  @Test
  @DisplayName("사용자의 오디오 파일 목록 페이징 조회 - 성공")
  @WithMockUser(username = "test@example.com")
  void getMyAudioFilesPaged_Success() throws Exception {
    // given
    Page<AudioFileDto> audioFilePage = new PageImpl<>(Collections.singletonList(testAudioFileDto));
    when(audioFileService.getUserAudioFiles(eq(testUser), any(Pageable.class))).thenReturn(
        audioFilePage);

    // when & then
    mockMvc.perform(get("/api/songs/audio/my/paged")
            .param("page", "0")
            .param("size", "10")
            .param("sort", "createdAt")
            .param("direction", "desc"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].videoId").value(testAudioFile.getVideoId()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("오디오 파일 검색 - 성공")
  @WithMockUser(username = "test@example.com")
  void searchAudioFiles_Success() throws Exception {
    // given
    String keyword = "test";
    Page<AudioFileDto> audioFilePage = new PageImpl<>(Collections.singletonList(testAudioFileDto));
    when(audioFileService.searchAudioFiles(eq(keyword), any(Pageable.class))).thenReturn(
        audioFilePage);

    // when & then
    mockMvc.perform(get("/api/songs/audio/search")
            .param("keyword", keyword)
            .param("page", "0")
            .param("size", "10"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].videoId").value(testAudioFile.getVideoId()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("오디오 파일 메타데이터 수정 - 성공")
  @WithMockUser(username = "test@example.com")
  void updateAudioFileMetadata_Success() throws Exception {
    // given
    String videoId = "R9tUikvBv5M";
    AudioMetadataUpdateRequest request = new AudioMetadataUpdateRequest();
    request.setTitle("Updated Title");
    request.setArtist("Updated Artist");

    AudioFileDto updatedDto = AudioFileDto.builder()
        .id(1L)
        .videoId(videoId)
        .title("Updated Title")
        .artist("Updated Artist")
        .fileName(testAudioFileDto.getFileName())
        .fileSize(testAudioFileDto.getFileSize())
        .mimeType(testAudioFileDto.getMimeType())
        .extension(testAudioFileDto.getExtension())
        .bitrate(testAudioFileDto.getBitrate())
        .durationSeconds(testAudioFileDto.getDurationSeconds())
        .thumbnailUrl(testAudioFileDto.getThumbnailUrl())
        .downloadUrl(testAudioFileDto.getDownloadUrl())
        .uploaderId(testAudioFileDto.getUploaderId())
        .uploaderName(testAudioFileDto.getUploaderName())
        .createdAt(testAudioFileDto.getCreatedAt())
        .updatedAt(LocalDateTime.now())
        .build();

    when(audioFileService.updateAudioFileMetadata(
        eq(videoId), eq(request.getTitle()), eq(request.getArtist()), eq(testUser)))
        .thenReturn(updatedDto);

    // when & then
    mockMvc.perform(put("/api/songs/audio/{videoId}/metadata", videoId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.videoId").value(videoId))
        .andExpect(jsonPath("$.title").value("Updated Title"))
        .andExpect(jsonPath("$.artist").value("Updated Artist"));
  }

  @Test
  @DisplayName("오디오 파일 메타데이터 수정 - 파일 없음")
  @WithMockUser(username = "test@example.com")
  void updateAudioFileMetadata_NotFound_ReturnsNotFound() throws Exception {
    // given
    String videoId = "nonexistent";
    AudioMetadataUpdateRequest request = new AudioMetadataUpdateRequest();
    request.setTitle("Updated Title");
    request.setArtist("Updated Artist");

    when(audioFileService.updateAudioFileMetadata(
        eq(videoId), eq(request.getTitle()), eq(request.getArtist()), eq(testUser)
    )).thenThrow(new ResourceNotFoundException("오디오 파일을 찾을 수 없습니다: " + videoId));

    // when * then
    mockMvc.perform(put("/api/songs/audio/{videoId}/metadata", videoId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("오디오 파일을 찾을 수 없습니다: " + videoId));
  }

  @Test
  @DisplayName("오디오 파일 삭제 - 성공")
  @WithMockUser(username = "test@example.com")
  void deleteAudioFile_Success() throws Exception {
    // given
    String videoId = "R9tUikvBv5M";
    when(audioFileService.deleteAudioFile(videoId, testUser)).thenReturn(true);

    // when & then
    mockMvc.perform(delete("/api/songs/audio/{videoId}", videoId))
        .andDo(print())
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("오디오 파일 삭제 - 파일 없음")
  @WithMockUser(username = "test@example.com")
  void deleteAudioFile_NotFound_ReturnsNotFound() throws Exception {
    // given
    String videoId = "nonexistent";
    when(audioFileService.deleteAudioFile(videoId, testUser))
        .thenThrow(new ResourceNotFoundException("오디오 파일을 찾을 수 없습니다: " + videoId));

    // when & then
    mockMvc.perform(delete("/api/songs/audio/{videoId}", videoId))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("오디오 파일을 찾을 수 없습니다: " + videoId));
  }

  @Test
  @DisplayName("비디오 ID로 오디오 파일 정보 조회 - 성공")
  @WithMockUser(username = "test@example.com")
  void getAudioFileByVideoId_Success() throws Exception {
    // given
    String videoId = "R9tUikvBv5M";
    when(audioFileService.getAudioFileByVideoId(videoId)).thenReturn(testAudioFile);

    // when & then
    mockMvc.perform(get("/api/songs/audio/{videoId}", videoId))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.videoId").value(videoId))
        .andExpect(jsonPath("$.title").value(testAudioFile.getTitle()))
        .andExpect(jsonPath("$.artist").value(testAudioFile.getArtist()));
  }

  @Test
  @DisplayName("비디오 ID로 오디오 파일 정보 조회 - 파일 없음")
  @WithMockUser(username = "test@example.com")
  void getAudioFileByVideoId_NotFound_ReturnsNotFound() throws Exception {
    // given
    String videoId = "nonexistent";
    when(audioFileService.getAudioFileByVideoId(videoId))
        .thenThrow(new ResourceNotFoundException("오디오 파일을 찾을 수 없습니다: " + videoId));

    // when & then
    mockMvc.perform(get("/api/songs/audio/{videoId}", videoId))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("오디오 파일을 찾을 수 없습니다: " + videoId));
  }

  @Test
  @DisplayName("인증되지 않은 사용자 접근 - 실패")
  void unauthenticatedAccess_ReturnsUnauthorized() throws Exception {
    // when & then
    mockMvc.perform(get("/api/songs/audio/my"))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }
}
