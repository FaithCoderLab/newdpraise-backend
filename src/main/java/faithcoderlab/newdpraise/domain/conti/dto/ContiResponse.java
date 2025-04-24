package faithcoderlab.newdpraise.domain.conti.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContiResponse {
  private Long id;
  private String title;
  private String description;
  private LocalDate scheduledAt;
  private Long creatorId;
  private String creatorName;
  private List<SongDto> songs;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private boolean isShared;
  private boolean canEdit;
  private boolean canShare;
  private String permissionType;

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
    private String specialInstructions;
  }
}
