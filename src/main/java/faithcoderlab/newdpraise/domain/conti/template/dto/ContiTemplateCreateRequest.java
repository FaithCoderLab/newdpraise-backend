package faithcoderlab.newdpraise.domain.conti.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContiTemplateCreateRequest {

  @NotBlank(message = "템플릿 이름은 필수 입력 항목입니다.")
  private String name;

  private String description;

  private Long contiId;

  private boolean isPublic;
}
