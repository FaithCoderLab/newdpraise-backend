package faithcoderlab.newdpraise.domain.user.dto;

import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
  private Long id;
  private String email;
  private String name;
  private String instrument;
  private String profileImage;
  private Role role;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static UserProfileResponse fromUser(User user) {
    return UserProfileResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .instrument(user.getInstrument())
        .profileImage(user.getProfileImage())
        .role(user.getRole())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
