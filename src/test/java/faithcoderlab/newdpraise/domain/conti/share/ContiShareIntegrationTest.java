package faithcoderlab.newdpraise.domain.conti.share;

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
import faithcoderlab.newdpraise.domain.conti.Conti;
import faithcoderlab.newdpraise.domain.conti.ContiRepository;
import faithcoderlab.newdpraise.domain.conti.ContiStatus;
import faithcoderlab.newdpraise.domain.conti.share.dto.ContiShareRequest;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestSecurityConfig.class})
public class ContiShareIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ContiRepository contiRepository;

  @Autowired
  private ContiShareRepository contiShareRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User creator;
  private User sharedUser;
  private Conti testConti;

  @BeforeEach
  @Transactional
  void setUp() {
    userRepository.deleteAll();
    contiRepository.deleteAll();
    contiShareRepository.deleteAll();

    creator = User.builder()
        .email("creator@example.com")
        .password(passwordEncoder.encode("Password123!"))
        .name("Creator")
        .role(Role.USER)
        .isActive(true)
        .build();
    creator = userRepository.save(creator);

    sharedUser = User.builder()
        .email("shared@example.com")
        .password(passwordEncoder.encode("Password123!"))
        .name("Shared User")
        .role(Role.USER)
        .isActive(true)
        .build();
    sharedUser = userRepository.save(sharedUser);

    testConti = Conti.builder()
        .title("통합 테스트 콘티")
        .description("콘티 공유 통합 테스트")
        .scheduledAt(LocalDate.now())
        .creator(creator)
        .status(ContiStatus.DRAFT)
        .version("1.0")
        .build();
    testConti = contiRepository.save(testConti);
  }

  @AfterEach
  @Transactional
  void tearDown() {
    contiShareRepository.deleteAll();
    contiRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 및 수락 통합 테스트")
  @WithMockUser(username = "creator@example.com")
  @Transactional
  void shareContiAndAcceptIntegrationTest() throws Exception {
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(testConti.getId());
    request.setUserEmail(sharedUser.getEmail());
    request.setPermission(ContiSharePermission.EDIT);

    MvcResult createResult = mockMvc.perform(post("/conti/shares")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.contiId").value(testConti.getId()))
        .andExpect(jsonPath("$.userEmail").value(sharedUser.getEmail()))
        .andExpect(jsonPath("$.permission").value("EDIT"))
        .andExpect(jsonPath("$.accepted").value(false))
        .andReturn();

    String createResponseJson = createResult.getResponse().getContentAsString();
    Long shareId = objectMapper.readTree(createResponseJson).path("id").asLong();

    Optional<ContiShare> savedShare = contiShareRepository.findById(shareId);
    assertThat(savedShare).isPresent();
    assertThat(savedShare.get().getConti().getId()).isEqualTo(testConti.getId());
    assertThat(savedShare.get().getUser().getId()).isEqualTo(sharedUser.getId());
    assertThat(savedShare.get().getPermission()).isEqualTo(ContiSharePermission.EDIT);
    assertThat(savedShare.get().isAccepted()).isFalse();

    mockMvc.perform(post("/conti/shares/{shareId}/accept", shareId)
            .with(SecurityMockMvcRequestPostProcessors.user("shared@example.com")))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(shareId))
        .andExpect(jsonPath("$.accepted").value(true))
        .andExpect(jsonPath("$.acceptedAt").exists());

    Optional<ContiShare> acceptedShare = contiShareRepository.findById(shareId);
    assertThat(acceptedShare).isPresent();
    assertThat(acceptedShare.get().isAccepted()).isTrue();
    assertThat(acceptedShare.get().getAcceptedAt()).isNotNull();
  }

  @Test
  @DisplayName("공유된 콘티 목록 조회 통합 테스트")
  @WithMockUser(username = "shared@example.com")
  @Transactional
  void getSharedContisIntegrationTest() throws Exception {
    ContiShare share = ContiShare.builder()
        .conti(testConti)
        .user(sharedUser)
        .sharedBy(creator)
        .permission(ContiSharePermission.VIEW)
        .accepted(true)
        .invitedAt(LocalDate.now().atStartOfDay())
        .acceptedAt(LocalDate.now().atStartOfDay())
        .build();
    contiShareRepository.save(share);

    mockMvc.perform(get("/conti/shares/shared-with-me"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].contiId").value(testConti.getId()))
        .andExpect(jsonPath("$[0].contiTitle").value(testConti.getTitle()))
        .andExpect(jsonPath("$[0].accepted").value(true));
  }

  @Test
  @DisplayName("공유 요청 목록 조회 통합 테스트")
  @WithMockUser(username = "shared@example.com")
  @Transactional
  void getPendingSharesIntegrationTest() throws Exception {
    ContiShare share = ContiShare.builder()
        .conti(testConti)
        .user(sharedUser)
        .sharedBy(creator)
        .permission(ContiSharePermission.VIEW)
        .accepted(false)
        .invitedAt(LocalDate.now().atStartOfDay())
        .build();
    contiShareRepository.save(share);

    mockMvc.perform(get("/conti/shares/pending"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].contiId").value(testConti.getId()))
        .andExpect(jsonPath("$[0].contiTitle").value(testConti.getTitle()))
        .andExpect(jsonPath("$[0].accepted").value(false));
  }

  @Test
  @DisplayName("공유 권한 수정 통합 테스트")
  @WithMockUser(username = "creator@example.com")
  @Transactional
  void updateSharePermissionIntegrationTest() throws Exception {
    ContiShare share = ContiShare.builder()
        .conti(testConti)
        .user(sharedUser)
        .sharedBy(creator)
        .permission(ContiSharePermission.VIEW)
        .accepted(true)
        .invitedAt(LocalDate.now().atStartOfDay())
        .acceptedAt(LocalDate.now().atStartOfDay())
        .build();
    ContiShare savedShare = contiShareRepository.save(share);

    mockMvc.perform(put("/conti/shares/{shareId}/permission", savedShare.getId())
            .param("permission", "ADMIN"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(savedShare.getId()))
        .andExpect(jsonPath("$.permission").value("ADMIN"));

    Optional<ContiShare> updatedShare = contiShareRepository.findById(savedShare.getId());
    assertThat(updatedShare).isPresent();
    assertThat(updatedShare.get().getPermission()).isEqualTo(ContiSharePermission.ADMIN);
  }

  @Test
  @DisplayName("공유 삭제 통합 테스트")
  @WithMockUser(username = "creator@example.com")
  @Transactional
  void deleteShareIntegrationTest() throws Exception {
    ContiShare share = ContiShare.builder()
        .conti(testConti)
        .user(sharedUser)
        .sharedBy(creator)
        .permission(ContiSharePermission.VIEW)
        .accepted(true)
        .invitedAt(LocalDate.now().atStartOfDay())
        .acceptedAt(LocalDate.now().atStartOfDay())
        .build();
    ContiShare savedShare = contiShareRepository.save(share);

    mockMvc.perform(delete("/conti/shares/{shareId}", savedShare.getId()))
        .andDo(print())
        .andExpect(status().isNoContent());

    Optional<ContiShare> deletedShare = contiShareRepository.findById(savedShare.getId());
    assertThat(deletedShare).isEmpty();
  }

  @Test
  @DisplayName("콘티 권한에 따른 공유 목록 조회 통합 테스트")
  @WithMockUser(username = "creator@example.com")
  @Transactional
  void getContiSharesIntegrationTest() throws Exception {
    ContiShare share1 = ContiShare.builder()
        .conti(testConti)
        .user(sharedUser)
        .sharedBy(creator)
        .permission(ContiSharePermission.VIEW)
        .accepted(true)
        .invitedAt(LocalDate.now().atStartOfDay())
        .acceptedAt(LocalDate.now().atStartOfDay())
        .build();
    contiShareRepository.save(share1);

    User anotherUser = User.builder()
        .email("another@example.com")
        .password(passwordEncoder.encode("Password123!"))
        .name("Another User")
        .role(Role.USER)
        .isActive(true)
        .build();
    anotherUser = userRepository.save(anotherUser);

    ContiShare share2 = ContiShare.builder()
        .conti(testConti)
        .user(anotherUser)
        .sharedBy(creator)
        .permission(ContiSharePermission.EDIT)
        .accepted(false)
        .invitedAt(LocalDate.now().atStartOfDay())
        .build();
    contiShareRepository.save(share2);

    mockMvc.perform(get("/conti/shares/conti/{contiId}", testConti.getId()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].contiId").value(testConti.getId()))
        .andExpect(jsonPath("$[1].contiId").value(testConti.getId()))
        .andExpect(jsonPath("$[?(@.userEmail == \"shared@example.com\")].permission").value("VIEW"))
        .andExpect(
            jsonPath("$[?(@.userEmail == \"another@example.com\")].permission").value("EDIT"));
  }

  @Test
  @DisplayName("공유된 콘티 접근 권한 통합 테스트")
  @Transactional
  void contiAccessPermissionIntegrationTest() throws Exception {
    ContiShare viewShare = ContiShare.builder()
        .conti(testConti)
        .user(sharedUser)
        .sharedBy(creator)
        .permission(ContiSharePermission.VIEW)
        .accepted(true)
        .invitedAt(LocalDate.now().atStartOfDay())
        .acceptedAt(LocalDate.now().atStartOfDay())
        .build();
    contiShareRepository.save(viewShare);

    mockMvc.perform(get("/conti/{contiId}", testConti.getId())
            .with(SecurityMockMvcRequestPostProcessors.user("shared@example.com")))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testConti.getId()))
        .andExpect(jsonPath("$.title").value(testConti.getTitle()))
        .andExpect(jsonPath("$.canEdit").value(false))
        .andExpect(jsonPath("$.canShare").value(false))
        .andExpect(jsonPath("$.permissionType").value("VIEW"));

    String updateJson = "{\"title\":\"Updated Title\"}";
    mockMvc.perform(put("/conti/{contiId}", testConti.getId())
            .with(SecurityMockMvcRequestPostProcessors.user("shared@example.com"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(updateJson))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("콘티를 수정할 권한이 없습니다."));
  }

  @Test
  @DisplayName("편집 권한 공유 통합 테스트")
  @Transactional
  void editPermissionIntegrationTest() throws Exception {
    ContiShare editShare = ContiShare.builder()
        .conti(testConti)
        .user(sharedUser)
        .sharedBy(creator)
        .permission(ContiSharePermission.EDIT)
        .accepted(true)
        .invitedAt(LocalDate.now().atStartOfDay())
        .acceptedAt(LocalDate.now().atStartOfDay())
        .build();
    contiShareRepository.save(editShare);

    mockMvc.perform(get("/conti/{contiId}", testConti.getId())
            .with(SecurityMockMvcRequestPostProcessors.user("shared@example.com")))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testConti.getId()))
        .andExpect(jsonPath("$.canEdit").value(true))
        .andExpect(jsonPath("$.canShare").value(false))
        .andExpect(jsonPath("$.permissionType").value("EDIT"));

    String updateJson = "{\"title\":\"Updated Title\"}";
    mockMvc.perform(put("/conti/{contiId}", testConti.getId())
            .with(SecurityMockMvcRequestPostProcessors.user("shared@example.com"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(updateJson))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Title"));

    Optional<Conti> updatedConti = contiRepository.findById(testConti.getId());
    assertThat(updatedConti).isPresent();
    assertThat(updatedConti.get().getTitle()).isEqualTo("Updated Title");
  }
}
