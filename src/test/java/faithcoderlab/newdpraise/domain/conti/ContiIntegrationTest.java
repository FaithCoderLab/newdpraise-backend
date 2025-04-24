package faithcoderlab.newdpraise.domain.conti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import faithcoderlab.newdpraise.config.TestConfig;
import faithcoderlab.newdpraise.config.TestSecurityConfig;
import faithcoderlab.newdpraise.domain.conti.dto.ContiCreateRequest;
import faithcoderlab.newdpraise.domain.conti.dto.ContiCreateRequest.SongDto;
import faithcoderlab.newdpraise.domain.conti.dto.ContiUpdateRequest;
import faithcoderlab.newdpraise.domain.song.Song;
import faithcoderlab.newdpraise.domain.song.SongRepository;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestSecurityConfig.class})
public class ContiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ContiRepository contiRepository;

  @Autowired
  private SongRepository songRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User testUser;
  private Conti testConti;

  @Transactional
  @BeforeEach
  void setUp() {
    songRepository.deleteAll();
    contiRepository.deleteAll();
    userRepository.deleteAll();

    testUser = User.builder()
        .email("suming@example.com")
        .password(passwordEncoder.encode("Password123!"))
        .name("수밍")
        .instrument("피아노")
        .role(Role.USER)
        .isActive(true)
        .build();
    testUser = userRepository.save(testUser);

    testConti = Conti.builder()
        .title("테스트 콘티")
        .description("통합 테스트용 콘티")
        .scheduledAt(LocalDate.now())
        .creator(testUser)
        .songs(new ArrayList<>())
        .version("1.0")
        .status(ContiStatus.DRAFT)
        .build();
    testConti = contiRepository.save(testConti);

    Song song = Song.builder()
        .title("테스트 곡")
        .originalKey("C")
        .performanceKey("D")
        .artist("테스트 아티스트")
        .build();
    Song savedSong = songRepository.save(song);

    testConti.getSongs().add(savedSong);
    testConti = contiRepository.save(testConti);
  }

  @AfterEach
  void tearDown() {
    contiRepository.deleteAll();
    songRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @Transactional
  @DisplayName("콘티 생성 및 조회 통합 테스트")
  @WithMockUser(username = "suming@example.com")
  void createAndRetrieveContiTest() throws Exception {
    ContiCreateRequest createRequest = ContiCreateRequest.builder()
        .title("새 콘티")
        .description("통합 테스트로 생성된 콘티")
        .scheduledAt(LocalDate.now().plusDays(7))
        .songs(Collections.singletonList(
            SongDto.builder()
                .title("새 곡")
                .originalKey("E")
                .performanceKey("F")
                .artist("통합 테스트")
                .youtubeUrl("https://youtube.com/watch?v=integration-test")
                .build()
        ))
        .build();

    MvcResult createResult = mockMvc.perform(post("/conti")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("새 콘티"))
        .andExpect(jsonPath("$.description").value("통합 테스트로 생성된 콘티"))
        .andExpect(jsonPath("$.songs[0].title").value("새 곡"))
        .andReturn();

    String createResponseJson = createResult.getResponse().getContentAsString();
    Long createdContiId = objectMapper.readTree(createResponseJson).path("id").asLong();

    mockMvc.perform(get("/conti/{contiId}", createdContiId))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(createdContiId))
        .andExpect(jsonPath("$.title").value("새 콘티"))
        .andExpect(jsonPath("$.status").value("DRAFT"));

    Conti savedConti = contiRepository.findById(createdContiId).orElseThrow();
    assertThat(savedConti.getTitle()).isEqualTo("새 콘티");
    assertThat(savedConti.getSongs()).hasSize(1);
    assertThat(savedConti.getSongs().get(0).getTitle()).isEqualTo("새 곡");
  }

  @Test
  @Transactional
  @DisplayName("콘티 목록 조회, 수정, 삭제 통합 테스트")
  @WithMockUser(username = "suming@example.com")
  void listUpdateDeleteCOntiTest() throws Exception {
    mockMvc.perform(get("/conti"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].id").value(testConti.getId()))
        .andExpect(jsonPath("$[0].title").value("테스트 콘티"));

    ContiUpdateRequest updateRequest = ContiUpdateRequest.builder()
        .title("수정된 콘티")
        .description("수정된 설명")
        .status(ContiStatus.FINALIZED)
        .build();

    mockMvc.perform(put("/conti/{contiId}", testConti.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("수정된 콘티"))
        .andExpect(jsonPath("$.description").value("수정된 설명"))
        .andExpect(jsonPath("$.status").value("FINALIZED"));

    Conti updatedConti = contiRepository.findById(testConti.getId()).orElseThrow();
    assertThat(updatedConti.getTitle()).isEqualTo("수정된 콘티");
    assertThat(updatedConti.getStatus()).isEqualTo(ContiStatus.FINALIZED);

    mockMvc.perform(delete("/conti/{contiId}", testConti.getId()))
        .andDo(print())
        .andExpect(status().isNoContent());

    assertThat(contiRepository.findById(testConti.getId())).isEmpty();
  }

  @Test
  @DisplayName("날짜 및 키워드로 콘티 검색 통합 테스트")
  @WithMockUser(username = "suming@example.com")
  void searchContiTest() throws Exception {
    Conti anotherConti = Conti.builder()
        .title("다른 콘티")
        .description("검색 테스트용")
        .scheduledAt(LocalDate.now().plusMonths(1))
        .creator(testUser)
        .status(ContiStatus.DRAFT)
        .version("1.0")
        .build();
    contiRepository.save(anotherConti);

    mockMvc.perform(get("/conti")
            .param("title", "테스트"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].title").value("테스트 콘티"))
        .andExpect(jsonPath("$[?(@.title == '다른 콘티')]").doesNotExist());

    LocalDate startDate = LocalDate.now().minusDays(1);
    LocalDate endDate = LocalDate.now().plusDays(1);

    mockMvc.perform(get("/conti")
            .param("startDate", startDate.toString())
            .param("endDate", endDate.toString()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].title").value("테스트 콘티"))
        .andExpect(jsonPath("$[?(@.title == '다른 콘티')]").doesNotExist());

    mockMvc.perform(get("/conti/status/{status}", ContiStatus.DRAFT))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.title == '테스트 콘티')]").exists())
        .andExpect(jsonPath("$[?(@.title == '다른 콘티')]").exists());

    mockMvc.perform(get("/conti/paged")
            .param("page", "0")
            .param("size", "10")
            .param("sort", "title"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  @Transactional
  @DisplayName("예정된 콘티/과거 콘티 조회 통합 테스트")
  @WithMockUser(username = "suming@example.com")
  void upcomingPastContiTest() throws Exception {
    Conti pastConti = Conti.builder()
        .title("과거 콘티")
        .description("지난주 콘티")
        .scheduledAt(LocalDate.now().minusWeeks(1))
        .creator(testUser)
        .status(ContiStatus.ARCHIVED)
        .version("1.0")
        .build();
    contiRepository.save(pastConti);

    Conti futureConti = Conti.builder()
        .title("미래 콘티")
        .description("다음주 콘티")
        .scheduledAt(LocalDate.now().plusWeeks(1))
        .creator(testUser)
        .status(ContiStatus.DRAFT)
        .version("1.0")
        .build();
    contiRepository.save(futureConti);

    mockMvc.perform(get("/conti/upcoming"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.title == '테스트 콘티')]").exists())
        .andExpect(jsonPath("$[?(@.title == '미래 콘티')]").exists())
        .andExpect(jsonPath("$[?(@.title == '과거 콘티')]").doesNotExist());

    mockMvc.perform(get("/conti/past"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.title == '과거 콘티')]").exists())
        .andExpect(jsonPath("$[?(@.title == '미래 콘티')]").doesNotExist());

    mockMvc.perform(get("/conti/status/{status}", ContiStatus.ARCHIVED))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.title == '과거 콘티')]").exists())
        .andExpect(jsonPath("$[?(@.title == '테스트 콘티')]").doesNotExist())
        .andExpect(jsonPath("$[?(@.title == '미래 콘티')]").doesNotExist());
  }

  @Test
  @Transactional
  @DisplayName("콘티 상태 변경 통합 테스트")
  @WithMockUser(username = "suming@example.com")
  void updateContiStatusTest() throws Exception {
    mockMvc.perform(put("/conti/{contiId}/status", testConti.getId())
            .param("status", ContiStatus.FINALIZED.name()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testConti.getId()))
        .andExpect(jsonPath("$.status").value("FINALIZED"));

    Conti updatedConti = contiRepository.findById(testConti.getId()).orElseThrow();
    assertThat(updatedConti.getStatus()).isEqualTo(ContiStatus.FINALIZED);

    mockMvc.perform(put("/conti/{contiId}/status", updatedConti.getId())
            .param("status", ContiStatus.ARCHIVED.name()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"));

    updatedConti = contiRepository.findById(testConti.getId()).orElseThrow();
    assertThat(updatedConti.getStatus()).isEqualTo(ContiStatus.ARCHIVED);
  }

  @Test
  @DisplayName("다른 사용자의 콘티 접근 제한 테스트")
  @WithMockUser(username = "suming@example.com")
  void accessControlTest() throws Exception {
    User anotherUser = User.builder()
        .email("another@example.com")
        .password(passwordEncoder.encode("Password123!"))
        .name("다른 사용자")
        .role(Role.USER)
        .isActive(true)
        .build();
    userRepository.save(anotherUser);

    Conti anotherUserConti = Conti.builder()
        .title("다른 사용자의 콘티")
        .description("접근 제한 테스트용")
        .scheduledAt(LocalDate.now())
        .creator(anotherUser)
        .status(ContiStatus.DRAFT)
        .version("1.0")
        .build();
    contiRepository.save(anotherUserConti);

    ContiUpdateRequest updateRequest = ContiUpdateRequest.builder()
        .title("수정 시도")
        .build();

    mockMvc.perform(put("/conti/{contiId}", anotherUserConti.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andDo(print())
        .andExpect(status().isNotFound());

    mockMvc.perform(put("/conti/{contiId}/status", anotherUserConti.getId())
            .param("status", ContiStatus.FINALIZED.name()))
        .andDo(print())
        .andExpect(status().isNotFound());

    mockMvc.perform(delete("/conti/{contiId}", anotherUserConti.getId()))
        .andDo(print())
        .andExpect(status().isNotFound());

    assertThat(contiRepository.findById(anotherUserConti.getId())).isPresent();
  }
}
