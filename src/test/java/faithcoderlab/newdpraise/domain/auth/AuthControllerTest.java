package faithcoderlab.newdpraise.domain.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import faithcoderlab.newdpraise.config.TestConfig;
import faithcoderlab.newdpraise.config.TestSecurityConfig;
import faithcoderlab.newdpraise.domain.auth.dto.LoginRequest;
import faithcoderlab.newdpraise.domain.auth.dto.LoginResponse;
import faithcoderlab.newdpraise.domain.auth.dto.TokenRefreshRequest;
import faithcoderlab.newdpraise.domain.auth.dto.TokenRefreshResponse;
import faithcoderlab.newdpraise.domain.user.Role;
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
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private AuthService authService;

  @Test
  @DisplayName("로그인 API 테스트 - 성공")
  void loginApiSuccess() throws Exception {
    // given
    LoginRequest request = LoginRequest.builder()
        .email("suming@example.com")
        .password("Password123!")
        .build();

    LoginResponse response = LoginResponse.builder()
        .id(1L)
        .email("suming@example.com")
        .name("수밍")
        .instrument("피아노")
        .role(Role.USER)
        .accessToken("test-access-token")
        .refreshToken("test-refresh-token")
        .build();

    when(authService.login(any(LoginRequest.class))).thenReturn(response);

    // when & then
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("suming@example.com"))
        .andExpect(jsonPath("$.name").value("수밍"))
        .andExpect(jsonPath("$.instrument").value("피아노"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.accessToken").value("test-access-token"))
        .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"));
  }

  @Test
  @DisplayName("로그인 API 테스트 - 유효성 검사 실패")
  void loginApiValidationFail() throws Exception {
    // given
    LoginRequest request = LoginRequest.builder()
        .email("invalid-email")
        .password("")
        .build();

    // when & then
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("토큰 재발급 API 테스트 - 성공")
  void refreshTokenApiSuccess() throws Exception {
    // given
    TokenRefreshRequest request = new TokenRefreshRequest("valid-refresh-token");

    TokenRefreshResponse response = TokenRefreshResponse.builder()
        .accessToken("new-access-token")
        .refreshToken("new-refresh-token")
        .build();

    when(authService.refreshToken(any(TokenRefreshRequest.class))).thenReturn(response);

    // when & then
    mockMvc.perform(post("/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("new-access-token"))
        .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
  }

  @Test
  @DisplayName("로그아웃 API 테스트 - 성공")
  @WithMockUser(username = "suming@example.com")
  void logoutApiSuccess() throws Exception {
    // given
    doNothing().when(authService).logout(anyString());

    // when & then
    mockMvc.perform(post("/auth/logout"))
        .andDo(print())
        .andExpect(status().isOk());
  }
}
