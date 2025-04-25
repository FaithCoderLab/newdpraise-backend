package faithcoderlab.newdpraise.domain.conti.template.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContiTemplateResponse {

  private Long id;

  private String name;

  private String description;

  private Long creatorId;

  private String creatorName;

  private boolean isPublic;

  private Integer usageCount;

  private int songCount;

  private String createdAt;

  private String updatedAt;

  private boolean isOwner;
}
