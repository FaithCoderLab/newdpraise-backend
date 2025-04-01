package faithcoderlab.newdpraise.domain.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import faithcoderlab.newdpraise.config.TestConfig;
import faithcoderlab.newdpraise.domain.user.dto.SignUpRequest;
import faithcoderlab.newdpraise.domain.user.dto.SignUpResponse;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
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
    SignUpRequest request = SignUpRequest.builder()
        .email("suming@example.com")
        .password("Password123!")
        .name("수밍")
        .instrument("피아노")
        .build();

    SignUpResponse response = SignUpResponse.builder()
        .id(1L)
        .email("suming@example.com")
        .name("수밍")
        .instrument("피아노")
        .role(Role.USER)
        .createdAt(LocalDateTime.now())
        .build();

    when(userService.signup(any(SignUpRequest.class))).thenReturn(response);

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

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidSignUpRequestProvider")
  @DisplayName("회원가입 실패 - 유효성 검증")
  void SignupRailWithInvalidRequest(String testCase, SignUpRequest request, String fieldName, String expectedMessage) throws Exception {
    // when & then
    mockMvc.perform(post("/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("유효성 검증에 실패했습니다."))
        .andExpect(jsonPath("$.errors." + fieldName).value(expectedMessage));
  }

  private static Stream<Arguments> invalidSignUpRequestProvider() {
    return Stream.of(
        Arguments.of(
            "유효하지 않은 이메일",
            SignUpRequest.builder()
                .email("invalid-email")
                .password("Password123!")
                .name("수밍")
                .instrument("드럼")
                .build(),
            "email",
            "유효한 이메일 형식이 아닙니다."
        ),
        Arguments.of(
            "짧은 비밀번호",
            SignUpRequest.builder()
                .email("esss@example.com")
                .password("pw123!")
                .name("수밍")
                .instrument("기타")
                .build(),
            "password",
            "비밀번호는 최소 8자 이상이어야 합니다."
        ),
        Arguments.of(
            "비밀번호 조합 부족",
            SignUpRequest.builder()
                .email("test@example.com")
                .password("password123")
                .name("밍밍")
                .instrument("베이스")
                .build(),
            "password",
            "비밀번호는 숫자, 영문자, 특수문자를 포함해야 합니다."
        ),
        Arguments.of(
            "이름 누락",
            SignUpRequest.builder()
                .email("suming@example.com")
                .password("Password123!")
                .name("")
                .instrument("어쿠스틱 기타")
                .build(),
            "name",
            "이름은 필수 입력 항목입니다."
        ),
        Arguments.of(
            "이메일 누락",
            SignUpRequest.builder()
                .email("")
                .password("Password123!")
                .name("지현")
                .instrument("건반")
                .build(),
            "email",
            "이메일은 필수 입력 항목입니다."
        )
    );
  }
}