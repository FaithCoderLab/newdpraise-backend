package faithcoderlab.newdpraise.domain.auth;

import faithcoderlab.newdpraise.domain.auth.dto.LoginRequest;
import faithcoderlab.newdpraise.domain.auth.dto.LoginResponse;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import faithcoderlab.newdpraise.global.security.JwtProvider;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;
  private final RefreshTokenRepository refreshTokenRepository;

  @Transactional
  public LoginResponse login(LoginRequest loginRequest) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getEmail(),
            loginRequest.getPassword()
        )
    );

    User user = userRepository.findByEmail(loginRequest.getEmail())
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

    String accessToken = jwtProvider.generateAccessToken(user);
    String refreshToken = jwtProvider.generateRefreshToken(user);

    saveRefreshToken(user.getEmail(), refreshToken);

    return LoginResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .instrument(user.getInstrument())
        .profileImage(user.getProfileImage())
        .role(user.getRole())
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }

  private void saveRefreshToken(String userEmail, String token) {
    Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserEmail(userEmail);

    RefreshToken refreshToken;
    if (existingToken.isPresent()) {
      refreshToken = existingToken.get();
      refreshToken.setToken(token);
      refreshToken.setExpiresAt(toLocalDateTime(jwtProvider.extractExpiration(token)));
    } else {
      refreshToken = RefreshToken.builder()
          .token(token)
          .userEmail(userEmail)
          .expiresAt(toLocalDateTime(jwtProvider.extractExpiration(token)))
          .build();
    }

    refreshTokenRepository.save(refreshToken);
  }

  private LocalDateTime toLocalDateTime(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }
}
