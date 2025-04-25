package faithcoderlab.newdpraise.domain.conti.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faithcoderlab.newdpraise.domain.conti.Conti;
import faithcoderlab.newdpraise.domain.conti.ContiRepository;
import faithcoderlab.newdpraise.domain.conti.ContiStatus;
import faithcoderlab.newdpraise.domain.conti.share.dto.ContiShareRequest;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import faithcoderlab.newdpraise.global.exception.AuthenticationException;
import faithcoderlab.newdpraise.global.exception.ResourceAlreadyExistsException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContiShareServiceTest {

  @Mock
  private ContiShareRepository contiShareRepository;

  @Mock
  private ContiRepository contiRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private ContiShareService contiShareService;

  private User creator;
  private User targetUser;
  private User adminUser;
  private Conti testConti;
  private ContiShare testShare;

  @BeforeEach
  void setUp() {
    creator = User.builder()
        .id(1L)
        .email("creator@example.com")
        .name("Creator")
        .role(Role.USER)
        .build();

    targetUser = User.builder()
        .id(2L)
        .email("target@example.com")
        .name("Target User")
        .role(Role.USER)
        .build();

    adminUser = User.builder()
        .id(3L)
        .email("admin@example.com")
        .role(Role.USER)
        .build();

    testConti = Conti.builder()
        .id(1L)
        .title("Test Conti")
        .description("Test Description")
        .scheduledAt(LocalDate.now())
        .creator(creator)
        .status(ContiStatus.DRAFT)
        .version("1.0")
        .createdAt(LocalDateTime.now())
        .build();

    testShare = ContiShare.builder()
        .id(1L)
        .conti(testConti)
        .user(targetUser)
        .sharedBy(creator)
        .permission(ContiSharePermission.VIEW)
        .accepted(false)
        .invitedAt(LocalDateTime.now())
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 성공")
  void createShareSuccess() {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(1L);
    request.setUserEmail("target@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));
    when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(targetUser));
    when(contiShareRepository.findByContiAndUser(testConti, targetUser)).thenReturn(
        Optional.empty());
    when(contiShareRepository.save(any(ContiShare.class))).thenReturn(testShare);

    // when
    ContiShare result = contiShareService.createShare(request, creator);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getConti()).isEqualTo(testConti);
    assertThat(result.getUser()).isEqualTo(targetUser);
    assertThat(result.getSharedBy()).isEqualTo(creator);
    assertThat(result.getPermission()).isEqualTo(ContiSharePermission.VIEW);
    assertThat(result.isAccepted()).isFalse();

    verify(contiRepository).findById(1L);
    verify(userRepository).findByEmail("target@example.com");
    verify(contiShareRepository).findByContiAndUser(testConti, targetUser);
    verify(contiShareRepository).save(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 실패 (콘티 없음)")
  void createShareFailNoConti() {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(999L);
    request.setUserEmail("target@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    when(contiRepository.findById(999L)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> contiShareService.createShare(request, creator))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("콘티를 찾을 수 없습니다");

    verify(contiRepository).findById(999L);
    verify(userRepository, never()).findByEmail(anyString());
    verify(contiShareRepository, never()).save(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 실패 (사용자 없음)")
  void createShareFailNoUser() {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(1L);
    request.setUserEmail("nonexistent@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));
    when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> contiShareService.createShare(request, creator))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("사용자를 찾을 수 없습니다");

    verify(contiRepository).findById(1L);
    verify(userRepository).findByEmail("nonexistent@example.com");
    verify(contiShareRepository, never()).save(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 실패 (자기 자신에게 공유)")
  void createShareFailSelfShare() {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(1L);
    request.setUserEmail("creator@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));
    when(userRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(creator));

    // when & then
    assertThatThrownBy(() -> contiShareService.createShare(request, creator))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("자신에게는 콘티를 공유할 수 없습니다");

    verify(contiRepository).findById(1L);
    verify(userRepository).findByEmail("creator@example.com");
    verify(contiShareRepository, never()).save(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 실패 (이미 공유됨)")
  void createShareFailAlreadyShared() {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(1L);
    request.setUserEmail("target@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));
    when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(targetUser));
    when(contiShareRepository.findByContiAndUser(testConti, targetUser)).thenReturn(
        Optional.of(testShare));

    // when & then
    assertThatThrownBy(() -> contiShareService.createShare(request, creator))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessageContaining("이미 해당 사용자에게 공유된 콘티입니다");

    verify(contiRepository).findById(1L);
    verify(userRepository).findByEmail("target@example.com");
    verify(contiShareRepository).findByContiAndUser(testConti, targetUser);
    verify(contiShareRepository, never()).save(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 실패 (권한 없음)")
  void createShareFailNoPermission() {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(1L);
    request.setUserEmail("target@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    User otherUser = User.builder()
        .id(4L)
        .email("other@example.com")
        .name("Other User")
        .role(Role.USER)
        .build();

    Conti conti = Conti.builder()
        .id(1L)
        .title("Test Conti")
        .creator(creator)
        .build();

    when(contiRepository.findById(1L)).thenReturn(Optional.of(conti));
    when(contiShareRepository.hasPermission(1L, 4L,
        List.of(ContiSharePermission.ADMIN))).thenReturn(false);

    // when & then
    assertThatThrownBy(() -> contiShareService.createShare(request, otherUser))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("콘티를 공유할 권한이 없습니다");

    verify(contiRepository).findById(1L);
    verify(contiShareRepository).hasPermission(1L, 4L, List.of(ContiSharePermission.ADMIN));
    verify(userRepository, never()).findByEmail(anyString());
    verify(contiShareRepository, never()).save(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 초대 수락 - 성공")
  void acceptShareSuccess() {
    // given
    when(contiShareRepository.findById(1L)).thenReturn(Optional.of(testShare));
    when(contiShareRepository.save(any(ContiShare.class))).thenReturn(testShare);

    // when
    ContiShare result = contiShareService.acceptShare(1L, targetUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.isAccepted()).isTrue();
    assertThat(result.getAcceptedAt()).isNotNull();

    verify(contiShareRepository).findById(1L);
    verify(contiShareRepository).save(testShare);
  }

  @Test
  @DisplayName("콘티 공유 초대 수락 - 실패 (공유 정보 없음)")
  void acceptShareFailNoShare() {
    // given
    when(contiShareRepository.findById(999L)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> contiShareService.acceptShare(999L, targetUser))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("공유 정보를 찾을 수 없습니다");

    verify(contiShareRepository).findById(999L);
    verify(contiShareRepository, never()).save(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 초대 수락 - 실패 (다른 사용자)")
  void acceptShareFailWrongUser() {
    // given
    when(contiShareRepository.findById(1L)).thenReturn(Optional.of(testShare));

    // when & then
    assertThatThrownBy(() -> contiShareService.acceptShare(1L, creator))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("해당 공유 요청에 대한 권한이 없습니다");

    verify(contiShareRepository).findById(1L);
    verify(contiShareRepository, never()).save(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 초대 수락 - 실패 (이미 수락됨)")
  void acceptShareFailAlreadyAccepted() {
    // given
    ContiShare acceptedShare = ContiShare.builder()
        .id(1L)
        .conti(testConti)
        .user(targetUser)
        .sharedBy(creator)
        .permission(ContiSharePermission.VIEW)
        .accepted(true)
        .invitedAt(LocalDateTime.now())
        .acceptedAt(LocalDateTime.now())
        .build();

    when(contiShareRepository.findById(1L)).thenReturn(Optional.of(acceptedShare));

    // when & then
    assertThatThrownBy(() -> contiShareService.acceptShare(1L, targetUser))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessageContaining("이미 수락된 공유 요청입니다");

    verify(contiShareRepository).findById(1L);
    verify(contiShareRepository, never()).save(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 삭제 - 성공 (공유 받은 사용자)")
  void deleteShareSuccessAsUser() {
    // given
    when(contiShareRepository.findById(1L)).thenReturn(Optional.of(testShare));

    // when
    contiShareService.deleteShare(1L, targetUser);

    // then
    verify(contiShareRepository).findById(1L);
    verify(contiShareRepository).delete(testShare);
  }

  @Test
  @DisplayName("콘티 공유 삭제 - 성공 (공유한 사용자)")
  void deleteShareSuccessAsSharer() {
    // given
    when(contiShareRepository.findById(1L)).thenReturn(Optional.of(testShare));

    // when
    contiShareService.deleteShare(1L, creator);

    // then
    verify(contiShareRepository).findById(1L);
    verify(contiShareRepository).delete(testShare);
  }

  @Test
  @DisplayName("콘티 공유 삭제 - 실패 (공유 정보 없음)")
  void deleteShareFailNoShare() {
    // given
    when(contiShareRepository.findById(999L)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> contiShareService.deleteShare(999L, targetUser))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("공유 정보를 찾을 수 없습니다");

    verify(contiShareRepository).findById(999L);
    verify(contiShareRepository, never()).delete(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 삭제 - 실패 (권한 없음)")
  void deleteShareFailNoPermission() {
    // given
    User otherUser = User.builder()
        .id(4L)
        .email("other@example.com")
        .name("Other User")
        .role(Role.USER)
        .build();

    when(contiShareRepository.findById(1L)).thenReturn(Optional.of(testShare));

    // when & then
    assertThatThrownBy(() -> contiShareService.deleteShare(1L, otherUser))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("해당 공유 요청을 삭제할 권한이 없습니다");

    verify(contiShareRepository).findById(1L);
    verify(contiShareRepository, never()).delete(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 권한 수정 - 성공")
  void updateSharePermissionSuccess() {
    // given
    when(contiShareRepository.findById(1L)).thenReturn(Optional.of(testShare));
    when(contiShareRepository.save(any(ContiShare.class))).thenReturn(testShare);

    // when
    ContiShare result = contiShareService.updateSharePermission(1L, ContiSharePermission.EDIT,
        creator);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getPermission()).isEqualTo(ContiSharePermission.EDIT);

    verify(contiShareRepository).findById(1L);
    verify(contiShareRepository).save(testShare);
  }

  @Test
  @DisplayName("콘티 공유 권한 수정 - 실패 (공유 정보 없음)")
  void updateSharePermissionFailNoShare() {
    // given
    when(contiShareRepository.findById(999L)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
        () -> contiShareService.updateSharePermission(999L, ContiSharePermission.EDIT, creator))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("공유 정보를 찾을 수 없습니다");

    verify(contiShareRepository).findById(999L);
    verify(contiShareRepository, never()).delete(any(ContiShare.class));
  }

  @Test
  @DisplayName("콘티 공유 권한 수정 - 실패 (권한 없음)")
  void updateSharePermissionFailNoPermission() {
    // given
    User otherUser = User.builder()
        .id(4L)
        .email("other@example.com")
        .name("Other User")
        .role(Role.USER)
        .build();

    when(contiShareRepository.findById(1L)).thenReturn(Optional.of(testShare));

    // when & then
    assertThatThrownBy(
        () -> contiShareService.updateSharePermission(1L, ContiSharePermission.EDIT, otherUser))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("해당 공유 요청의 권한을 수정할 권한이 없습니다");

    verify(contiShareRepository).findById(1L);
    verify(contiShareRepository, never()).save(any(ContiShare.class));
  }

  @Test
  @DisplayName("사용자에게 공유된 콘티 목록 조회")
  void getSharedContis() {
    // given
    ContiShare acceptedShare = ContiShare.builder()
        .id(1L)
        .conti(testConti)
        .user(targetUser)
        .sharedBy(creator)
        .permission(ContiSharePermission.VIEW)
        .accepted(true)
        .build();

    when(contiShareRepository.findByUserAndAcceptedTrue(targetUser)).thenReturn(
        Collections.singletonList(acceptedShare));

    // when
    List<ContiShare> result = contiShareService.getSharedContis(targetUser);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getConti()).isEqualTo(testConti);
    assertThat(result.get(0).isAccepted()).isTrue();

    verify(contiShareRepository).findByUserAndAcceptedTrue(targetUser);
  }

  @Test
  @DisplayName("사용자가 받은 콘티 공유 요청 목록 조회")
  void getPendingShares() {
    // given
    when(contiShareRepository.findByUserAndAcceptedFalse(targetUser)).thenReturn(
        Collections.singletonList(testShare));

    // when
    List<ContiShare> result = contiShareService.getPendingShares(targetUser);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getConti()).isEqualTo(testConti);
    assertThat(result.get(0).isAccepted()).isFalse();

    verify(contiShareRepository).findByUserAndAcceptedFalse(targetUser);
  }

  @Test
  @DisplayName("콘티에 대한 공유 목록 조회 - 성공")
  void getContiSharesSuccess() {
    // given
    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));
    when(contiShareRepository.findByConti(testConti)).thenReturn(
        Collections.singletonList(testShare));

    // when
    List<ContiShare> result = contiShareService.getContiShares(1L, creator);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getUser()).isEqualTo(targetUser);
    assertThat(result.get(0).getPermission()).isEqualTo(ContiSharePermission.VIEW);

    verify(contiRepository).findById(1L);
    verify(contiShareRepository).findByConti(testConti);
  }

  @Test
  @DisplayName("콘티에 대한 공유 목록 조회 - 실패 (콘티 없음)")
  void getContiSharesFailNoConti() {
    // given
    when(contiRepository.findById(999L)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> contiShareService.getContiShares(999L, creator))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("콘티를 찾을 수 없습니다");

    verify(contiRepository).findById(999L);
    verify(contiShareRepository, never()).findByConti(any(Conti.class));
  }

  @Test
  @DisplayName("콘티에 대한 공유 목록 조회 - 실패 (권한 없음)")
  void getContiSharesFailNoPermission() {
    // given
    User otherUser = User.builder()
        .id(4L)
        .email("other@example.com")
        .name("Other User")
        .role(Role.USER)
        .build();

    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));
    when(
        contiShareRepository.hasPermission(1L, 4L, List.of(ContiSharePermission.ADMIN))).thenReturn(
        false);

    // when & then
    assertThatThrownBy(() -> contiShareService.getContiShares(1L, otherUser))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("콘티를 공유할 권한이 없습니다");

    verify(contiRepository).findById(1L);
    verify(contiShareRepository, never()).findByConti(any(Conti.class));
  }

  @Test
  @DisplayName("권한 확인 - 콘티 편집 가능")
  void canEditContiSuccess() {
    // given & when
    boolean creatorCanEdit = contiShareService.canEditConti(testConti, creator);
    // then
    assertThat(creatorCanEdit).isTrue();

    when(contiShareRepository.hasPermission(1L, 3L,
        List.of(ContiSharePermission.ADMIN, ContiSharePermission.EDIT)))
        .thenReturn(true);
    // when
    boolean adminCanEdit = contiShareService.canEditConti(testConti, adminUser);
    // then
    assertThat(adminCanEdit).isTrue();
    verify(contiShareRepository).hasPermission(1L, 3L,
        List.of(ContiSharePermission.ADMIN, ContiSharePermission.EDIT));
  }

  @Test
  @DisplayName("권한 확인 - 콘티 편집 불가")
  void canEditContiFail() {
    // given
    User otherUser = User.builder()
        .id(4L)
        .email("other@example.com")
        .name("Other User")
        .role(Role.USER)
        .build();

    when(contiShareRepository.hasPermission(1L, 4L,
        List.of(ContiSharePermission.ADMIN, ContiSharePermission.EDIT)))
        .thenReturn(false);

    // when
    boolean otherCanEdit = contiShareService.canEditConti(testConti, otherUser);

    // then
    assertThat(otherCanEdit).isFalse();
    verify(contiShareRepository).hasPermission(1L, 4L,
        List.of(ContiSharePermission.ADMIN, ContiSharePermission.EDIT));
  }

  @Test
  @DisplayName("권한 확인 - 콘티 조회 가능")
  void canViewContiSuccess() {
    // given & when
    boolean creatorCanView = contiShareService.canViewConti(testConti, creator);
    // then
    assertThat(creatorCanView).isTrue();

    when(contiShareRepository.hasPermission(1L, 2L,
        List.of(ContiSharePermission.ADMIN, ContiSharePermission.EDIT, ContiSharePermission.VIEW)))
        .thenReturn(true);
    // when
    boolean userCanView = contiShareService.canViewConti(testConti, targetUser);
    // when
    assertThat(userCanView).isTrue();
    verify(contiShareRepository).hasPermission(1L, 2L,
        List.of(ContiSharePermission.ADMIN, ContiSharePermission.EDIT, ContiSharePermission.VIEW));
  }

  @Test
  @DisplayName("권한 확인 - 콘티 조회 불가")
  void canViewContiFail() {
    // given
    User otherUser = User.builder()
        .id(4L)
        .email("other@example.com")
        .name("Other User")
        .role(Role.USER)
        .build();

    when(contiShareRepository.hasPermission(1L, 4L,
        List.of(ContiSharePermission.ADMIN, ContiSharePermission.EDIT, ContiSharePermission.VIEW)))
        .thenReturn(false);

    // when
    boolean otherCanView = contiShareService.canViewConti(testConti, otherUser);

    // then
    assertThat(otherCanView).isFalse();
    verify(contiShareRepository).hasPermission(1L, 4L,
        List.of(ContiSharePermission.ADMIN, ContiSharePermission.EDIT, ContiSharePermission.VIEW));
  }
}
