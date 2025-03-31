package faithcoderlab.newdpraise.domain.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import faithcoderlab.newdpraise.config.TestConfig;
import faithcoderlab.newdpraise.domain.user.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(faithcoderlab.newdpraise.config.TestConfig.class)
@ActiveProfiles("test")
@Transactional
public class UserApiTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("회원가입 API 통합 테스트 - 성공")
  void signupApiSuccess() throws Exception {
    // given
    SignupRequest request = SignupRequest.builder()
        .email("suming@example.com")
        .password("Password123!")
        .name("수밍")
        .instrument("피아노")
        .build();

    // when & then
    mockMvc.perform(post("/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.email").value("suming@example.com"))
        .andExpect(jsonPath("$.name").value("수밍"))
        .andExpect(jsonPath("$.instrument").value("피아노"))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  @DisplayName("회원가입 API 통합 테스트 - 중복 이메일 실패")
  void signupApiDuplicateEmailFail() throws Exception {
    // given
    SignupRequest request = SignupRequest.builder()
        .email("duplicate@example.com")
        .password("Password123!")
        .name("테스트유저")
        .instrument("베이스")
        .build();

    // 첫번째 요청은 성공
    mockMvc.perform(post("/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // when & then
    // 두번째 요청은 실패
    mockMvc.perform(post("/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
  }
}
