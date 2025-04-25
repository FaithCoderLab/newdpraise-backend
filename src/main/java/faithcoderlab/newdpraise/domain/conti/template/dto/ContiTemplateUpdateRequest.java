package faithcoderlab.newdpraise.domain.conti.template.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContiTemplateUpdateRequest {

  private String name;

  private String description;

  private Boolean isPublic;
}
