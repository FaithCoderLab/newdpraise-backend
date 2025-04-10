package faithcoderlab.newdpraise.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faithcoderlab.newdpraise.domain.auth.dto.LoginRequest;
import faithcoderlab.newdpraise.domain.auth.dto.LoginResponse;
import faithcoderlab.newdpraise.domain.auth.dto.TokenRefreshRequest;
import faithcoderlab.newdpraise.domain.auth.dto.TokenRefreshResponse;
import faithcoderlab.newdpraise.domain.user.Role;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import faithcoderlab.newdpraise.global.exception.AuthenticationException;
import faithcoderlab.newdpraise.global.security.JwtProvider;
import java.time.LocalDateTime;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JwtProvider jwtProvider;

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
    when(jwtProvider.generateAccessToken(any(User.class))).thenReturn("access-token");
    when(jwtProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
    when(jwtProvider.extractExpiration(anyString())).thenReturn(
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

  @Test
  @DisplayName("토큰 재발급 성공")
  void refreshTokenSuccess() {
    // given
    TokenRefreshRequest request = new TokenRefreshRequest("valid-refresh-token");

    RefreshToken refreshToken = RefreshToken.builder()
        .id(1L)
        .token("valid-refresh-token")
        .userEmail("suming@example.com")
        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();

    User user = User.builder()
        .id(1L)
        .email("suming@example.com")
        .name("수밍")
        .role(Role.USER)
        .build();

    when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(refreshToken));
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(jwtProvider.generateAccessToken(any(User.class))).thenReturn("new-access-token");
    when(jwtProvider.generateRefreshToken(any(User.class))).thenReturn("new-refresh-token");
    when(jwtProvider.extractExpiration(anyString())).thenReturn(new Date(System.currentTimeMillis() + 3600000));
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

    // when
    TokenRefreshResponse response = authService.refreshToken(request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getAccessToken()).isEqualTo("new-access-token");
    assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");

    verify(refreshTokenRepository).findByToken("valid-refresh-token");
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("토큰 재발급 실패 - 만료된 리프레시 토큰")
  void refreshTokenFailWithExpiredToken() {
    // given
    TokenRefreshRequest request = new TokenRefreshRequest("expired-refresh-token");

    RefreshToken refreshToken = RefreshToken.builder()
        .id(1L)
        .token("expired-refresh-token")
        .userEmail("suming@example.com")
        .expiresAt(LocalDateTime.now().minusDays(1))
        .build();

    when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(refreshToken));

    // when & then
    assertThatThrownBy(() -> authService.refreshToken(request))
        .isInstanceOf(AuthenticationException.class)
        .hasMessage("만료된 리프레시 토큰입니다.");

    verify(refreshTokenRepository).findByToken("expired-refresh-token");
    verify(refreshTokenRepository).delete(refreshToken);
  }

  @Test
  @DisplayName("토큰 재발급 실패 - 연결된 사용자가 없는 경우")
  void refreshTokenFailWithUserNotFound() {
    // given
    TokenRefreshRequest request = new TokenRefreshRequest("valid-refresh-token");

    RefreshToken refreshToken = RefreshToken.builder()
        .id(1L)
        .token("valid-refresh-token")
        .userEmail("non-existing@example.com")
        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();

    when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(refreshToken));
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authService.refreshToken(request))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("사용자를 찾을 수 없습니다.");

    verify(refreshTokenRepository).findByToken("valid-refresh-token");
    verify(userRepository).findByEmail("non-existing@example.com");
  }

  @Test
  @DisplayName("로그아웃 성공")
  void logoutSuccess() {
    // given
    String userEmail = "suming@example.com";
    doNothing().when(refreshTokenRepository).deleteByUserEmail(anyString());

    // when
    authService.logout(userEmail);

    // then
    verify(refreshTokenRepository).deleteByUserEmail(userEmail);
  }
}
