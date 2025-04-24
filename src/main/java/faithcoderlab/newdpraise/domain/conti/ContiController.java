package faithcoderlab.newdpraise.domain.conti;

import faithcoderlab.newdpraise.domain.conti.dto.ContiCreateRequest;
import faithcoderlab.newdpraise.domain.conti.dto.ContiParseRequest;
import faithcoderlab.newdpraise.domain.conti.dto.ContiParseResponse;
import faithcoderlab.newdpraise.domain.conti.dto.ContiResponse;
import faithcoderlab.newdpraise.domain.conti.dto.ContiSearchRequest;
import faithcoderlab.newdpraise.domain.conti.dto.ContiUpdateRequest;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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

  @Operation(summary = "콘티 수정", description = "기존 콘티를 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "콘티 수정 성공",
          content = @Content(schema = @Schema(implementation = ContiResponse.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "콘티를 찾을 수 없음")
  })
  @PutMapping("/{contiId}")
  public ResponseEntity<ContiResponse> updateConti(
      @PathVariable Long contiId,
      @Valid @RequestBody ContiUpdateRequest request,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);
    Conti conti = contiService.updateConti(contiId, request, user);
    ContiResponse response = mapToContiResponse(conti);

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "콘티 목록 조회", description = "사용자의 콘티 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping
  @Transactional(readOnly = true)
  public ResponseEntity<List<ContiResponse>> getContiList(
      Principal principal,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
      @RequestParam(required = false) String title
  ) {
    User user = getUserFromPrincipal(principal);
    List<Conti> contiList;

    if (startDate != null && endDate != null && title != null && !title.isEmpty()) {
      contiList = contiService.searchByDateRangeAndTitle(startDate, endDate, title);
    } else if (startDate != null && endDate != null) {
      contiList = contiService.searchByDateRange(startDate, endDate);
    } else if (title != null && !title.isEmpty()) {
      contiList = contiService.searchByTitleForUser(user, title);
    } else {
      contiList = contiService.getUserContiList(user);
    }

    List<ContiResponse> responseList = contiList.stream()
        .map(this::mapToContiResponse)
        .collect(Collectors.toList());

    return ResponseEntity.ok(responseList);
  }

  @Operation(summary = "페이징 처리된 콘티 목록 조회", description = "페이징 처리된 사용자의 콘티 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/paged")
  @Transactional(readOnly = true)
  public ResponseEntity<Page<ContiResponse>> getPagedContiList(
      Principal principal,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "scheduledAt") String sort,
      @RequestParam(defaultValue = "desc") String direction
  ) {
    User user = getUserFromPrincipal(principal);

    Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
        Sort.Direction.ASC : Sort.Direction.DESC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

    Page<Conti> contiPage = contiService.getUserContiList(user, pageable);
    Page<ContiResponse> responsePage = contiPage.map(this::mapToContiResponse);

    return ResponseEntity.ok(responsePage);
  }

  @Operation(summary = "고급 검색", description = "다양한 조건으로 콘티를 검색합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "검색 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @PostMapping("/search")
  @Transactional(readOnly = true)
  public ResponseEntity<Page<ContiResponse>> advancedSearch(
      @RequestBody ContiSearchRequest request,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "scheduledAt") String sort,
      @RequestParam(defaultValue = "desc") String direction,
      Principal principal
  ) {
    getUserFromPrincipal(principal);

    Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
        Sort.Direction.ASC : Sort.Direction.DESC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

    Page<Conti> contiPage = contiService.advancedSearch(request, pageable);
    Page<ContiResponse> responsePage = contiPage.map(this::mapToContiResponse);

    return ResponseEntity.ok(responsePage);
  }

  @Operation(summary = "콘티 상태 업데이트", description = "콘티의 상태를 업데이트합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "상태 업데이트 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "콘티를 찾을 수 없음")
  })
  @PutMapping("/{contiId}/status")
  public ResponseEntity<ContiResponse> updateContiStatus(
      @PathVariable Long contiId,
      @RequestParam ContiStatus status,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);
    Conti conti = contiService.getContiByIdAndCreator(contiId, user);

    contiService.updateContiStatus(contiId, status);
    conti = contiService.getContiById(contiId);

    ContiResponse response = mapToContiResponse(conti);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "콘티 삭제", description = "콘티를 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "콘티를 찾을 수 없음")
  })
  @DeleteMapping("/{contiId}")
  public ResponseEntity<Void> deleteConti(
      @PathVariable Long contiId,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);
    Conti conti = contiService.getContiByIdAndCreator(contiId, user);

    contiService.deleteConti(contiId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "예정된 콘티 목록 조회", description = "특정 날짜 이후의 예정된 콘티 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/upcoming")
  @Transactional(readOnly = true)
  public ResponseEntity<List<ContiResponse>> getUpcomingContis(
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date,
      Principal principal
  ) {
    getUserFromPrincipal(principal);

    LocalDate searchDate = date != null ? date : LocalDate.now();
    List<Conti> contiList = contiService.getUpcomingContis(searchDate);

    List<ContiResponse> responseList = contiList.stream()
        .map(this::mapToContiResponse)
        .collect(Collectors.toList());

    return ResponseEntity.ok(responseList);
  }

  @Operation(summary = "과거 콘티 목록 조회", description = "특정 날짜 이전의 과거 콘티 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/past")
  @Transactional(readOnly = true)
  public ResponseEntity<List<ContiResponse>> getPastContis(
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date,
      Principal principal
  ) {
    getUserFromPrincipal(principal);

    LocalDate searchDate = date != null ? date : LocalDate.now();
    List<Conti> contiList = contiService.getPastContis(searchDate);

    List<ContiResponse> responseList = contiList.stream()
        .map(this::mapToContiResponse)
        .collect(Collectors.toList());

    return ResponseEntity.ok(responseList);
  }

  @Operation(summary = "상태별 콘티 목록 조회", description = "특정 상태의 콘티 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/status/{status}")
  @Transactional(readOnly = true)
  public ResponseEntity<List<ContiResponse>> getContisByStatus(
      @PathVariable ContiStatus status,
      @RequestParam(required = false, defaultValue = "false") boolean onlyMine,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);

    List<Conti> contiList;
    if (onlyMine) {
      contiList = contiService.getUserContisByStatus(user, status);
    } else {
      contiList = contiService.getContisByStatus(status);
    }

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
  @Transactional(readOnly = true)
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
      throw new AuthenticationException("인증되지 않은 사용자입니다.");
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
    List<ContiResponse.SongDto> songDtos = new ArrayList<>();
    if (conti.getSongs() != null) {
      songDtos = conti.getSongs().stream()
          .map(song -> ContiResponse.SongDto.builder()
              .id(song.getId())
              .title(song.getTitle())
              .originalKey(song.getOriginalKey())
              .performanceKey(song.getPerformanceKey())
              .artist(song.getArtist())
              .youtubeUrl(song.getYoutubeUrl())
              .specialInstructions(song.getSpecialInstructions())
              .build())
          .toList();
    }

    return ContiResponse.builder()
        .id(conti.getId())
        .title(conti.getTitle())
        .description(conti.getDescription())
        .scheduledAt(conti.getScheduledAt())
        .creatorId(conti.getCreator() != null ? conti.getCreator().getId() : null)
        .creatorName(conti.getCreator() != null ? conti.getCreator().getName() : null)
        .songs(songDtos)
        .status(conti.getStatus().name())
        .createdAt(conti.getCreatedAt())
        .updatedAt(conti.getUpdatedAt())
        .build();
  }
}
