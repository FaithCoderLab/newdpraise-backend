package faithcoderlab.newdpraise.domain.conti.dto;

import faithcoderlab.newdpraise.domain.conti.ContiStatus;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContiSearchRequest {
  private LocalDate startDate;
  private LocalDate endDate;
  private String title;
  private Long creatorId;
  private ContiStatus status;

  public boolean isEmpty() {
    return startDate == null &&
        endDate == null &&
        (title == null || title.isEmpty()) &&
        creatorId == null &&
        status == null;
  }
}
