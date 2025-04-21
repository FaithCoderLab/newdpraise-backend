package faithcoderlab.newdpraise.domain.conti;

import faithcoderlab.newdpraise.domain.conti.dto.ContiCreateRequest;
import faithcoderlab.newdpraise.domain.conti.dto.ContiParseRequest;
import faithcoderlab.newdpraise.domain.conti.dto.ContiParseResponse;
import faithcoderlab.newdpraise.domain.conti.dto.ContiResponse;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.domain.user.UserRepository;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conti")
@RequiredArgsConstructor
@Tag(name = "Conti", description = "콘티 관련 API")
public class ContiController {

  private final ContiParserService contiParserService;
  private final ContiService contiService;
  private final UserRepository userRepository;

  @Operation(summary = "콘티 텍스트 파싱", description = "텍스트 형태의 콘티를 파싱하여 구조화된 데이터로 변환합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "파싱 성공",
          content = @Content(schema = @Schema(implementation = ContiParseResponse.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 데이터"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @PostMapping("/parse")
  public ResponseEntity<ContiParseResponse> parseContiText(
      @Valid @RequestBody ContiParseRequest request, Principal principal
  ) {

    User user = getUserFromPrincipal(principal);
    Conti conti = contiParserService.parseContiText(request.getContiText(), user);
    ContiParseResponse response = mapToParseResponse(conti);

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "콘티 생성", description = "새로운 콘티를 생성합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "콘티 생성 성공",
          content = @Content(schema = @Schema(implementation = ContiResponse.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @PostMapping
  public ResponseEntity<ContiResponse> createConti(
      @Valid @RequestBody ContiCreateRequest request, Principal principal
  ) {

    User user = getUserFromPrincipal(principal);
    Conti conti = contiService.createConti(request, user);
    ContiResponse response = mapToContiResponse(conti);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "콘티 목록 조회", description = "사용자의 콘티 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping
  public ResponseEntity<List<ContiResponse>> getContiList(Principal principal) {
    User user = getUserFromPrincipal(principal);
    List<Conti> contiList = contiService.getUserContiList(user);
    List<ContiResponse> responseList = contiList.stream()
        .map(this::mapToContiResponse)
        .collect(Collectors.toList());

    return ResponseEntity.ok(responseList);
  }

  @Operation(summary = "콘티 상세 조회", description = "특정 콘티의 상세 정보를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공",
          content = @Content(schema = @Schema(implementation = ContiResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "콘티를 찾을 수 없음")
  })
  @GetMapping("/{contiId}")
  public ResponseEntity<ContiResponse> getContiDetail(
      @PathVariable Long contiId, Principal principal
  ) {

    getUserFromPrincipal(principal);
    Conti conti = contiService.getContiById(contiId);
    ContiResponse response = mapToContiResponse(conti);

    return ResponseEntity.ok(response);
  }

  private User getUserFromPrincipal(Principal principal) {
    if (principal == null) {
      throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
    }

    return userRepository.findByEmail(principal.getName())
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
  }

  private ContiParseResponse mapToParseResponse(Conti conti) {
    List<ContiParseResponse.SongDto> songDtos = conti.getSongs().stream()
        .map(song -> ContiParseResponse.SongDto.builder()
            .title(song.getTitle())
            .originalKey(song.getOriginalKey())
            .performanceKey(song.getPerformanceKey())
            .youtubeUrl(song.getYoutubeUrl())
            .specialInstructions(song.getSpecialInstructions())
            .bpm(song.getBpm())
            .build())
        .toList();

    return ContiParseResponse.builder()
        .title(conti.getTitle())
        .scheduledAt(conti.getScheduledAt())
        .songs(songDtos)
        .version(conti.getVersion())
        .status(conti.getStatus().name())
        .build();
  }

  private ContiResponse mapToContiResponse(Conti conti) {
    return ContiResponse.builder()
        .id(conti.getId())
        .title(conti.getTitle())
        .description(conti.getDescription())
        .scheduledAt(conti.getScheduledAt())
        .creatorId(conti.getCreator() != null ? conti.getCreator().getId() : null)
        .creatorName(conti.getCreator() != null ? conti.getCreator().getName() : null)
        .songs(conti.getSongs().stream()
            .map(song -> ContiResponse.SongDto.builder()
                .id(song.getId())
                .title(song.getTitle())
                .originalKey(song.getOriginalKey())
                .performanceKey(song.getPerformanceKey())
                .artist(song.getArtist())
                .youtubeUrl(song.getYoutubeUrl())
                .specialInstructions(song.getSpecialInstructions())
                .build())
            .collect(Collectors.toList()))
        .status(conti.getStatus().name())
        .createdAt(conti.getCreatedAt())
        .updatedAt(conti.getUpdatedAt())
        .build();
  }
}
