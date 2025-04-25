package faithcoderlab.newdpraise.domain.conti.share.dto;

import faithcoderlab.newdpraise.domain.conti.share.ContiSharePermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContiShareRequest {

  @NotNull(message = "콘티 ID는 필수 입력 항목입니다.")
  private Long contiId;

  @NotBlank(message = "이메일은 필수 입력 항목입니다.")
  @Email(message = "유효한 이메일 형식이 아닙니다.")
  private String userEmail;

  @NotNull(message = "권한은 필수 입력 항목입니다.")
  private ContiSharePermission permission;
}
