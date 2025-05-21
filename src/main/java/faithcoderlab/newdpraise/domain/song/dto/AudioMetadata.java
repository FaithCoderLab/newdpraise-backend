package faithcoderlab.newdpraise.domain.song.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioMetadata {
  private String key;
  private Integer bpm;
  private Long durationSeconds;
}
