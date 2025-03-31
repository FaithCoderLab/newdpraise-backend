package faithcoderlab.newdpraise.domain.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import faithcoderlab.newdpraise.domain.user.dto.SignupRequest;
import faithcoderlab.newdpraise.domain.user.dto.SignupResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private UserService userService;

  @Test
  @DisplayName("회원가입 성공 테스트")
  void signupSuccess() throws Exception {
    // given
    SignupRequest request = SignupRequest.builder()
        .email("suming@example.com")
        .password("Password123!")
        .name("수밍")
        .instrument("피아노")
        .build();

    SignupResponse response = SignupResponse.builder()
        .id(1L)
        .email("suming@example.com")
        .name("수밍")
        .instrument("피아노")
        .role(Role.USER)
        .createdAt(LocalDateTime.now())
        .build();

    when(userService.signup(any(SignupRequest.class))).thenReturn(response);

    // when & then
    mockMvc.perform(post("/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("suming@example.com"))
        .andExpect(jsonPath("$.name").value("수밍"))
        .andExpect(jsonPath("$.instrument").value("피아노"))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  @DisplayName("회원가입 실패 - 유효하지 않은 이메일")
  void SignupRailWithInvalidEmail() throws Exception {
    // given
    SignupRequest request = SignupRequest.builder()
        .email("invalid-email") // 이메일 형식 X
        .password("Password123!")
        .name("테스트유저")
        .instrument("드럼")
        .build();

    // when & then
    mockMvc.perform(post("/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("회원가입 실패 - 비밀번호 유효성 검사")
  void SignupRailWithInvalidPassword() throws Exception {
    // given
    SignupRequest request = SignupRequest.builder()
        .email("test@example.com")
        .password("short") // 8자 미만
        .name("테스트유저")
        .instrument("기타")
        .build();

    // when & then
    mockMvc.perform(post("/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("회원가입 실패 - 이름 누락")
  void signupFailWithMissingName() throws Exception {
    // given
    SignupRequest request = SignupRequest.builder()
        .email("suming@example.com")
        .password("Password123!")
        .name("") // 이름 누락
        .instrument("베이스")
        .build();

    // when & then
    mockMvc.perform(post("/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }
}