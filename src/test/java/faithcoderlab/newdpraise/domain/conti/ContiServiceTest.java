package faithcoderlab.newdpraise.domain.conti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faithcoderlab.newdpraise.domain.conti.dto.ContiCreateRequest;
import faithcoderlab.newdpraise.domain.song.Song;
import faithcoderlab.newdpraise.domain.song.SongRepository;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
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
class ContiServiceTest {

  @Mock
  private ContiRepository contiRepository;

  @Mock
  private SongRepository songRepository;

  @Mock
  private ContiParserService contiParserService;

  @InjectMocks
  private ContiService contiService;

  private User testUser;
  private Conti testConti;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .email("suming@example.com")
        .name("수밍")
        .role(Role.USER)
        .build();

    testConti = Conti.builder()
        .id(1L)
        .title("테스트 콘티")
        .scheduledAt(LocalDate.now())
        .creator(testUser)
        .version("1.0")
        .status(ContiStatus.DRAFT)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("콘티 생성 - 텍스트 형태")
  void createContiFromText() {
    // given
    ContiCreateRequest request = ContiCreateRequest.builder()
        .contiText("20250405 찬양집회 콘티\n1. 물댄 동산 G")
        .build();

    when(contiParserService.parseContiText(anyString(), any(User.class)))
        .thenReturn(testConti);
    when(contiRepository.save(any(Conti.class))).thenReturn(testConti);

    // when
    Conti result = contiService.createConti(request, testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    verify(contiParserService).parseContiText(request.getContiText(), testUser);
    verify(contiRepository).save(any(Conti.class));
  }

  @Test
  @DisplayName("콘티 생성 - 구조화된 데이터")
  void createContiFromStructuredData() {
    // given
    List<ContiCreateRequest.SongDto> songDtos = Collections.singletonList(
        ContiCreateRequest.SongDto.builder()
            .title("테스트 곡")
            .originalKey("C")
            .performanceKey("D")
            .build()
    );

    ContiCreateRequest request = ContiCreateRequest.builder()
        .title("새 콘티")
        .description("테스트 설명")
        .scheduledAt(LocalDate.now())
        .songs(songDtos)
        .build();

    List<Song> savedSongs = Collections.singletonList(
        Song.builder()
            .id(1L)
            .title("테스트 곡")
            .originalKey("C")
            .performanceKey("D")
            .build()
    );

    when(songRepository.saveAll(anyList())).thenReturn(savedSongs);
    when(contiRepository.save(any(Conti.class))).thenReturn(testConti);

    // when
    Conti result = contiService.createConti(request, testUser);

    // then
    assertThat(result).isNotNull();
    verify(songRepository).saveAll(anyList());
    verify(contiRepository).save(any(Conti.class));
  }

  @Test
  @DisplayName("사용자 콘티 목록 조회")
  void getUserContiList() {
    // given
    List<Conti> contiList = Collections.singletonList(testConti);
    when(contiRepository.findByCreatorOrderByScheduledAtDesc(testUser))
        .thenReturn(contiList);

    // when
    List<Conti> result = contiService.getUserContiList(testUser);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("콘티 ID로 조회 - 성공")
  void getContiByIdSuccess() {
    // given
    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));

    // when
    Conti result = contiService.getContiById(1L);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("콘티 ID로 조회 - 실패")
  void getContiByIdNotFound() {
    // given
    when(contiRepository.findById(999L)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> contiService.getContiById(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("콘티를 찾을 수 없습니다");
  }

  @Test
  @DisplayName("콘티 상태 업데이트")
  void updateContiStatus() {
    // given
    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));
    when(contiRepository.save(any(Conti.class))).thenReturn(testConti);

    // when
    contiService.updateContiStatus(1L, ContiStatus.FINALIZED);

    // then
    verify(contiRepository).save(any(Conti.class));
  }

  @Test
  @DisplayName("콘티 삭제")
  void deleteConti() {
    // given
    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));

    // when
    contiService.deleteConti(1L);

    // then
    verify(contiRepository).delete(testConti);
  }
}
