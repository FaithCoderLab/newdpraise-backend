package faithcoderlab.newdpraise.domain.song.download;

import faithcoderlab.newdpraise.domain.song.download.dto.DownloadTaskCreateRequest;
import faithcoderlab.newdpraise.domain.song.download.dto.DownloadTaskDto;
import faithcoderlab.newdpraise.domain.song.download.dto.DownloadTaskUpdateRequest;
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
import java.util.concurrent.CompletableFuture;
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
@RequestMapping("/api/songs/downloads")
@RequiredArgsConstructor
@Tag(name = "DownloadTask", description = "음원 다운로드 작업 관리 API")
public class DownloadTaskController {

  private final DownloadTaskService downloadTaskService;
  private final UserRepository userRepository;

  @Operation(summary = "다운로드 작업 생성", description = "유튜브 URL에서 오디오 다운로드 작업을 생성합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "다운로드 작업 생성 성공",
          content = @Content(schema = @Schema(implementation = DownloadTaskDto.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 URL"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "409", description = "이미 다운로드 중이거나 다운로드된 비디오")
  })
  @PostMapping
  public ResponseEntity<DownloadTaskDto> createDownloadTask(
      @Valid @RequestBody DownloadTaskCreateRequest request, Principal principal
  ) {
    User user = getUserFromPrincipal(principal);

    DownloadTask task = downloadTaskService.createDownloadTask(
        request.getYoutubeUrl(),
        request.getCustomTitle(),
        request.getCustomArtist(),
        user
    );

    DownloadTaskDto response = downloadTaskService.mapToDto(task);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "다운로드 시작", description = "대기 중인 다운로드 작업을 시작합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "다운로드 시작됨"),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 요청"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "다운로드 작업을 찾을 수 없음")
  })
  @PostMapping("/{taskId}/start")
  public ResponseEntity<Void> startDownload(@PathVariable Long taskId, Principal principal) {
    getUserFromPrincipal(principal);

    CompletableFuture<DownloadTask> future = downloadTaskService.startDownload(taskId);

    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  @Operation(summary = "다운로드 취소", description = "진행 중인 다운로드 작업을 취소합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "다운로드 취소 성공",
          content = @Content(schema = @Schema(implementation = DownloadTaskDto.class))),
      @ApiResponse(responseCode = "400", description = "이미 완료된 다운로드"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "다운로드 취소 권한 없음"),
      @ApiResponse(responseCode = "404", description = "다운로드 작업을 찾을 수 없음")
  })
  @PostMapping("/{taskId}/cancel")
  public ResponseEntity<DownloadTaskDto> cancelDownload(@PathVariable Long taskId,
      Principal principal) {
    User user = getUserFromPrincipal(principal);

    DownloadTask task = downloadTaskService.cancelDownload(taskId, user);

    DownloadTaskDto response = downloadTaskService.mapToDto(task);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "다운로드 작업 삭제", description = "다운로드 작업을 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "다운로드 작업 삭제 성공"),
      @ApiResponse(responseCode = "400", description = "진행 중인 다운로드"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "다운로드 삭제 권한 없음"),
      @ApiResponse(responseCode = "404", description = "다운로드 작업을 찾을 수 없음")
  })
  @DeleteMapping("/{taskId}")
  public ResponseEntity<Void> deleteDownloadTask(@PathVariable Long taskId, Principal principal) {
    User user = getUserFromPrincipal(principal);

    downloadTaskService.deleteDownloadTask(taskId, user);

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "다운로드 작업 조회", description = "특정 다운로드 작업을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "다운로드 작업 조회 성공",
          content = @Content(schema = @Schema(implementation = DownloadTaskDto.class))),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "다운로드 작업을 찾을 수 없음")
  })
  @GetMapping("/{taskId}")
  public ResponseEntity<DownloadTaskDto> getDownloadTask(@PathVariable Long taskId,
      Principal principal) {
    getUserFromPrincipal(principal);

    DownloadTask task = downloadTaskService.getDownloadTask(taskId);

    DownloadTaskDto response = downloadTaskService.mapToDto(task);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "다운로드 작업 목록 조회", description = "사용자의 다운로드 작업 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "다운로드 작업 목록 조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping
  public ResponseEntity<List<DownloadTaskDto>> getDownloadTasks(Principal principal) {
    User user = getUserFromPrincipal(principal);

    List<DownloadTask> tasks = downloadTaskService.getUserDownloadTasks(user);

    List<DownloadTaskDto> response = downloadTaskService.mapToDtoList(tasks);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "페이징된 다운로드 작업 목록 조회", description = "사용자의 다운로드 작업 목록을 페이징하여 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "다운로드 작업 목록 조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/paged")
  public ResponseEntity<Page<DownloadTaskDto>> getPagedDownloadTasks(
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

    Page<DownloadTask> taskPage = downloadTaskService.getUserDownloadTasks(user, pageable);

    Page<DownloadTaskDto> response = taskPage.map(downloadTaskService::mapToDto);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "활성 다운로드 작업 목록 조회", description = "사용자의 활성(대기 중, 진행 중) 다운로드 작업 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "활성 다운로드 작업 목록 조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/active")
  public ResponseEntity<List<DownloadTaskDto>> getActiveDownloadTasks(Principal principal) {
    User user = getUserFromPrincipal(principal);

    List<DownloadTask> tasks = downloadTaskService.getUserActiveDownloadTasks(user);

    List<DownloadTaskDto> response = downloadTaskService.mapToDtoList(tasks);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "다운로드 진행률 조회", description = "특정 다운로드 작업의 진행률을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "진행률 조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/{videoId}/progress")
  public ResponseEntity<Float> getDownloadProgress(@PathVariable String videoId,
      Principal principal) {
    getUserFromPrincipal(principal);

    float progress = downloadTaskService.getDownloadProgress(videoId);
    return ResponseEntity.ok(progress);
  }

  @Operation(summary = "다운로드 작업 메타데이터 업데이트", description = "다운로드 작업의 메타데이터를 업데이트합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "메타데이터 업데이트 성공",
          content = @Content(schema = @Schema(implementation = DownloadTaskDto.class))),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "403", description = "메타데이터 업데이트 권한 없음"),
      @ApiResponse(responseCode = "404", description = "다운로드 작업을 찾을 수 없음")
  })
  @PutMapping("/{taskId}")
  public ResponseEntity<DownloadTaskDto> updateDownloadTask(
      @PathVariable Long taskId,
      @Valid @RequestBody DownloadTaskUpdateRequest request,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);

    DownloadTask task = downloadTaskService.getDownloadTask(taskId);

    if (!task.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("해당 다운로드 작업을 수정할 권한이 없습니다");
    }

    if (request.getTitle() != null) {
      task.setTitle(request.getTitle());
    }

    if (request.getArtist() != null) {
      task.setArtist(request.getArtist());
    }

    DownloadTask updatedTask = downloadTaskService.updateDownloadTask(task);

    DownloadTaskDto response = downloadTaskService.mapToDto(updatedTask);
    return ResponseEntity.ok(response);
  }

  private User getUserFromPrincipal(Principal principal) {
    if (principal == null) {
      throw new AuthenticationException("인증되지 않은 사용자입니다.");
    }

    return userRepository.findByEmail(principal.getName())
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
  }
}
