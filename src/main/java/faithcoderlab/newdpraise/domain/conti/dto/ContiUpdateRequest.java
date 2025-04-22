package faithcoderlab.newdpraise.domain.conti.dto;

import faithcoderlab.newdpraise.domain.conti.ContiStatus;
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
public class ContiUpdateRequest {
  private String title;
  private String description;
  private LocalDate scheduledAt;
  private ContiStatus status;
  private List<SongDto> songs;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SongDto {
    private Long id;
    private String title;
    private String originalKey;
    private String performanceKey;
    private String artist;
    private String youtubeUrl;
    private String referenceUrl;
    private String specialInstructions;
    private String bpm;
  }
}
