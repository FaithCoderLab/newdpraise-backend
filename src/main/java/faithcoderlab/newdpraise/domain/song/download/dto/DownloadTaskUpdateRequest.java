package faithcoderlab.newdpraise.domain.song.download.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadTaskUpdateRequest {
  private String title;
  private String artist;
}
