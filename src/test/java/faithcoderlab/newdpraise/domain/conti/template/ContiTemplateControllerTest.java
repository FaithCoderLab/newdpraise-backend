package faithcoderlab.newdpraise.domain.conti.template;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
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
import faithcoderlab.newdpraise.domain.conti.Conti;
import faithcoderlab.newdpraise.domain.conti.ContiStatus;
import faithcoderlab.newdpraise.domain.conti.template.dto.ContiTemplateApplyRequest;
import faithcoderlab.newdpraise.domain.conti.template.dto.ContiTemplateCreateRequest;
import faithcoderlab.newdpraise.domain.conti.template.dto.ContiTemplateUpdateRequest;
import faithcoderlab.newdpraise.domain.song.Song;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import faithcoderlab.newdpraise.global.exception.AuthenticationException;
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
class ContiTemplateControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ContiTemplateService contiTemplateService;

  @MockBean
  private UserRepository userRepository;

  private User testUser;
  private ContiTemplate testTemplate;
  private Conti testConti;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .email("suming@example.com")
        .name("수밍")
        .role(Role.USER)
        .build();

    Song testSong = Song.builder()
        .id(1L)
        .title("테스트 곡")
        .originalKey("C")
        .performanceKey("D")
        .artist("테스트 아티스트")
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

    when(userRepository.findByEmail("suming@example.com")).thenReturn(Optional.of(testUser));
  }

  @Test
  @DisplayName("템플릿 생성 - 성공")
  @WithMockUser(username = "suming@example.com")
  void createTemplate_Success() throws Exception {
    // given
    ContiTemplateCreateRequest request = ContiTemplateCreateRequest.builder()
        .name("새 템플릿")
        .description("테스트 용 템플릿")
        .contiId(1L)
        .isPublic(true)
        .build();

    when(
        contiTemplateService.createTemplate(any(ContiTemplateCreateRequest.class), any(User.class)))
        .thenReturn(testTemplate);

    // when & then
    mockMvc.perform(post("/conti/templates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("테스트 템플릿"))
        .andExpect(jsonPath("$.isPublic").value(true))
        .andExpect(jsonPath("$.usageCount").value(5));
  }

  @Test
  @DisplayName("템플릿 조회 - 성공")
  @WithMockUser(username = "suming@example.com")
  void getTemplate_Success() throws Exception {
    // given
    when(contiTemplateService.getAccessibleTemplate(anyLong(), any(User.class)))
        .thenReturn(testTemplate);

    // when & then
    mockMvc.perform(get("/conti/templates/{templateId}", 1L))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("테스트 템플릿"))
        .andExpect(jsonPath("$.isPublic").value(true))
        .andExpect(jsonPath("$.usageCount").value(5));
  }

  @Test
  @DisplayName("템플릿 조회 - 실패 (권한 없음)")
  @WithMockUser(username = "suming@example.com")
  void getTemplate_Unauthorized() throws Exception {
    // given
    when(contiTemplateService.getAccessibleTemplate(anyLong(), any(User.class)))
        .thenThrow(new AuthenticationException("해당 템플릿에 접근할 권한이 없습니다."));

    // when & then
    mockMvc.perform(get("/conti/templates/{templateId}", 1L))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("해당 템플릿에 접근할 권한이 없습니다."));
  }

  @Test
  @DisplayName("템플릿 수정 - 성공")
  @WithMockUser(username = "suming@example.com")
  void updateTemplate_Success() throws Exception {
    // given
    ContiTemplateUpdateRequest request = ContiTemplateUpdateRequest.builder()
        .name("수정된 템플릿")
        .description("수정된 설명")
        .isPublic(false)
        .build();

    when(contiTemplateService.updateTemplate(anyLong(), any(ContiTemplateUpdateRequest.class),
        any(User.class)))
        .thenReturn(testTemplate);

    // when & then
    mockMvc.perform(put("/conti/templates/{templateId}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("테스트 템플릿"));
  }

  @Test
  @DisplayName("템플릿 삭제 - 성공")
  @WithMockUser(username = "suming@example.com")
  void deleteTemplate_Success() throws Exception {
    // given
    doNothing().when(contiTemplateService).deleteTemplate(anyLong(), any(User.class));

    // when & then
    mockMvc.perform(delete("/conti/templates/{templateId}", 1L))
        .andDo(print())
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("내 템플릿 목록 조회 - 성공")
  @WithMockUser(username = "suming@example.com")
  void getMyTemplates_Success() throws Exception {
    // given
    List<ContiTemplate> templates = Collections.singletonList(testTemplate);
    when(contiTemplateService.getUserTemplates(any(User.class))).thenReturn(templates);

    // when & then
    mockMvc.perform(get("/conti/templates/my"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].name").value("테스트 템플릿"));
  }

  @Test
  @DisplayName("공개 템플릿 목록 조회 - 성공")
  @WithMockUser(username = "suming@example.com")
  void getPublicTemplates_Success() throws Exception {
    // given
    List<ContiTemplate> templates = Collections.singletonList(testTemplate);
    when(contiTemplateService.getPublicTemplates()).thenReturn(templates);

    // when & then
    mockMvc.perform(get("/conti/templates/public"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].name").value("테스트 템플릿"))
        .andExpect(jsonPath("$[0].isPublic").value(true));
  }

  @Test
  @DisplayName("접근 가능한 템플릿 목록 조회 - 성공")
  @WithMockUser(username = "suming@example.com")
  void getAccessibleTemplates_Success() throws Exception {
    // given
    List<ContiTemplate> templates = Collections.singletonList(testTemplate);
    when(contiTemplateService.getAccessibleTemplates(any(User.class))).thenReturn(templates);

    // when & then
    mockMvc.perform(get("/conti/templates/accessible"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].name").value("테스트 템플릿"));
  }

  @Test
  @DisplayName("인기 템플릿 목록 조회 - 성공")
  @WithMockUser(username = "suming@example.com")
  void getPopularTemplates_Success() throws Exception {
    // given
    List<ContiTemplate> templates = Collections.singletonList(testTemplate);
    when(contiTemplateService.getPopularTemplates(5)).thenReturn(templates);

    // when & then
    mockMvc.perform(get("/conti/templates/popular")
            .param("limit", "5"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].name").value("테스트 템플릿"))
        .andExpect(jsonPath("$[0].usageCount").value(5));
  }

  @Test
  @DisplayName("템플릿 적용 - 성공")
  @WithMockUser(username = "suming@example.com")
  void applyTemplate_Success() throws Exception {
    // given
    ContiTemplateApplyRequest request = ContiTemplateApplyRequest.builder()
        .templateId(1L)
        .customTitle("커스텀 제목")
        .customDescription("커스텀 설명")
        .build();

    when(contiTemplateService.applyTemplate(anyLong(), any(User.class))).thenReturn(testConti);

    // when & then
    mockMvc.perform(post("/conti/templates/apply")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.title").value("커스텀 제목"))
        .andExpect(jsonPath("$.status").value("DRAFT"));
  }

  @Test
  @DisplayName("인증되지 않은 사용자 - 실패")
  void unauthorized_Fail() throws Exception {
    // when & then
    mockMvc.perform(get("/conti/templates/my"))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }
}
