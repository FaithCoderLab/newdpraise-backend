package faithcoderlab.newdpraise.domain.conti.share.dto;

import faithcoderlab.newdpraise.domain.conti.share.ContiShare;
import faithcoderlab.newdpraise.domain.conti.share.ContiSharePermission;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContiShareResponse {

  private Long id;
  private Long contiId;
  private String contiTitle;
  private Long userId;
  private String userEmail;
  private String userName;
  private Long sharedById;
  private String sharedByName;
  private ContiSharePermission permission;
  private boolean accepted;
  private LocalDateTime invitedAt;
  private LocalDateTime acceptedAt;

  public static ContiShareResponse fromContiShare(ContiShare contiShare) {
    return ContiShareResponse.builder()
        .id(contiShare.getId())
        .contiId(contiShare.getConti().getId())
        .contiTitle(contiShare.getConti().getTitle())
        .userId(contiShare.getUser().getId())
        .userEmail(contiShare.getUser().getEmail())
        .userName(contiShare.getUser().getName())
        .sharedById(contiShare.getSharedBy() != null ? contiShare.getSharedBy().getId() : null)
        .sharedByName(contiShare.getSharedBy() != null ? contiShare.getSharedBy().getName() : null)
        .permission(contiShare.getPermission())
        .accepted(contiShare.isAccepted())
        .invitedAt(contiShare.getInvitedAt())
        .acceptedAt(contiShare.getAcceptedAt())
        .build();
  }
}
