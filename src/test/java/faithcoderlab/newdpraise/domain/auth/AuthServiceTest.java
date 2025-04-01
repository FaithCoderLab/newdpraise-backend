package faithcoderlab.newdpraise.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faithcoderlab.newdpraise.domain.auth.dto.LoginRequest;
import faithcoderlab.newdpraise.domain.auth.dto.LoginResponse;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import faithcoderlab.newdpraise.global.security.JwtUtil;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JwtUtil jwtUtil;

  @Mock
  private UserRepository userRepository;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks
  private AuthService authService;

  @Test
  @DisplayName("로그인 성공 테스트")
  void loginSuccess() {
    // given
    LoginRequest request = LoginRequest.builder()
        .email("suming@example.com")
        .password("password123!")
        .build();

    User user = User.builder()
        .id(1L)
        .email("suming@example.com")
        .name("수밍")
        .instrument("피아노")
        .role(Role.USER)
        .build();

    when(authenticationManager.authenticate(
        any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(jwtUtil.generateAccessToken(any(User.class))).thenReturn("access-token");
    when(jwtUtil.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
    when(jwtUtil.extractExpiration(anyString())).thenReturn(
        new Date(System.currentTimeMillis() + 3600000));

    // when
    LoginResponse response = authService.login(request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getEmail()).isEqualTo(user.getEmail());
    assertThat(response.getName()).isEqualTo(user.getName());
    assertThat(response.getRole()).isEqualTo(user.getRole());
    assertThat(response.getAccessToken()).isEqualTo("access-token");
    assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

    verify(refreshTokenRepository).findByUserEmail(user.getEmail());
  }

  @Test
  @DisplayName("로그인 실패 - 잘못된 자격 증명")
  void loginFailWithBadCredentials() {
    // given
    LoginRequest request = LoginRequest.builder()
        .email("suming@example.com")
        .password("wrongpassword")
        .build();

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("잘못된 자격 증명"));

    // when & then
    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(BadCredentialsException.class);
  }
}
