package faithcoderlab.newdpraise.domain.song.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeVideoInfo {
  private String videoId;
  private String title;
  private String author;
  private Long lengthSeconds;
  private String thumbnailUrl;
  private Long viewCount;
  private boolean hasAudioFormats;
}
