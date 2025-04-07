package faithcoderlab.newdpraise.global.service;

import faithcoderlab.newdpraise.config.AppConfig;
import faithcoderlab.newdpraise.global.exception.FileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileService {

  private final AppConfig appConfig;

  public String saveFile(MultipartFile file, String subdirectory) {
    if (file.isEmpty()) {
      throw new FileException("업로드할 파일이 비어있습니다.");
    }

    if (file.getSize() > appConfig.getFileMaxSize()) {
      throw new FileException("파일 크기가 최대 허용 크기를 초과했습니다.");
    }

    String originalFilename = file.getOriginalFilename();
    String fileExtension = getFileExtension(originalFilename);
    if (!isValidImageExtension(fileExtension)) {
      throw new FileException("유효하지 않은 이미지 파일 형식입니다.");
    }

    String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

    Path uploadPath = Paths.get(appConfig.getFileUploadDir(), subdirectory);
    try {
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      Path filePath = uploadPath.resolve(uniqueFilename);
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

      return subdirectory + "/" + uniqueFilename;
    } catch (IOException e) {
      throw new FileException("파일 저장 중 오류가 발생했습니다: " + e.getMessage());
    }
  }

  private String getFileExtension(String filename) {
    if (filename == null || filename.lastIndexOf(".") == -1) {
      return "";
    }
    return filename.substring(filename.lastIndexOf("."));
  }

  private boolean isValidImageExtension(String extension) {
    return extension != null && (
        extension.equalsIgnoreCase(".jpg") ||
            extension.equalsIgnoreCase(".jpeg") ||
            extension.equalsIgnoreCase(".png") ||
            extension.equalsIgnoreCase(".gif")
    );
  }
}
