package faithcoderlab.newdpraise.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
  @NotBlank(message = "이름은 필수 입력 항목입니다.")
  private String name;

  private String instrument;
}
