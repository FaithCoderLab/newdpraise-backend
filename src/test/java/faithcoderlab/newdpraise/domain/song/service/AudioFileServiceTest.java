package faithcoderlab.newdpraise.domain.song.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faithcoderlab.newdpraise.domain.song.AudioFile;
import faithcoderlab.newdpraise.domain.song.AudioFileRepository;
import faithcoderlab.newdpraise.domain.song.dto.AudioDownloadResult;
import faithcoderlab.newdpraise.domain.song.dto.AudioFileDto;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.global.exception.ResourceAlreadyExistsException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AudioFileServiceTest {

  @Mock
  private AudioFileRepository audioFileRepository;

  @Mock
  private YoutubeDownloadService youtubeDownloadService;

  @InjectMocks
  private AudioFileService audioFileService;

  private User testUser;
  private AudioFile testAudioFile;
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
  }

  @Test
  @DisplayName("오디오 파일 저장 - 성공")
  void saveAudioFile_Success() {
    // given
    String originalUrl = "https://www.youtube.com/watch?v=R9tUikvBv5M";

    when(audioFileRepository.existsByVideoId(testDownloadResult.getVideoId())).thenReturn(false);
    when(audioFileRepository.save(any(AudioFile.class))).thenReturn(testAudioFile);

    // when
    AudioFile result = audioFileService.saveAudioFile(testDownloadResult, testUser, originalUrl);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getVideoId()).isEqualTo(testDownloadResult.getVideoId());
    assertThat(result.getTitle()).isEqualTo(testDownloadResult.getTitle());
    assertThat(result.getArtist()).isEqualTo(testDownloadResult.getArtist());
    assertThat(result.getUploader()).isEqualTo(testUser);
    assertThat(result.getOriginalUrl()).isEqualTo(originalUrl);

    verify(audioFileRepository).existsByVideoId(testDownloadResult.getVideoId());
    verify(audioFileRepository).save(any(AudioFile.class));
  }

  @Test
  @DisplayName("오디오 파일 저장 - 이미 존재함")
  void saveAudioFile_AlreadyExists_ThrowsException() {
    // given
    String originalUrl = "https://www.youtube.com/watch?v=R9tUikvBv5M";

    when(audioFileRepository.existsByVideoId(testDownloadResult.getVideoId())).thenReturn(true);

    // when & then
    assertThatThrownBy(
        () -> audioFileService.saveAudioFile(testDownloadResult, testUser, originalUrl))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessageContaining("이미 다운로드된 오디오 파일입니다");

    verify(audioFileRepository).existsByVideoId(testDownloadResult.getVideoId());
    verify(audioFileRepository, never()).save(any(AudioFile.class));
  }

  @Test
  @DisplayName("비디오 ID로 오디오 파일 조회 - 성공")
  void getAudioFileByVideoId_Success() {
    // given
    String videoId = "R9tUikvBv5M";

    when(audioFileRepository.findByVideoId(videoId)).thenReturn(Optional.of(testAudioFile));

    // when
    AudioFile result = audioFileService.getAudioFileByVideoId(videoId);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getVideoId()).isEqualTo(videoId);
    assertThat(result.getTitle()).isEqualTo(testAudioFile.getTitle());

    verify(audioFileRepository).findByVideoId(videoId);
  }

  @Test
  @DisplayName("비디오 ID로 오디오 파일 조회 - 존재하지 않음")
  void getAudioFileByVideoId_NotFound_ThrowsException() {
    // given
    String videoId = "nonexistent";

    when(audioFileRepository.findByVideoId(videoId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> audioFileService.getAudioFileByVideoId(videoId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("오디오 파일을 찾을 수 없습니다");

    verify(audioFileRepository).findByVideoId(videoId);
  }

  @Test
  @DisplayName("사용자의 오디오 파일 목록 조회 - 성공")
  void getUserAudioFiles_Success() {
    // given
    List<AudioFile> audioFiles = Arrays.asList(testAudioFile);

    when(audioFileRepository.findByUploader(testUser)).thenReturn(audioFiles);

    // when
    List<AudioFileDto> result = audioFileService.getUserAudioFiles(testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVideoId()).isEqualTo(testAudioFile.getVideoId());
    assertThat(result.get(0).getTitle()).isEqualTo(testAudioFile.getTitle());

    verify(audioFileRepository).findByUploader(testUser);
  }

  @Test
  @DisplayName("사용자의 오디오 파일 목록 페이징 조회 - 성공")
  void getUserAudioFilesPaged_Success() {
    // given
    Page<AudioFile> audioFilePage = new PageImpl<>(Collections.singletonList(testAudioFile));
    Pageable pageable = Pageable.unpaged();

    when(audioFileRepository.findByUploader(testUser, pageable)).thenReturn(audioFilePage);

    // when
    Page<AudioFileDto> result = audioFileService.getUserAudioFiles(testUser, pageable);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getVideoId()).isEqualTo(testAudioFile.getVideoId());
    assertThat(result.getContent().get(0).getTitle()).isEqualTo(testAudioFile.getTitle());

    verify(audioFileRepository).findByUploader(testUser, pageable);
  }

  @Test
  @DisplayName("키워드로 오디오 파일 검색 - 성공")
  void searchAudioFiles_Success() {
    // given
    String keyword = "test";
    Page<AudioFile> audioFilePage = new PageImpl<>(Collections.singletonList(testAudioFile));
    Pageable pageable = Pageable.unpaged();

    when(audioFileRepository.searchByKeyword(keyword, pageable)).thenReturn(audioFilePage);

    // when
    Page<AudioFileDto> result = audioFileService.searchAudioFiles(keyword, pageable);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getVideoId()).isEqualTo(testAudioFile.getVideoId());
    assertThat(result.getContent().get(0).getTitle()).isEqualTo(testAudioFile.getTitle());

    verify(audioFileRepository).searchByKeyword(keyword, pageable);
  }

  @Test
  @DisplayName("오디오 파일 삭제 - 성공")
  void deleteAudioFile_Success() {
    // given
    String videoId = "R9tUikvBv5M";
    File mockFile = mock(File.class);

    when(audioFileRepository.findByVideoIdAndUploaderId(videoId, testUser.getId())).thenReturn(
        Optional.of(testAudioFile));
    when(mockFile.delete()).thenReturn(true);

    AudioFileService testService = new AudioFileService(audioFileRepository,
        youtubeDownloadService) {
      @Override
      protected File getFileFromPath(String filePath) {
        return mockFile;
      }
    };

    // when
    boolean result = testService.deleteAudioFile(videoId, testUser);

    // then
    assertThat(result).isTrue();

    verify(audioFileRepository).findByVideoIdAndUploaderId(videoId, testUser.getId());
    verify(audioFileRepository).delete(testAudioFile);
    verify(mockFile).delete();
  }

  @Test
  @DisplayName("오디오 파일 삭제 - 파일 없음")
  void deleteAudioFile_NotFound_ThrowsException() {
    // given
    String videoId = "nonexistent";

    when(audioFileRepository.findByVideoIdAndUploaderId(videoId, testUser.getId())).thenReturn(
        Optional.empty());

    // when & then
    assertThatThrownBy(() -> audioFileService.deleteAudioFile(videoId, testUser))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("오디오 파일을 찾을 수 없습니다");

    verify(audioFileRepository).findByVideoIdAndUploaderId(videoId, testUser.getId());
    verify(audioFileRepository, never()).delete(any(AudioFile.class));
  }

  @Test
  @DisplayName("오디오 파일 메타데이터 업데이트 - 성공")
  void updateAudioFileMetadata_Success() {
    // given
    String videoId = "R9tUikvBv5M";
    String newTitle = "Updated Title";
    String newArtist = "Updated Artist";

    when(audioFileRepository.findByVideoIdAndUploaderId(videoId, testUser.getId())).thenReturn(
        Optional.of(testAudioFile));
    when(audioFileRepository.save(any(AudioFile.class))).thenReturn(testAudioFile);

    // when
    AudioFileDto result = audioFileService.updateAudioFileMetadata(videoId, newTitle, newArtist,
        testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo(newTitle);
    assertThat(result.getArtist()).isEqualTo(newArtist);

    verify(audioFileRepository).findByVideoIdAndUploaderId(videoId, testUser.getId());
    verify(audioFileRepository).save(testAudioFile);
  }

  @Test
  @DisplayName("오디오 파일 메타데이터 업데이트 - 파일 없음")
  void updateAudioFileMetadata_NotFound_ThrowsException() {
    // given
    String videoId = "nonexistent";
    String newTitle = "Updated Title";
    String newArtist = "Updated Artist";

    when(audioFileRepository.findByVideoIdAndUploaderId(videoId, testUser.getId())).thenReturn(
        Optional.empty());

    // when & then
    assertThatThrownBy(
        () -> audioFileService.updateAudioFileMetadata(videoId, newTitle, newArtist, testUser))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("오디오 파일을 찾을 수 없습니다");

    verify(audioFileRepository).findByVideoIdAndUploaderId(videoId, testUser.getId());
    verify(audioFileRepository, never()).save(any(AudioFile.class));
  }
}
