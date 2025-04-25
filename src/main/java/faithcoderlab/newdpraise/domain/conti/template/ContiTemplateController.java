package faithcoderlab.newdpraise.domain.conti.template;

import faithcoderlab.newdpraise.domain.conti.Conti;
import faithcoderlab.newdpraise.domain.conti.dto.ContiResponse;
import faithcoderlab.newdpraise.domain.conti.template.dto.ContiTemplateApplyRequest;
import faithcoderlab.newdpraise.domain.conti.template.dto.ContiTemplateCreateRequest;
import faithcoderlab.newdpraise.domain.conti.template.dto.ContiTemplateResponse;
import faithcoderlab.newdpraise.domain.conti.template.dto.ContiTemplateUpdateRequest;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/conti/templates")
@RequiredArgsConstructor
@Tag(name = "ContiTemplate", description = "콘티 템플릿 관련 API")
public class ContiTemplateController {

  private final ContiTemplateService contiTemplateService;
  private final UserRepository userRepository;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd HH:mm:ss");

  @Operation(summary = "템플릿 생성", description = "새로운 콘티 템플릿을 생성합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "템플릿 생성 성공",
          content = @Content(schema = @Schema(implementation = ContiTemplateResponse.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "원본 콘티를 찾을 수 없음")
  })
  @PostMapping
  public ResponseEntity<ContiTemplateResponse> createTemplate(
      @Valid @RequestBody ContiTemplateCreateRequest request,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);
    ContiTemplate template = contiTemplateService.createTemplate(request, user);

    ContiTemplateResponse response = mapToTemplateResponse(template, user);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "템플릿 조회", description = "특정 콘티 템플릿을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "템플릿 조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "템플릿 접근 권한 없음"),
      @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
  })
  @GetMapping("/{templateId}")
  public ResponseEntity<ContiTemplateResponse> getTemplate(
      @PathVariable Long templateId,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);
    ContiTemplate template = contiTemplateService.getAccessibleTemplate(templateId, user);

    ContiTemplateResponse response = mapToTemplateResponse(template, user);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "템플릿 수정", description = "기존 콘티 템플릿을 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "템플릿 수정 성공"),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "템플릿 수정 권한 없음"),
      @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
  })
  @PutMapping("/{templateId}")
  public ResponseEntity<ContiTemplateResponse> updateTemplate(
      @PathVariable Long templateId,
      @Valid @RequestBody ContiTemplateUpdateRequest request,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);
    ContiTemplate template = contiTemplateService.updateTemplate(templateId, request, user);

    ContiTemplateResponse response = mapToTemplateResponse(template, user);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "템플릿 삭제", description = "콘티 템플릿을 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "템플릿 삭제 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "템플릿 삭제 권한 없음"),
      @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
  })
  @DeleteMapping("/{templateId}")
  public ResponseEntity<Void> deleteTemplate(
      @PathVariable Long templateId,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);
    contiTemplateService.deleteTemplate(templateId, user);

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "내 템플릿 목록 조회", description = "사용자가 생성한 템플릿 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/my")
  public ResponseEntity<List<ContiTemplateResponse>> getMyTemplates(Principal principal) {
    User user = getUserFromPrincipal(principal);
    List<ContiTemplate> templates = contiTemplateService.getUserTemplates(user);

    List<ContiTemplateResponse> responses = templates.stream()
        .map(template -> mapToTemplateResponse(template, user))
        .collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  @Operation(summary = "내 템플릿 목록 페이징 조회", description = "사용자가 생성한 템플릿 목록을 페이징하여 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/my/paged")
  public ResponseEntity<Page<ContiTemplateResponse>> getMyTemplates(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);

    Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
        Sort.Direction.ASC : Sort.Direction.DESC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
    Page<ContiTemplate> templatePage = contiTemplateService.getUserTemplates(user, pageable);

    Page<ContiTemplateResponse> responsePage = templatePage.map(
        template -> mapToTemplateResponse(template, user)
    );

    return ResponseEntity.ok(responsePage);
  }

  @Operation(summary = "공개 템플릿 목록 조회", description = "공개된 템플릿 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/public")
  public ResponseEntity<List<ContiTemplateResponse>> getPublicTemplates(Principal principal) {
    User user = getUserFromPrincipal(principal);
    List<ContiTemplate> templates = contiTemplateService.getPublicTemplates();

    List<ContiTemplateResponse> responses = templates.stream()
        .map(template -> mapToTemplateResponse(template, user))
        .collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  @Operation(summary = "공개 템플릿 목록 페이징 조회", description = "공개된 템플릿 목록을 페이징하여 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/public/paged")
  public ResponseEntity<Page<ContiTemplateResponse>> getPublicTemplatesPaged(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);

    Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
        Sort.Direction.ASC : Sort.Direction.DESC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
    Page<ContiTemplate> templatePage = contiTemplateService.getPublicTemplates(pageable);

    Page<ContiTemplateResponse> responsePage = templatePage.map(
        template -> mapToTemplateResponse(template, user)
    );

    return ResponseEntity.ok(responsePage);
  }

  @Operation(summary = "접근 가능한 템플릿 목록 조회", description = "사용자가 접근 가능한 모든 템플릿 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/accessible")
  public ResponseEntity<List<ContiTemplateResponse>> getAccessibleTemplates(Principal principal) {
    User user = getUserFromPrincipal(principal);
    List<ContiTemplate> templates = contiTemplateService.getAccessibleTemplates(user);

    List<ContiTemplateResponse> responses = templates.stream()
        .map(template -> mapToTemplateResponse(template, user))
        .collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  @Operation(summary = "인기 템플릿 목록 조회", description = "사용 횟수 기준 인기 템플릿 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/popular")
  public ResponseEntity<List<ContiTemplateResponse>> getPopularTemplates(
      @RequestParam(defaultValue = "5") int limit,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);
    List<ContiTemplate> templates = contiTemplateService.getPopularTemplates(limit);

    List<ContiTemplateResponse> responses = templates.stream()
        .map(template -> mapToTemplateResponse(template, user))
        .collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  @Operation(summary = "템플릿 적용", description = "템플릿을 적용하여 새 콘티를 생성합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "콘티 생성 성공",
          content = @Content(schema = @Schema(implementation = ContiResponse.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "템플릿 접근 권한 없음"),
      @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음")
  })
  @PostMapping("/apply")
  public ResponseEntity<ContiResponse> applyTemplate(
      @Valid @RequestBody ContiTemplateApplyRequest request,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);
    Conti conti = contiTemplateService.applyTemplate(request.getTemplateId(), user);

    if (request.getCustomTitle() != null && !request.getCustomTitle().isBlank()) {
      conti.setTitle(request.getCustomTitle());
    }

    if (request.getCustomDescription() != null && !request.getCustomDescription().isBlank()) {
      conti.setDescription(request.getCustomDescription());
    }

    ContiResponse response = mapToContiResponse(conti);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  private User getUserFromPrincipal(Principal principal) {
    if (principal == null) {
      throw new AuthenticationException("인증되지 않은 사용자입니다.");
    }

    return userRepository.findByEmail(principal.getName())
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
  }

  private ContiTemplateResponse mapToTemplateResponse(ContiTemplate template, User currentUser) {
    boolean isOwner = template.getCreator() != null &&
        template.getCreator().getId().equals(currentUser.getId());

    return ContiTemplateResponse.builder()
        .id(template.getId())
        .name(template.getName())
        .description(template.getDescription())
        .creatorId(template.getCreator() != null ? template.getCreator().getId() : null)
        .creatorName(template.getCreator() != null ? template.getCreator().getName() : null)
        .isPublic(template.isPublic())
        .usageCount(template.getUsageCount())
        .songCount(template.getSongs() != null ? template.getSongs().size() : 0)
        .createdAt(
            template.getCreatedAt() != null ? template.getCreatedAt().format(DATE_FORMATTER) : null)
        .updatedAt(
            template.getUpdatedAt() != null ? template.getUpdatedAt().format(DATE_FORMATTER) : null)
        .isOwner(isOwner)
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
          .collect(Collectors.toList());
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
        .canEdit(true)
        .build();
  }
}
