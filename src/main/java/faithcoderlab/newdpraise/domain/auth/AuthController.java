package faithcoderlab.newdpraise.domain.auth;

import faithcoderlab.newdpraise.domain.auth.dto.LoginRequest;
import faithcoderlab.newdpraise.domain.auth.dto.LoginResponse;
import faithcoderlab.newdpraise.domain.auth.dto.TokenRefreshRequest;
import faithcoderlab.newdpraise.domain.auth.dto.TokenRefreshResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "로그인 성공",
          content = @Content(schema = @Schema(implementation = LoginResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증 실패"),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터")
  })
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
    LoginResponse response = authService.login(loginRequest);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
          content = @Content(schema = @Schema(implementation = TokenRefreshResponse.class))),
      @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
  })
  @PostMapping("/refresh")
  public ResponseEntity<TokenRefreshResponse> refreshToken(
      @Valid @RequestBody TokenRefreshRequest request
  ) {
    TokenRefreshResponse response = authService.refreshToken(request);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "로그아웃", description = "현재 사용자의 리프레시 토큰을 무효화합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(Principal principal) {
    authService.logout(principal.getName());
    return ResponseEntity.ok().build();
  }
}
