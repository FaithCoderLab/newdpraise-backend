package faithcoderlab.newdpraise.global.exception;

public class FileException extends RuntimeException {

  public FileException(String message) {
    super(message);
  }

  public FileException(String message, Throwable cause) {
    super(message, cause);
  }
}
