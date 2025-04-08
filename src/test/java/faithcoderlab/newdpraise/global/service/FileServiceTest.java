package faithcoderlab.newdpraise.global.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import faithcoderlab.newdpraise.config.AppConfig;
import faithcoderlab.newdpraise.global.exception.FileException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  @TempDir
  Path tempDir;

  @Mock
  private AppConfig appConfig;

  @InjectMocks
  private FileService fileService;

  @Test
  @DisplayName("파일 저장 - 성공")
  void saveFileSuccess() {
    // given
    String uploadDir = tempDir.toString();
    when(appConfig.getFileUploadDir()).thenReturn(uploadDir);
    when(appConfig.getFileMaxSize()).thenReturn(1024 * 1024L);

    MockMultipartFile file = new MockMultipartFile(
        "file", "test.jpg", "image/jpeg", "test image content".getBytes()
    );

    // when
    String savedPath = fileService.saveFile(file, "profiles");

    // then
    assertThat(savedPath).isNotNull();
    assertThat(savedPath).startsWith("profiles/");
    assertThat(savedPath).endsWith(".jpg");
  }

  @Test
  @DisplayName("파일 저장 - 실패 (빈 파일)")
  void saveFileFailWithEmptyFile() {
    // given
    MockMultipartFile file = new MockMultipartFile(
        "file", "empty.jpg", "image/jpeg", new byte[0]
    );

    // when & then
    assertThatThrownBy(() -> fileService.saveFile(file, "profiles"))
        .isInstanceOf(FileException.class)
        .hasMessageContaining("비어있습니다.");
  }

  @Test
  @DisplayName("파일 저장 - 실패 (최대 크기 초과)")
  void saveFileFailWithOversizedFile() {
    // given
    when(appConfig.getFileMaxSize()).thenReturn(10L);

    MockMultipartFile file = new MockMultipartFile(
        "file", "large.jpg", "image/jpeg", "test image content with size over 10 bytes".getBytes()
    );

    // when & then
    assertThatThrownBy(() -> fileService.saveFile(file, "profiles"))
        .isInstanceOf(FileException.class)
        .hasMessageContaining("최대 허용 크기를 초과");
  }

  @ParameterizedTest
  @DisplayName("파일 저장 - 실패 (유효하지 않은 파일 형식)")
  @ValueSource(strings = {".txt", ".pdf", ".doc", ".exe"})
  void saveFileFailWithInvalidExtension(String extension) {
    // given
    when(appConfig.getFileMaxSize()).thenReturn(1024 * 1024L);

    MockMultipartFile file = new MockMultipartFile(
        "file", "test" + extension, "application/octet-stream", "test content".getBytes()
    );

    // when & then
    assertThatThrownBy(() -> fileService.saveFile(file, "profiles"))
        .isInstanceOf(FileException.class)
        .hasMessageContaining("유효하지 않은 이미지 파일 형식");
  }
}
