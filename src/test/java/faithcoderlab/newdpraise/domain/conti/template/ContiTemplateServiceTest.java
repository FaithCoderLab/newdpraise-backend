package faithcoderlab.newdpraise.domain.conti.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faithcoderlab.newdpraise.domain.conti.Conti;
import faithcoderlab.newdpraise.domain.conti.ContiRepository;
import faithcoderlab.newdpraise.domain.conti.ContiService;
import faithcoderlab.newdpraise.domain.conti.ContiStatus;
import faithcoderlab.newdpraise.domain.conti.template.dto.ContiTemplateCreateRequest;
import faithcoderlab.newdpraise.domain.conti.template.dto.ContiTemplateUpdateRequest;
import faithcoderlab.newdpraise.domain.song.Song;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.global.exception.AuthenticationException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ContiTemplateServiceTest {

  @Mock
  private ContiTemplateRepository contiTemplateRepository;

  @Mock
  private ContiRepository contiRepository;

  @Mock
  private ContiService contiService;

  @InjectMocks
  private ContiTemplateService contiTemplateService;

  private User testUser;
  private User otherUser;
  private Conti testConti;
  private ContiTemplate testTemplate;
  private Song testSong;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .email("suming@example.com")
        .name("수밍")
        .role(Role.USER)
        .build();

    otherUser = User.builder()
        .id(2L)
        .email("other@example.com")
        .name("수밍 아님")
        .role(Role.USER)
        .build();

    testSong = Song.builder()
        .id(1L)
        .title("테스트 곡")
        .originalKey("C")
        .performanceKey("D")
        .artist("테스트 아티스트")
        .build();

    testConti = Conti.builder()
        .id(1L)
        .title("테스트 콘티")
        .description("테스트 설명")
        .scheduledAt(LocalDate.now())
        .creator(testUser)
        .songs(new ArrayList<>(Collections.singletonList(testSong)))
        .version("1.0")
        .status(ContiStatus.DRAFT)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    testTemplate = ContiTemplate.builder()
        .id(1L)
        .name("테스트 템플릿")
        .description("테스트 템플릿 설명")
        .creator(testUser)
        .songs(new ArrayList<>(Collections.singletonList(testSong)))
        .isPublic(true)
        .usageCount(5)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("콘티로부터 템플릿 생성 - 성공")
  void createTemplateFromConti_Success() {
    // given
    ContiTemplateCreateRequest request = ContiTemplateCreateRequest.builder()
        .name("새 템플릿")
        .description("테스트 용 템플릿")
        .contiId(1L)
        .isPublic(true)
        .build();

    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));
    when(contiTemplateRepository.save(any(ContiTemplate.class))).thenReturn(testTemplate);

    // when
    ContiTemplate result = contiTemplateService.createTemplate(request, testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getName()).isEqualTo("테스트 템플릿");

    verify(contiRepository).findById(1L);
    verify(contiTemplateRepository).save(any(ContiTemplate.class));
  }

  @Test
  @DisplayName("직접 템플릿 생성 - 성공")
  void createTemplateDirectly_Success() {
    // given
    ContiTemplateCreateRequest request = ContiTemplateCreateRequest.builder()
        .name("새 템플릿")
        .description("테스트 용 템플릿")
        .isPublic(true)
        .build();

    when(contiTemplateRepository.save(any(ContiTemplate.class))).thenReturn(testTemplate);

    // when
    ContiTemplate result = contiTemplateService.createTemplate(request, testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);

    verify(contiRepository, never()).findById(anyLong());
    verify(contiTemplateRepository).save(any(ContiTemplate.class));
  }

  @Test
  @DisplayName("콘티로부터 템플릿 생성 - 실패 (콘티 없음)")
  void createTemplateFromConti_NotFound() {
    // given
    ContiTemplateCreateRequest request = ContiTemplateCreateRequest.builder()
        .name("새 템플릿")
        .description("테스트 용 템플릿")
        .contiId(99L)
        .isPublic(true)
        .build();

    when(contiRepository.findById(99L)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> contiTemplateService.createTemplate(request, testUser))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("콘티를 찾을 수 없습니다");

    verify(contiRepository).findById(99L);
    verify(contiTemplateRepository, never()).save(any(ContiTemplate.class));
  }

  @Test
  @DisplayName("콘티로부터 템플릿 생성 - 실패 (권한 없음)")
  void createTemplateFromConti_Unauthorized() {
    // given
    ContiTemplateCreateRequest request = ContiTemplateCreateRequest.builder()
        .name("새 템플릿")
        .description("테스트 용 템플릿")
        .contiId(1L)
        .isPublic(true)
        .build();

    Conti otherUserConti = Conti.builder()
        .id(1L)
        .title("다른 사용자의 콘티")
        .creator(otherUser)
        .build();

    when(contiRepository.findById(1L)).thenReturn(Optional.of(otherUserConti));

    // when & then
    assertThatThrownBy(() -> contiTemplateService.createTemplate(request, testUser))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("템플릿을 생성할 권한이 없습니다");

    verify(contiRepository).findById(1L);
    verify(contiTemplateRepository, never()).save(any(ContiTemplate.class));
  }

  @Test
  @DisplayName("템플릿 조회 - 성공")
  void getTemplate_Success() {
    // given
    when(contiTemplateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));

    // when
    ContiTemplate result = contiTemplateService.getTemplate(1L);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getName()).isEqualTo("테스트 템플릿");

    verify(contiTemplateRepository).findById(1L);
  }

  @Test
  @DisplayName("템플릿 조회 - 실패 (템플릿 없음)")
  void getTemplate_NotFound() {
    // given
    when(contiTemplateRepository.findById(99L)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> contiTemplateService.getTemplate(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("템플릿을 찾을 수 없습니다");

    verify(contiTemplateRepository).findById(99L);
  }

  @Test
  @DisplayName("접근 가능한 템플릿 조회 - 공개 템플릿 성공")
  void getAccessibleTemplate_PublicSuccess() {
    // given
    when(contiTemplateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));

    // when
    ContiTemplate result = contiTemplateService.getAccessibleTemplate(1L, otherUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);

    verify(contiTemplateRepository).findById(1L);
  }

  @Test
  @DisplayName("접근 가능한 템플릿 조회 - 본인 비공개 템플릿 성공")
  void getAccessibleTemplate_OwnerSuccess() {
    // given
    ContiTemplate privateTemplate = ContiTemplate.builder()
        .id(1L)
        .name("비공개 템플릿")
        .creator(testUser)
        .isPublic(false)
        .build();

    when(contiTemplateRepository.findById(1L)).thenReturn(Optional.of(privateTemplate));

    // when
    ContiTemplate result = contiTemplateService.getAccessibleTemplate(1L, testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.isPublic()).isFalse();

    verify(contiTemplateRepository).findById(1L);
  }

  @Test
  @DisplayName("접근 가능한 템플릿 조회 - 실패 (권한 없음)")
  void getAccessibleTemplate_Unauthorized() {
    // given
    ContiTemplate privateTemplate = ContiTemplate.builder()
        .id(1L)
        .name("비공개 템플릿")
        .creator(testUser)
        .isPublic(false)
        .build();

    when(contiTemplateRepository.findById(1L)).thenReturn(Optional.of(privateTemplate));

    // when & then
    assertThatThrownBy(() -> contiTemplateService.getAccessibleTemplate(1L, otherUser))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("해당 템플릿에 접근할 권한이 없습니다");

    verify(contiTemplateRepository).findById(1L);
  }

  @Test
  @DisplayName("템플릿 수정 - 성공")
  void updateTemplate_Success() {
    // given
    ContiTemplateUpdateRequest request = ContiTemplateUpdateRequest.builder()
        .name("수정된 템플릿")
        .description("수정된 설명")
        .isPublic(false)
        .build();

    when(contiTemplateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(contiTemplateRepository.save(any(ContiTemplate.class))).thenReturn(testTemplate);

    // when
    ContiTemplate result = contiTemplateService.updateTemplate(1L, request, testUser);

    // then
    assertThat(result).isNotNull();
    verify(contiTemplateRepository).findById(1L);
    verify(contiTemplateRepository).save(testTemplate);
  }

  @Test
  @DisplayName("템플릿 수정 - 실패 (권한 없음)")
  void updateTemplate_Unauthorized() {
    // given
    ContiTemplateUpdateRequest request = ContiTemplateUpdateRequest.builder()
        .name("수정된 템플릿")
        .build();

    when(contiTemplateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));

    // when & then
    assertThatThrownBy(() -> contiTemplateService.updateTemplate(1L, request, otherUser))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("해당 템플릿을 수정할 권한이 없습니다");

    verify(contiTemplateRepository).findById(1L);
    verify(contiTemplateRepository, never()).save(any(ContiTemplate.class));
  }

  @Test
  @DisplayName("템플릿 삭제 - 성공")
  void deleteTemplate_Success() {
    // given
    when(contiTemplateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));

    // when
    contiTemplateService.deleteTemplate(1L, testUser);

    // then
    verify(contiTemplateRepository).findById(1L);
    verify(contiTemplateRepository).delete(testTemplate);
  }

  @Test
  @DisplayName("템플릿 삭제 - 실패 (권한 없음)")
  void deleteTemplate_Unauthorized() {
    // given
    when(contiTemplateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));

    // when & then
    assertThatThrownBy(() -> contiTemplateService.deleteTemplate(1L, otherUser))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("해당 템플릿을 삭제할 권한이 없습니다");

    verify(contiTemplateRepository).findById(1L);
    verify(contiTemplateRepository, never()).delete(any(ContiTemplate.class));
  }

  @Test
  @DisplayName("사용자 템플릿 목록 조회 - 성공")
  void getUserTemplates_Success() {
    // given
    List<ContiTemplate> templates = Collections.singletonList(testTemplate);
    when(contiTemplateRepository.findByCreator(testUser)).thenReturn(templates);

    // when
    List<ContiTemplate> result = contiTemplateService.getUserTemplates(testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.get(0).getId()).isEqualTo(1L);

    verify(contiTemplateRepository).findByCreator(testUser);
  }

  @Test
  @DisplayName("사용자 템플릿 목록 페이징 조회 - 성공")
  void getUserTemplatesPaged_Success() {
    // given
    Page<ContiTemplate> templatePage = new PageImpl<>(Collections.singletonList(testTemplate));
    when(contiTemplateRepository.findByCreator(any(User.class), any(Pageable.class))).thenReturn(
        templatePage);

    // when
    Page<ContiTemplate> results = contiTemplateService.getUserTemplates(testUser, Pageable.unpaged());

    // then
    assertThat(results.getTotalElements()).isEqualTo(1);
    assertThat(results.getContent().get(0).getId()).isEqualTo(1L);

    verify(contiTemplateRepository).findByCreator(any(User.class), any(Pageable.class));
  }

  @Test
  @DisplayName("공개 템플릿 목록 조회 - 성공")
  void getPublicTemplates_Success() {
    // given
    List<ContiTemplate> templates = Collections.singletonList(testTemplate);
    when(contiTemplateRepository.findByIsPublicTrue()).thenReturn(templates);

    // when
    List<ContiTemplate> results = contiTemplateService.getPublicTemplates();

    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getId()).isEqualTo(1L);

    verify(contiTemplateRepository).findByIsPublicTrue();
  }

  @Test
  @DisplayName("접근 가능한 템플릿 목록 조회 - 성공")
  void getAccessibleTemplates_Success() {
    // given
    List<ContiTemplate> templates = Collections.singletonList(testTemplate);
    when(contiTemplateRepository.findAccessibleTemplates(testUser.getId())).thenReturn(templates);

    // when
    List<ContiTemplate> results = contiTemplateService.getAccessibleTemplates(testUser);

    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getId()).isEqualTo(1L);

    verify(contiTemplateRepository).findAccessibleTemplates(testUser.getId());
  }

  @Test
  @DisplayName("인기 템플릿 목록 조회 - 성공")
  void getPopularTemplates_Success() {
    // given
    List<ContiTemplate> templates = Collections.singletonList(testTemplate);
    when(contiTemplateRepository.findPopularTemplates(any(Pageable.class))).thenReturn(templates);

    // when
    List<ContiTemplate> results = contiTemplateService.getPopularTemplates(5);

    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getId()).isEqualTo(1L);

    verify(contiTemplateRepository).findPopularTemplates(any(Pageable.class));
  }

  @Test
  @DisplayName("템플릿 적용 - 성공")
  void applyTemplate_Success() {
    // given
    when(contiTemplateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(contiRepository.save(any(Conti.class))).thenReturn(testConti);

    // when
    Conti result = contiTemplateService.applyTemplate(1L, testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);

    verify(contiTemplateRepository).findById(1L);
    verify(contiTemplateRepository).save(testTemplate);
    verify(contiRepository).save(any(Conti.class));
  }

  @Test
  @DisplayName("템플릿 적용 - 실패 (권한 없음)")
  void applyTemplate_Unauthorized() {
    // given
    ContiTemplate privateTemplate = ContiTemplate.builder()
        .id(1L)
        .name("비공개 템플릿")
        .creator(testUser)
        .isPublic(false)
        .build();

    when(contiTemplateRepository.findById(1L)).thenReturn(Optional.of(privateTemplate));

    // when & then
    assertThatThrownBy(() -> contiTemplateService.applyTemplate(1L, otherUser))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("해당 템플릿에 접근할 권한이 없습니다");

    verify(contiTemplateRepository).findById(1L);
    verify(contiRepository, never()).save(any(Conti.class));
  }
}
