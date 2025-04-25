package faithcoderlab.newdpraise.domain.conti.share;

import faithcoderlab.newdpraise.domain.conti.Conti;
import faithcoderlab.newdpraise.domain.conti.ContiRepository;
import faithcoderlab.newdpraise.domain.conti.share.dto.ContiShareRequest;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import faithcoderlab.newdpraise.global.exception.AuthenticationException;
import faithcoderlab.newdpraise.global.exception.ResourceAlreadyExistsException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContiShareService {

  private final ContiShareRepository contiShareRepository;
  private final ContiRepository contiRepository;
  private final UserRepository userRepository;

  @Transactional
  public ContiShare createShare(ContiShareRequest request, User currentUser) {
    Conti conti = contiRepository.findById(request.getContiId())
        .orElseThrow(
            () -> new ResourceNotFoundException("콘티를 찾을 수 없습니다. ID: " + request.getContiId()));

    validateSharePermission(conti, currentUser);

    User targetUser = userRepository.findByEmail(request.getUserEmail())
        .orElseThrow(() -> new ResourceNotFoundException(
            "사용자를 찾을 수 없습니다.: Email: " + request.getUserEmail()));

    if (targetUser.getId().equals(currentUser.getId())) {
      throw new IllegalArgumentException("자신에게는 콘티를 공유할 수 없습니다.");
    }

    contiShareRepository.findByContiAndUser(conti, targetUser).ifPresent(share -> {
      throw new ResourceAlreadyExistsException("이미 해당 사용자에게 공유된 콘티입니다.");
    });

    ContiShare contiShare = ContiShare.builder()
        .conti(conti)
        .user(targetUser)
        .sharedBy(currentUser)
        .permission(request.getPermission())
        .accepted(false)
        .build();

    return contiShareRepository.save(contiShare);
  }

  @Transactional
  public ContiShare acceptShare(Long shareId, User currentUser) {
    ContiShare share = contiShareRepository.findById(shareId)
        .orElseThrow(() -> new ResourceNotFoundException("공유 정보를 찾을 수 없습니다. ID: " + shareId));

    if (!share.getUser().getId().equals(currentUser.getId())) {
      throw new AuthenticationException("해당 공유 요청에 대한 권한이 없습니다.");
    }

    if (share.isAccepted()) {
      throw new ResourceAlreadyExistsException("이미 수락된 공유 요청입니다.");
    }

    share.setAccepted(true);
    share.setAcceptedAt(LocalDateTime.now());

    return contiShareRepository.save(share);
  }

  @Transactional
  public void deleteShare(Long shareId, User currentUser) {
    ContiShare share = contiShareRepository.findById(shareId)
        .orElseThrow(() -> new ResourceNotFoundException("공유 정보를 찾을 수 없습니다. ID: " + shareId));

    if (!share.getUser().getId().equals(currentUser.getId()) &&
        (share.getSharedBy() == null || !share.getSharedBy().getId().equals(currentUser.getId())) &&
        !share.getConti().getCreator().getId().equals(currentUser.getId())) {
      throw new AuthenticationException("해당 공유 요청을 삭제할 권한이 없습니다.");
    }

    contiShareRepository.delete(share);
  }

  @Transactional
  public ContiShare updateSharePermission(Long shareId, ContiSharePermission permission,
      User currentUser) {
    ContiShare share = contiShareRepository.findById(shareId)
        .orElseThrow(() -> new ResourceNotFoundException("공유 정보를 찾을 수 없습니다. ID: " + shareId));

    if ((share.getSharedBy() == null || !share.getSharedBy().getId().equals(currentUser.getId())) &&
        !share.getConti().getCreator().getId().equals(currentUser.getId())) {
      throw new AuthenticationException("해당 공유 요청의 권한을 수정할 권한이 없습니다.");
    }

    share.setPermission(permission);

    return contiShareRepository.save(share);
  }

  public List<ContiShare> getSharedContis(User user) {
    return contiShareRepository.findByUserAndAcceptedTrue(user);
  }

  public List<ContiShare> getPendingShares(User user) {
    return contiShareRepository.findByUserAndAcceptedFalse(user);
  }

  public List<ContiShare> getContiShares(Long contiId, User currentUser) {
    Conti conti = contiRepository.findById(contiId)
        .orElseThrow(() -> new ResourceNotFoundException("콘티를 찾을 수 없습니다. ID: " + contiId));

    validateSharePermission(conti, currentUser);

    return contiShareRepository.findByConti(conti);
  }

  public boolean hasPermission(Long contiId, Long userId, ContiSharePermission... permissions) {
    return contiShareRepository.hasPermission(contiId, userId, Arrays.asList(permissions));
  }

  private void validateSharePermission(Conti conti, User user) {
    boolean isCreator = conti.getCreator() != null && conti.getCreator().getId().equals(user.getId());
    boolean hasAdminPermission = contiShareRepository.hasPermission(
        conti.getId(), user.getId(), List.of(ContiSharePermission.ADMIN)
    );

    if (!isCreator && !hasAdminPermission) {
      throw new AuthenticationException("콘티를 공유할 권한이 없습니다.");
    }
  }

  public boolean canEditConti(Conti conti, User user) {
    if (conti.getCreator() != null && conti.getCreator().getId().equals(user.getId())) {
      return true;
    }

    return contiShareRepository.hasPermission(
        conti.getId(),
        user.getId(),
        List.of(ContiSharePermission.ADMIN, ContiSharePermission.EDIT)
    );
  }

  public boolean canViewConti(Conti conti, User user) {
    if (conti.getCreator() != null && conti.getCreator().getId().equals(user.getId())) {
      return true;
    }

    return contiShareRepository.hasPermission(
        conti.getId(),
        user.getId(),
        List.of(ContiSharePermission.ADMIN, ContiSharePermission.EDIT, ContiSharePermission.VIEW)
    );
  }
}
