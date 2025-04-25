package faithcoderlab.newdpraise.domain.conti.template.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContiTemplateApplyRequest {

  private Long templateId;

  private String customTitle;

  private String customDescription;
}
