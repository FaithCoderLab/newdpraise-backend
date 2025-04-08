package faithcoderlab.newdpraise.domain.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import faithcoderlab.newdpraise.config.TestConfig;
import faithcoderlab.newdpraise.config.TestSecurityConfig;
import faithcoderlab.newdpraise.domain.user.dto.UpdateProfileRequest;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestSecurityConfig.class})
@Transactional
public class UserProfileApiTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = createAndSaveTestUser();
  }

  @Test
  @DisplayName("현재 사용자 프로필 조회 통합 테스트")
  @WithMockUser(username = "suming@example.com")
  void getCurrentUserProfileIntegrationTest() throws Exception {
    // when & then
    mockMvc.perform(get("/users/me"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testUser.getId()))
        .andExpect(jsonPath("$.email").value("suming@example.com"))
        .andExpect(jsonPath("$.name").value("수밍"))
        .andExpect(jsonPath("$.instrument").value("피아노"));
  }

  @Test
  @DisplayName("다른 사용자 프로필 조회 통합 테스트")
  @WithMockUser(username = "other@example.com")
  void getUserProfileIntegrationTest() throws Exception {
    // when & then
    mockMvc.perform(get("/users/{userId}", testUser.getId()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testUser.getId()))
        .andExpect(jsonPath("$.email").value("suming@example.com"))
        .andExpect(jsonPath("$.name").value("수밍"));
  }

  @Test
  @DisplayName("프로필 업데이트 통합 테스트")
  @WithMockUser(username = "suming@example.com")
  void updateProfileIntegrationTest() throws Exception {
    // given
    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setName("수민");
    request.setInstrument("aux keys");

    // when & then
    mockMvc.perform(put("/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("수민"))
        .andExpect(jsonPath("$.instrument").value("aux keys"));
  }

  private User createAndSaveTestUser() {
    User user = User.builder()
        .email("suming@example.com")
        .password(passwordEncoder.encode("Password123!"))
        .name("수밍")
        .instrument("피아노")
        .role(Role.USER)
        .enabled(true)
        .build();
    return userRepository.save(user);
  }
}
