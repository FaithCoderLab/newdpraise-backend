package faithcoderlab.newdpraise.global.exception;

public class SongAnalysisException extends RuntimeException {

  public SongAnalysisException(String message) {
    super(message);
  }

  public SongAnalysisException(String message, Throwable cause) {
    super(message, cause);
  }
}
