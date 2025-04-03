package faithcoderlab.newdpraise.domain.auth.dto;

import faithcoderlab.newdpraise.domain.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
  private Long id;
  private String email;
  private String name;
  private String instrument;
  private String profileImage;
  private Role role;
  private String accessToken;
  private String refreshToken;
}
