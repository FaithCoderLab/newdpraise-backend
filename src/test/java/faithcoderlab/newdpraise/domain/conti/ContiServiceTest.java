package faithcoderlab.newdpraise.domain.conti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faithcoderlab.newdpraise.domain.conti.dto.ContiCreateRequest;
import faithcoderlab.newdpraise.domain.conti.dto.ContiSearchRequest;
import faithcoderlab.newdpraise.domain.conti.dto.ContiUpdateRequest;
import faithcoderlab.newdpraise.domain.song.Song;
import faithcoderlab.newdpraise.domain.song.SongRepository;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

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
  private Song testSong;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .email("suming@example.com")
        .name("수밍")
        .role(Role.USER)
        .build();

    testSong = Song.builder()
        .id(1L)
        .title("테스트 곡")
        .originalKey("C")
        .performanceKey("D")
        .build();

    testConti = Conti.builder()
        .id(1L)
        .title("테스트 콘티")
        .scheduledAt(LocalDate.now())
        .creator(testUser)
        .version("1.0")
        .status(ContiStatus.DRAFT)
        .songs(new ArrayList<>(Collections.singletonList(testSong)))
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
  @DisplayName("콘티 업데이트 - 성공")
  void updateContiSuccess() {
    // given
    Long contiId = 1L;
    ContiUpdateRequest request = ContiUpdateRequest.builder()
        .title("업데이트된 콘티")
        .description("업데이트된 설명")
        .scheduledAt(LocalDate.now().plusDays(1))
        .status(ContiStatus.FINALIZED)
        .build();

    when(contiRepository.findById(contiId)).thenReturn(Optional.of(testConti));
    when(contiRepository.save(any(Conti.class))).thenReturn(testConti);

    // when
    Conti result = contiService.updateConti(contiId, request, testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo(testConti.getTitle());
    assertThat(result.getStatus()).isEqualTo(testConti.getStatus());
    verify(contiRepository).save(testConti);
  }

  @Test
  @DisplayName("콘티 업데이트 - 곡 목록 포함")
  void updateContiWithSongs() {
    // given
    Long contiId = 1L;
    List<ContiUpdateRequest.SongDto> songDtos = Arrays.asList(
        ContiUpdateRequest.SongDto.builder()
            .id(1L)
            .title("업데이트된 곡")
            .originalKey("D")
            .performanceKey("E")
            .build(),
        ContiUpdateRequest.SongDto.builder()
            .title("새 곡")
            .originalKey("F")
            .performanceKey("G")
            .build()
    );

    ContiUpdateRequest request = ContiUpdateRequest.builder()
        .title("업데이트된 콘티")
        .songs(songDtos)
        .build();

    Song updatedSong = Song.builder()
        .id(1L)
        .title("업데이트된 곡")
        .originalKey("D")
        .performanceKey("E")
        .build();

    Song newSong = Song.builder()
        .id(2L)
        .title("새 곡")
        .originalKey("F")
        .performanceKey("G")
        .build();

    List<Song> updatedSongs = Arrays.asList(updatedSong, newSong);

    when(contiRepository.findById(contiId)).thenReturn(Optional.of(testConti));
    when(songRepository.findById(1L)).thenReturn(Optional.of(testSong));
    when(songRepository.saveAll(anyList())).thenReturn(updatedSongs);
    when(contiRepository.save(any(Conti.class))).thenReturn(testConti);

    // when
    Conti result = contiService.updateConti(contiId, request, testUser);

    // then
    assertThat(result).isNotNull();
    verify(songRepository).findById(1L);
    verify(songRepository).saveAll(anyList());
    verify(contiRepository).save(testConti);
  }

  @Test
  @DisplayName("콘티 업데이트 - 권한 없음")
  void updateContiNotAuthorized() {
    // given
    Long contiId = 1L;
    User anotherUser = User.builder()
        .id(2L)
        .email("another@example.com")
        .name("다른 사용자")
        .role(Role.USER)
        .build();

    Conti conti = Conti.builder()
        .id(contiId)
        .title("테스트 콘티")
        .creator(anotherUser)
        .build();

    ContiUpdateRequest request = ContiUpdateRequest.builder()
        .title("업데이트 시도")
        .build();

    when(contiRepository.findById(contiId)).thenReturn(Optional.of(conti));

    // when & then
    assertThatThrownBy(() -> contiService.updateConti(contiId, request, testUser))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("해당 사용자의 콘티를 찾을 수 없습니다");

    verify(contiRepository, never()).save(any(Conti.class));
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
  @DisplayName("페이징 처리된 사용자 콘티 목록 조회")
  void getUserContiListWithPaging() {
    // given
    Page<Conti> contiPage = new PageImpl<>(Collections.singletonList(testConti));
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Direction.DESC, "scheduledAt"));

    when(contiRepository.findByCreator(testUser, pageable)).thenReturn(contiPage);

    // when
    Page<Conti> result = contiService.getUserContiList(testUser, pageable);

    // then
    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
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
  @DisplayName("날짜 범위로 콘티 검색")
  void searchByDateRange() {
    // given
    LocalDate startDate = LocalDate.now().minusDays(7);
    LocalDate endDate = LocalDate.now();
    List<Conti> contiList = Collections.singletonList(testConti);

    when(contiRepository.findByScheduledAtBetweenOrderByScheduledAtDesc(startDate, endDate))
        .thenReturn(contiList);

    // when
    List<Conti> result = contiService.searchByDateRange(startDate, endDate);

    // then
    assertThat(result).hasSize(1);
    verify(contiRepository).findByScheduledAtBetweenOrderByScheduledAtDesc(startDate, endDate);
  }

  @Test
  @DisplayName("제목으로 콘티 검색")
  void searchByTitle() {
    // given
    String keyword = "테스트";
    List<Conti> contiList = Collections.singletonList(testConti);

    when(contiRepository.findByTitleContainingIgnoreCaseOrderByScheduledAtDesc(keyword)).thenReturn(
        contiList);

    // when
    List<Conti> result = contiService.searchByTitle(keyword);

    // then
    assertThat(result).hasSize(1);
    verify(contiRepository).findByTitleContainingIgnoreCaseOrderByScheduledAtDesc(keyword);
  }

  @Test
  @DisplayName("사용자별 제목으로 콘티 검색")
  void searchByTitleForUser() {
    // given
    String keyword = "테스트";
    List<Conti> contiList = Collections.singletonList(testConti);

    when(contiRepository.findByCreatorAndTitleContainingIgnoreCaseOrderByScheduledAtDesc(testUser,
        keyword)).thenReturn(contiList);

    // when
    List<Conti> result = contiService.searchByTitleForUser(testUser, keyword);

    // then
    assertThat(result).hasSize(1);
    verify(contiRepository).findByCreatorAndTitleContainingIgnoreCaseOrderByScheduledAtDesc(
        testUser, keyword
    );
  }

  @Test
  @DisplayName("날짜 범위와 제목으로 콘티 검색")
  void searchByDateRangeAndTitle() {
    // given
    LocalDate startDate = LocalDate.now().minusDays(7);
    LocalDate endDate = LocalDate.now();
    String keyword = "테스트";
    List<Conti> contiList = Collections.singletonList(testConti);

    when(contiRepository.findByScheduledAtBetweenAndTitleContainingIgnoreCaseOrderByScheduledAtDesc(
        startDate, endDate, keyword
    )).thenReturn(contiList);

    // when
    List<Conti> result = contiService.searchByDateRangeAndTitle(startDate, endDate, keyword);

    // then
    assertThat(result).hasSize(1);
    verify(
        contiRepository).findByScheduledAtBetweenAndTitleContainingIgnoreCaseOrderByScheduledAtDesc(
        startDate, endDate, keyword
    );
  }

  @Test
  @DisplayName("고급 검색 기능")
  void advancedSearch() {
    // given
    ContiSearchRequest request = ContiSearchRequest.builder()
        .startDate(LocalDate.now().minusDays(7))
        .endDate(LocalDate.now())
        .title("테스트")
        .creatorId(1L)
        .status(ContiStatus.DRAFT)
        .build();

    Pageable pageable = PageRequest.of(0, 10, Sort.by(Direction.DESC, "scheduledAt"));
    Page<Conti> contiPage = new PageImpl<>(Collections.singletonList(testConti));

    when(contiRepository.searchContis(
        request.getStartDate(),
        request.getEndDate(),
        request.getTitle(),
        request.getCreatorId(),
        request.getStatus(),
        pageable
    )).thenReturn(contiPage);

    // when
    Page<Conti> result = contiService.advancedSearch(request, pageable);

    // then
    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(contiRepository).searchContis(
        request.getStartDate(),
        request.getEndDate(),
        request.getTitle(),
        request.getCreatorId(),
        request.getStatus(),
        pageable
    );
  }

  @Test
  @DisplayName("예정된 콘티 목록 조회")
  void getUpcomingContis() {
    // given
    LocalDate date = LocalDate.now();
    List<Conti> contiList = Collections.singletonList(testConti);

    when(contiRepository.findByScheduledAtGreaterThanEqualOrderByScheduledAtAsc(date))
        .thenReturn(contiList);

    // when
    List<Conti> result = contiService.getUpcomingContis(date);

    // then
    assertThat(result).hasSize(1);
    verify(contiRepository).findByScheduledAtGreaterThanEqualOrderByScheduledAtAsc(date);
  }

  @Test
  @DisplayName("과거 콘티 목록 조회")
  void getPastContis() {
    // given
    LocalDate date = LocalDate.now();
    List<Conti> contiList = Collections.singletonList(testConti);

    when(contiRepository.findByScheduledAtLessThanEqualOrderByScheduledAtDesc(date))
        .thenReturn(contiList);

    // when
    List<Conti> result = contiService.getPastContis(date);

    // then
    assertThat(result).hasSize(1);
    verify(contiRepository).findByScheduledAtLessThanEqualOrderByScheduledAtDesc(date);
  }

  @Test
  @DisplayName("상태별 콘티 목록 조회")
  void getContisByStatus() {
    // given
    ContiStatus status = ContiStatus.DRAFT;
    List<Conti> contiList = Collections.singletonList(testConti);

    when(contiRepository.findByStatus(status)).thenReturn(contiList);

    // when
    List<Conti> result = contiService.getContisByStatus(status);

    // then
    assertThat(result).hasSize(1);
    verify(contiRepository).findByStatus(status);
  }

  @Test
  @DisplayName("사용자별 상태별 콘티 목록 조회")
  void getUserContisByStatus() {
    // given
    ContiStatus status = ContiStatus.DRAFT;
    List<Conti> contiList = Collections.singletonList(testConti);

    when(contiRepository.findByCreatorAndStatus(testUser, status)).thenReturn(contiList);

    // when
    List<Conti> result = contiService.getUserContisByStatus(testUser, status);

    // then
    assertThat(result).hasSize(1);
    verify(contiRepository).findByCreatorAndStatus(testUser, status);
  }

  @Test
  @DisplayName("콘티 상태 업데이트")
  void updateContiStatus() {
    // given
    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));
    when(contiRepository.save(any(Conti.class))).thenReturn(testConti);

    // when
    contiService.updateContiStatus(1L, ContiStatus.FINALIZED, testUser);

    // then
    verify(contiRepository).save(any(Conti.class));
  }

  @Test
  @DisplayName("콘티 삭제")
  void deleteConti() {
    // given
    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));

    // when
    contiService.deleteConti(1L,  testUser);

    // then
    verify(contiRepository).delete(testConti);
  }

  @Test
  @DisplayName("사용자별 콘티 검색 - 권한 확인")
  void getContiByIdAndCreator() {
    // given
    when(contiRepository.findById(1L)).thenReturn(Optional.of(testConti));

    // when
    Conti result = contiService.getContiByIdAndCreator(1L, testUser);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("사용자별 콘티 검색 - 다른 사용자의 콘티 접근 실패")
  void getContiByIdAndCreatorNotAuthorized() {
    // given
    Long contiId = 1L;
    User anotherUser = User.builder()
        .id(2L)
        .email("another@example.com")
        .name("다른 사용자")
        .build();

    Conti conti = Conti.builder()
        .id(1L)
        .title("테스트 콘티")
        .creator(anotherUser)
        .build();

    when(contiRepository.findById(contiId)).thenReturn(Optional.of(conti));

    // when & then
    assertThatThrownBy(() -> contiService.getContiByIdAndCreator(contiId, testUser))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("해당 사용자의 콘티를 찾을 수 없습니다");
  }
}
