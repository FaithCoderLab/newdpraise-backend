package faithcoderlab.newdpraise.global.exception;

import java.io.IOException;

public class YoutubeDownloadException extends RuntimeException {

  public YoutubeDownloadException(String message) {
    super(message);
  }

  public YoutubeDownloadException(String message, Throwable cause) {
    super(message, cause);
  }
}
