package faithcoderlab.newdpraise.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AppConfig {

  @Value("${youtube.api.key}")
  private String youtubeApiKey;

  @Value("${file.upload-dir}")
  private String fileUploadDir;

  @Value("${file.max-size}")
  private long fileMaxSize;

}
