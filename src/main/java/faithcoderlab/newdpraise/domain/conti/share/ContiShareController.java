package faithcoderlab.newdpraise.domain.conti.share;

import faithcoderlab.newdpraise.domain.conti.share.dto.ContiShareRequest;
import faithcoderlab.newdpraise.domain.conti.share.dto.ContiShareResponse;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
import faithcoderlab.newdpraise.global.exception.AuthenticationException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conti/shares")
@RequiredArgsConstructor
@Tag(name = "ContiShare", description = "콘티 공유 관련 API")
public class ContiShareController {

  private final ContiShareService contiShareService;
  private final UserRepository userRepository;

  @Operation(summary = "콘티 공유 초대 생성", description = "다른 사용자에게 콘티를 공유합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "공유 초대 생성 성공",
          content = @Content(schema = @Schema(implementation = ContiShareResponse.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "공유 권한 없음"),
      @ApiResponse(responseCode = "404", description = "콘티 또는 사용자를 찾을 수 없음"),
      @ApiResponse(responseCode = "409", description = "이미 공유된 콘티")
  })
  @PostMapping
  public ResponseEntity<ContiShareResponse> createShare(
      @Valid @RequestBody ContiShareRequest request,
      Principal principal
  ) {
    User currentUser = getUserFromPrincipal(principal);
    ContiShare contiShare = contiShareService.createShare(request, currentUser);
    ContiShareResponse response = ContiShareResponse.fromContiShare(contiShare);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "콘티 공유 초대 수락", description = "공유 초대를 수락합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "공유 초대 수락 성공",
          content = @Content(schema = @Schema(implementation = ContiShareResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "수락 권한 없음"),
      @ApiResponse(responseCode = "404", description = "공유 정보를 찾을 수 없음"),
      @ApiResponse(responseCode = "409", description = "이미 수락된 공유 요청")
  })
  @PostMapping("/{shareId}/accept")
  public ResponseEntity<ContiShareResponse> acceptShare(
      @PathVariable Long shareId,
      Principal principal
  ) {
    User currentUser = getUserFromPrincipal(principal);
    ContiShare contiShare = contiShareService.acceptShare(shareId, currentUser);
    ContiShareResponse response = ContiShareResponse.fromContiShare(contiShare);

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "콘티 공유 삭제", description = "공유 초대를 거절하거나 기존 공유를 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "공유 삭제 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
      @ApiResponse(responseCode = "404", description = "공유 정보를 찾을 수 없음")
  })
  @DeleteMapping("/{shareId}")
  public ResponseEntity<Void> deleteShare(
      @PathVariable Long shareId,
      Principal principal
  ) {
    User currentUser = getUserFromPrincipal(principal);
    contiShareService.deleteShare(shareId, currentUser);

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "콘티 공유 권한 수정", description = "공유된 콘티의 권한을 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "권한 수정 성공",
          content = @Content(schema = @Schema(implementation = ContiShareResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "권한 수정 권한 없음"),
      @ApiResponse(responseCode = "404", description = "공유 정보를 찾을 수 없음")
  })
  @PutMapping("/{shareId}/permission")
  public ResponseEntity<ContiShareResponse> updatePermission(
      @PathVariable Long shareId,
      @RequestParam ContiSharePermission permission,
      Principal principal
  ) {
    User currentUser = getUserFromPrincipal(principal);
    ContiShare contiShare = contiShareService.updateSharePermission(shareId, permission, currentUser);
    ContiShareResponse response = ContiShareResponse.fromContiShare(contiShare);

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "내게 공유된 콘티 목록 조회", description = "현재 사용자에게 공유된 콘티 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/shared-with-me")
  public ResponseEntity<List<ContiShareResponse>> getSharedWithMe(Principal principal) {
    User currentUser = getUserFromPrincipal(principal);
    List<ContiShare> shares = contiShareService.getSharedContis(currentUser);

    List<ContiShareResponse> responses = shares.stream()
        .map(ContiShareResponse::fromContiShare)
        .collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  @Operation(summary = "내게 온 공유 요청 목록 조회", description = "현재 사용자에게 온 미수락 공유 요청 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/pending")
  public ResponseEntity<List<ContiShareResponse>> getPendingShares(Principal principal) {
    User currentUser = getUserFromPrincipal(principal);
    List<ContiShare> shares = contiShareService.getPendingShares(currentUser);

    List<ContiShareResponse> responses = shares.stream()
        .map(ContiShareResponse::fromContiShare)
        .collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  @Operation(summary = "콘티 공유 목록 조회", description = "특정 콘티의 공유 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "조회 권한 없음"),
      @ApiResponse(responseCode = "404", description = "콘티를 찾을 수 없음")
  })
  @GetMapping("/conti/{contiId}")
  public ResponseEntity<List<ContiShareResponse>> getContiShares(
      @PathVariable Long contiId,
      Principal principal
  ) {
    User currentUser = getUserFromPrincipal(principal);
    List<ContiShare> shares = contiShareService.getContiShares(contiId, currentUser);

    List<ContiShareResponse> responses = shares.stream()
        .map(ContiShareResponse::fromContiShare)
        .collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  private User getUserFromPrincipal(Principal principal) {
    if (principal == null) {
      throw new AuthenticationException("인증되지 않은 사용자입니다.");
    }

    return userRepository.findByEmail(principal.getName())
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
  }
}
