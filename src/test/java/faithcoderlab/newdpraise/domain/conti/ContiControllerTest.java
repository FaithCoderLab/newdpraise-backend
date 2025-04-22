package faithcoderlab.newdpraise.domain.conti;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import faithcoderlab.newdpraise.config.TestConfig;
import faithcoderlab.newdpraise.config.TestSecurityConfig;
import faithcoderlab.newdpraise.domain.conti.dto.ContiCreateRequest;
import faithcoderlab.newdpraise.domain.conti.dto.ContiParseRequest;
import faithcoderlab.newdpraise.domain.song.Song;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestSecurityConfig.class})
class ContiControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ContiService contiService;

  @MockBean
  private ContiParserService contiParserService;

  @MockBean
  private UserRepository userRepository;

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
        .songs(new ArrayList<>())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("콘티 텍스트 파싱 - 성공")
  @WithMockUser(username = "suming@example.com")
  void parseContiTextSuccess() throws Exception {
    // given
    ContiParseRequest request = new ContiParseRequest("20250405 찬양집회 콘티\n1. 물댄 동산 G");
    List<Song> songs = Collections.singletonList(
        Song.builder()
            .title("물댄 동산")
            .performanceKey("G")
            .build()
    );

    Conti parsedConti = Conti.builder()
        .title("20250405 찬양집회 콘티")
        .scheduledAt(LocalDate.of(2025, 4, 5))
        .songs(songs)
        .version("1.0")
        .status(ContiStatus.DRAFT)
        .build();

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
    when(contiParserService.parseContiText(anyString(), any(User.class))).thenReturn(parsedConti);

    // when & then
    mockMvc.perform(post("/conti/parse")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("20250405 찬양집회 콘티"))
        .andExpect(jsonPath("$.songs[0].title").value("물댄 동산"))
        .andExpect(jsonPath("$.songs[0].performanceKey").value("G"));
  }

  @Test
  @DisplayName("콘티 생성 - 성공")
  @WithMockUser(username = "suming@example.com")
  void createContiSuccess() throws Exception {
    // given
    ContiCreateRequest request = ContiCreateRequest.builder()
        .title("새 콘티")
        .description("테스트 콘티 설명")
        .scheduledAt(LocalDate.now())
        .songs(Collections.singletonList(
            ContiCreateRequest.SongDto.builder()
                .title("테스트 곡")
                .originalKey("C")
                .performanceKey("D")
                .artist("테스트 아티스트")
                .youtubeUrl("https://youtube.com/watch?v=test123")
                .build()
        ))
        .build();

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
    when(contiService.createConti(any(ContiCreateRequest.class), any(User.class)))
        .thenReturn(testConti);

    // when & then
    mockMvc.perform(post("/conti")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.title").value("테스트 콘티"));
  }

  @Test
  @DisplayName("콘티 목록 조회 - 성공")
  @WithMockUser(username = "suming@example.com")
  void getContiListSuccess() throws Exception {
    // given
    List<Conti> contiList = Collections.singletonList(testConti);

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
    when(contiService.getUserContiList(any(User.class))).thenReturn(contiList);

    // when & then
    mockMvc.perform(get("/conti"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].title").value("테스트 콘티"));
  }

  @Test
  @DisplayName("콘티 상세 조회 - 성공")
  @WithMockUser(username = "suming@example.com")
  void getContiDetailSuccess() throws Exception {
    // given
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
    when(contiService.getContiById(anyLong())).thenReturn(testConti);

    // when & then
    mockMvc.perform(get("/conti/{contiId}", 1L))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.title").value("테스트 콘티"));
  }

  @Test
  @DisplayName("인증되지 않은 사용자 요청 - 실패")
  void unauthorizedRequest() throws Exception {
    // given
    ContiParseRequest request = new ContiParseRequest("테스트 콘티");

    // when & then
    mockMvc.perform(post("/conti/parse")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("인증되지 않은 사용자입니다."));
  }
}
