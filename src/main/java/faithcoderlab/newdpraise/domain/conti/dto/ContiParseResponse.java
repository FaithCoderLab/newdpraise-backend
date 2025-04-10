package faithcoderlab.newdpraise.domain.conti.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContiParseResponse {
  private String title;
  private LocalDate performanceDate;
  private List<SongDto> songs;
  private String version;
  private String status;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SongDto {
    private String title;
    private String originalKey;
    private String performanceKey;
    private String youtubeUrl;
    private String specialInstructions;
    private String bpm;
  }
}
