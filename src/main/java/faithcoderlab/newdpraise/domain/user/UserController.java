package faithcoderlab.newdpraise.domain.user;

import faithcoderlab.newdpraise.domain.user.dto.SignUpRequest;
import faithcoderlab.newdpraise.domain.user.dto.SignUpResponse;
import faithcoderlab.newdpraise.domain.user.dto.UpdateProfileRequest;
import faithcoderlab.newdpraise.domain.user.dto.UserProfileResponse;
import faithcoderlab.newdpraise.global.exception.AuthenticationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

  private final UserService userService;

  @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "회원가입 성공",
          content = @Content(schema = @Schema(implementation = SignUpResponse.class))),
      @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일"),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터")
  })
  @PostMapping("/signup")
  public ResponseEntity<SignUpResponse> signup(@Valid @RequestBody SignUpRequest request) {
    SignUpResponse response = userService.signup(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "현재 사용자 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "프로필 조회 성공",
          content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
  })
  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getCurrentUserProfile(Principal principal) {
    if (principal == null) {
      throw new AuthenticationException("인증되지 않은 사용자입니다.");
    }
    UserProfileResponse response = userService.getCurrentUserProfile(principal.getName());
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "사용자 프로필 조회", description = "특정 사용자의 프로필 정보를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "프로필 조회 성공",
          content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
  })
  @GetMapping("/{userId}")
  public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
    UserProfileResponse response = userService.getUserProfile(userId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "프로필 업데이트", description = "현재 로그인한 사용자의 프로필 정보를 업데이트합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "프로필 업데이트 성공",
          content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
  })
  @PutMapping("/me")
  public ResponseEntity<UserProfileResponse> updateProfile(
      Principal principal,
      @Valid @RequestBody UpdateProfileRequest request
  ) {
    UserProfileResponse response = userService.updateProfile(principal.getName(), request);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "프로필 이미지 업로드", description = "현재 로그인한 사용자의 프로필 이미지를 업로드합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "이미지 업로드 성공",
          content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 파일 또는 파일 형식"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
  })
  @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UserProfileResponse> uploadProfileImage(
      Principal principal,
      @RequestParam("file") MultipartFile file
  ) {
    UserProfileResponse response = userService.updateProfileImage(principal.getName(), file);
    return ResponseEntity.ok(response);
  }
}
