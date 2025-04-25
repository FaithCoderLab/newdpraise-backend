package faithcoderlab.newdpraise.domain.conti.share;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import faithcoderlab.newdpraise.domain.conti.share.dto.ContiShareRequest;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import faithcoderlab.newdpraise.global.exception.AuthenticationException;
import faithcoderlab.newdpraise.global.exception.ResourceAlreadyExistsException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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
class ContiShareControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ContiShareService contiShareService;

  @MockBean
  private UserRepository userRepository;

  private User testUser;
  private User targetUser;
  private Conti testConti;
  private ContiShare testShare;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .email("test@example.com")
        .name("Test User")
        .role(Role.USER)
        .build();

    targetUser = User.builder()
        .id(2L)
        .email("target@example.com")
        .name("Target User")
        .role(Role.USER)
        .build();

    testConti = Conti.builder()
        .id(1L)
        .title("Test Conti")
        .description("Test Description")
        .scheduledAt(LocalDate.now())
        .creator(testUser)
        .status(ContiStatus.DRAFT)
        .version("1.0")
        .createdAt(LocalDateTime.now())
        .build();

    testShare = ContiShare.builder()
        .id(1L)
        .conti(testConti)
        .user(targetUser)
        .sharedBy(testUser)
        .permission(ContiSharePermission.VIEW)
        .accepted(false)
        .invitedAt(LocalDateTime.now())
        .createdAt(LocalDateTime.now())
        .build();

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 성공")
  @WithMockUser(username = "test@example.com")
  void createShareSuccess() throws Exception {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(1L);
    request.setUserEmail("target@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    when(contiShareService.createShare(any(ContiShareRequest.class), any(User.class))).thenReturn(
        testShare);

    // when & then
    mockMvc.perform(post("/conti/shares")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.contiId").value(1L))
        .andExpect(jsonPath("$.contiTitle").value("Test Conti"))
        .andExpect(jsonPath("$.userEmail").value("target@example.com"))
        .andExpect(jsonPath("$.permission").value("VIEW"))
        .andExpect(jsonPath("$.accepted").value(false));
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 실패 (유효성 검증)")
  @WithMockUser(username = "test@example.com")
  void createShareValidationFail() throws Exception {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setUserEmail("target@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    // when & then
    mockMvc.perform(post("/conti/shares")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("유효성 검증에 실패했습니다."))
        .andExpect(jsonPath("$.errors.contiId").exists());
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 실패 (콘티 없음)")
  @WithMockUser(username = "test@example.com")
  void createShareFailNoConti() throws Exception {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(999L);
    request.setUserEmail("target@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    when(contiShareService.createShare(any(ContiShareRequest.class), any(User.class)))
        .thenThrow(new ResourceNotFoundException("콘티를 찾을 수 없습니다. ID: 999"));

    // when & then
    mockMvc.perform(post("/conti/shares")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("콘티를 찾을 수 없습니다. ID: 999"));
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 실패 (사용자 없음)")
  @WithMockUser(username = "test@example.com")
  void createShareFailNoUser() throws Exception {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(1L);
    request.setUserEmail("nonexistent@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    when(contiShareService.createShare(any(ContiShareRequest.class), any(User.class)))
        .thenThrow(new ResourceNotFoundException("사용자를 찾을 수 없습니다. Email: nonexistent@example.com"));

    // when & then
    mockMvc.perform(post("/conti/shares")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다. Email: nonexistent@example.com"));
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 실패 (이미 공유됨)")
  @WithMockUser(username = "test@example.com")
  void createShareFailAlreadyShared() throws Exception {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(1L);
    request.setUserEmail("target@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    when(contiShareService.createShare(any(ContiShareRequest.class), any(User.class)))
        .thenThrow(new ResourceAlreadyExistsException("이미 해당 사용자에게 공유된 콘티입니다."));

    // when & then
    mockMvc.perform(post("/conti/shares")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.message").value("이미 해당 사용자에게 공유된 콘티입니다."));
  }

  @Test
  @DisplayName("콘티 공유 초대 생성 - 실패 (권한 없음)")
  @WithMockUser(username = "test@example.com")
  void createShareFailNoPermission() throws Exception {
    // given
    ContiShareRequest request = new ContiShareRequest();
    request.setContiId(1L);
    request.setUserEmail("target@example.com");
    request.setPermission(ContiSharePermission.VIEW);

    when(contiShareService.createShare(any(ContiShareRequest.class), any(User.class)))
        .thenThrow(new AuthenticationException("콘티를 공유할 권한이 없습니다."));

    // when & then
    mockMvc.perform(post("/conti/shares")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("콘티를 공유할 권한이 없습니다."));
  }

  @Test
  @DisplayName("콘티 공유 초대 수락 - 성공")
  @WithMockUser(username = "test@example.com")
  void acceptShareSuccess() throws Exception {
    // given
    ContiShare acceptedShare = ContiShare.builder()
        .id(1L)
        .conti(testConti)
        .user(testUser)
        .sharedBy(targetUser)
        .permission(ContiSharePermission.VIEW)
        .accepted(true)
        .invitedAt(LocalDateTime.now())
        .acceptedAt(LocalDateTime.now())
        .build();

    when(contiShareService.acceptShare(anyLong(), any(User.class))).thenReturn(acceptedShare);

    // when & then
    mockMvc.perform(post("/conti/shares/{shareId}/accept", 1L))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.contiId").value(1L))
        .andExpect(jsonPath("$.accepted").value(true))
        .andExpect(jsonPath("$.acceptedAt").exists());
  }

  @Test
  @DisplayName("콘티 공유 초대 수락 - 실패 (공유 정보 없음)")
  @WithMockUser(username = "test@example.com")
  void acceptShareFailNoShare() throws Exception {
    // given
    when(contiShareService.acceptShare(anyLong(), any(User.class)))
        .thenThrow(new ResourceNotFoundException("공유 정보를 찾을 수 없습니다. ID: 999"));

    // when & then
    mockMvc.perform(post("/conti/shares/{shareId}/accept", 999L))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("공유 정보를 찾을 수 없습니다. ID: 999"));
  }

  @Test
  @DisplayName("콘티 공유 초대 수락 - 실패 (권한 없음)")
  @WithMockUser(username = "test@example.com")
  void acceptShareFailNoPermission() throws Exception {
    // given
    when(contiShareService.acceptShare(anyLong(), any(User.class)))
        .thenThrow(new AuthenticationException("해당 공유 요청에 대한 권한이 없습니다."));

    // when & then
    mockMvc.perform(post("/conti/shares/{shareId}/accept", 1L))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("해당 공유 요청에 대한 권한이 없습니다."));
  }

  @Test
  @DisplayName("콘티 공유 삭제 - 성공")
  @WithMockUser(username = "test@example.com")
  void deleteShareSuccess() throws Exception {
    // given
    doNothing().when(contiShareService).deleteShare(anyLong(), any(User.class));

    // when & then
    mockMvc.perform(delete("/conti/shares/{shareId}", 1L))
        .andDo(print())
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("콘티 공유 삭제 - 실패 (공유 정보 없음)")
  @WithMockUser(username = "test@example.com")
  void deleteShareFailNoShare() throws Exception {
    // given
    doThrow(new ResourceNotFoundException("공유 정보를 찾을 수 없습니다. ID: 999"))
        .when(contiShareService).deleteShare(anyLong(), any(User.class));

    // when & then
    mockMvc.perform(delete("/conti/shares/{shareId}", 999L))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("공유 정보를 찾을 수 없습니다. ID: 999"));
  }

  @Test
  @DisplayName("콘티 공유 권한 수정 - 성공")
  @WithMockUser(username = "test@example.com")
  void updatePermissionSuccess() throws Exception {
    // given
    ContiShare updatedShare = ContiShare.builder()
        .id(1L)
        .conti(testConti)
        .user(targetUser)
        .sharedBy(testUser)
        .permission(ContiSharePermission.EDIT)
        .accepted(false)
        .invitedAt(LocalDateTime.now())
        .build();

    when(contiShareService.updateSharePermission(anyLong(), any(ContiSharePermission.class),
        any(User.class))).thenReturn(updatedShare);

    // when & then
    mockMvc.perform(put("/conti/shares/{shareId}/permission", 1L)
            .param("permission", "EDIT"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.permission").value("EDIT"));
  }

  @Test
  @DisplayName("내게 공유된 콘티 목록 조회")
  @WithMockUser(username = "test@example.com")
  void getSharedWithMe() throws Exception {
    // given
    ContiShare acceptedShare = ContiShare.builder()
        .id(1L)
        .conti(testConti)
        .user(testUser)
        .sharedBy(targetUser)
        .permission(ContiSharePermission.VIEW)
        .accepted(true)
        .invitedAt(LocalDateTime.now())
        .acceptedAt(LocalDateTime.now())
        .build();

    when(contiShareService.getSharedContis(any(User.class))).thenReturn(
        Collections.singletonList(acceptedShare));

    // when & then
    mockMvc.perform(get("/conti/shares/shared-with-me"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].contiId").value(1L))
        .andExpect(jsonPath("$[0].contiTitle").value("Test Conti"))
        .andExpect(jsonPath("$[0].accepted").value(true));
  }

  @Test
  @DisplayName("내게 온 공유 요청 목록 조회")
  @WithMockUser(username = "test@example.com")
  void getPendingShares() throws Exception {
    // given
    ContiShare pendingShare = ContiShare.builder()
        .id(1L)
        .conti(testConti)
        .user(testUser)
        .sharedBy(targetUser)
        .permission(ContiSharePermission.VIEW)
        .accepted(false)
        .invitedAt(LocalDateTime.now())
        .build();

    when(contiShareService.getPendingShares(any(User.class))).thenReturn(
        Collections.singletonList(pendingShare));

    // when & then
    mockMvc.perform(get("/conti/shares/pending"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].contiId").value(1L))
        .andExpect(jsonPath("$[0].accepted").value(false));
  }

  @Test
  @DisplayName("콘티 공유 목록 조회")
  @WithMockUser(username = "test@example.com")
  void getContiShares() throws Exception {
    // given
    when(contiShareService.getContiShares(anyLong(), any(User.class))).thenReturn(
        Collections.singletonList(testShare));

    // when & then
    mockMvc.perform(get("/conti/shares/conti/{contiId}", 1L))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].userEmail").value("target@example.com"))
        .andExpect(jsonPath("$[0].permission").value("VIEW"));
  }
}
