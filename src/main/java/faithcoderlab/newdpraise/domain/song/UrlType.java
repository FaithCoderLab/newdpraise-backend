package faithcoderlab.newdpraise.domain.song;

public enum UrlType {
  YOUTUBE("youtube"),
  OTHER("other");

  private final String value;

  UrlType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static UrlType fromUrl(String url) {
    if (url == null) {
      return null;
    }

    if (url.contains("youtube.com") || url.contains("youtu.be")) {
      return YOUTUBE;
    }

    return OTHER;
  }
}
