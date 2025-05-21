package faithcoderlab.newdpraise.domain.song.controller;

import faithcoderlab.newdpraise.domain.song.dto.AudioDownloadResult;
import faithcoderlab.newdpraise.domain.song.dto.AudioFileDto;
import faithcoderlab.newdpraise.domain.song.dto.AudioMetadataUpdateRequest;
import faithcoderlab.newdpraise.domain.song.dto.YoutubeDownloadRequest;
import faithcoderlab.newdpraise.domain.song.service.AudioFileService;
import faithcoderlab.newdpraise.domain.song.service.YoutubeDownloadService;
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
@RequestMapping("/api/songs/audio")
@RequiredArgsConstructor
@Tag(name = "AudioFile", description = "오디오 파일 관리 API")
public class AudioFileController {

  private final AudioFileService audioFileService;
  private final YoutubeDownloadService youtubeDownloadService;
  private final UserRepository userRepository;

  @Operation(summary = "유튜브 오디오 다운로드 및 저장", description = "유튜브 URL에서 오디오를 다운로드하고 데이터베이스에 저장합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "다운로드 및 저장 성공",
          content = @Content(schema = @Schema(implementation = AudioFileDto.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 URL"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "409", description = "이미 다운로드된 파일")
  })
  @PostMapping("/download")
  public ResponseEntity<AudioFileDto> downloadAndSaveAudio(
      @Valid @RequestBody YoutubeDownloadRequest request, Principal principal) {
    User user = getUserFromPrincipal(principal);

    if (!youtubeDownloadService.isValidYoutubeUrl(request.getUrl())) {
      return ResponseEntity.badRequest().build();
    }

    AudioDownloadResult downloadResult = youtubeDownloadService.downloadAudio(request.getUrl());

    if (request.getCustomTitle() != null && !request.getCustomTitle().isBlank()) {
      downloadResult.setTitle(request.getCustomTitle());
    }

    if (request.getCustomArtist() != null && !request.getCustomArtist().isBlank()) {
      downloadResult.setArtist(request.getCustomArtist());
    }

    var audioFile = audioFileService.saveAudioFile(downloadResult, user, request.getUrl());

    AudioFileDto audioFileDto = AudioFileDto.builder()
        .id(audioFile.getId())
        .videoId(audioFile.getVideoId())
        .title(audioFile.getTitle())
        .artist(audioFile.getArtist())
        .fileName(audioFile.getFileName())
        .fileSize(audioFile.getFileSize())
        .mimeType(audioFile.getMimeType())
        .extension(audioFile.getExtension())
        .bitrate(audioFile.getBitrate())
        .durationSeconds(audioFile.getDurationSeconds())
        .thumbnailUrl(audioFile.getThumbnailUrl())
        .downloadUrl(audioFile.getDownloadUrl())
        .uploaderId(user != null ? user.getId() : null)
        .uploaderName(user != null ? user.getName() : null)
        .createdAt(audioFile.getCreatedAt())
        .updatedAt(audioFile.getUpdatedAt())
        .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(audioFileDto);
  }

  @Operation(summary = "사용자의 오디오 파일 목록 조회", description = "현재 사용자가 다운로드한 오디오 파일 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/my")
  public ResponseEntity<List<AudioFileDto>> getMyAudioFiles(Principal principal) {
    User user = getUserFromPrincipal(principal);
    List<AudioFileDto> audioFiles = audioFileService.getUserAudioFiles(user);
    return ResponseEntity.ok(audioFiles);
  }

  @Operation(summary = "사용자의 오디오 파일 목록 페이징 조회", description = "현재 사용자가 다운로드한 오디오 파일 목록을 페이징하여 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/my/paged")
  public ResponseEntity<Page<AudioFileDto>> getMyAudioFilesPaged(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);
    Sort.Direction sortDirection =
        direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
    Page<AudioFileDto> audioFilePage = audioFileService.getUserAudioFiles(user, pageable);

    return ResponseEntity.ok(audioFilePage);
  }

  @Operation(summary = "오디오 파일 검색", description = "제목이나 아티스트로 오디오 파일을 검색합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "검색 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/search")
  public ResponseEntity<Page<AudioFileDto>> searchAudioFiles(
      @RequestParam String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Principal principal
  ) {
    getUserFromPrincipal(principal);

    Pageable pageable = PageRequest.of(page, size);
    Page<AudioFileDto> audioFilePage = audioFileService.searchAudioFiles(keyword, pageable);

    return ResponseEntity.ok(audioFilePage);
  }

  @Operation(summary = "오디오 파일 메타데이터 수정", description = "오디오 파일의 제목이나 아티스트 정보를 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "수정 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
  })
  @PutMapping("/{videoId}/metadata")
  public ResponseEntity<AudioFileDto> updateAudioFileMetadata(
      @PathVariable String videoId,
      @Valid @RequestBody AudioMetadataUpdateRequest request,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);

    AudioFileDto updatedFile = audioFileService.updateAudioFileMetadata(
        videoId,
        request.getTitle(),
        request.getArtist(),
        user
    );

    return ResponseEntity.ok(updatedFile);
  }

  @Operation(summary = "오디오 파일 삭제", description = "오디오 파일을 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
  })
  @DeleteMapping("/{videoId}")
  public ResponseEntity<Void> deleteAudioFile(@PathVariable String videoId, Principal principal) {
    User user = getUserFromPrincipal(principal);

    boolean deleted = audioFileService.deleteAudioFile(videoId, user);

    if (deleted) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "비디오 ID로 오디오 파일 정보 조회", description = "비디오 ID로 저장된 오디오 파일 정보를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
  })
  @GetMapping("/{videoId}")
  public ResponseEntity<AudioFileDto> getAudioFileByVideoId(@PathVariable String videoId, Principal principal) {
    getUserFromPrincipal(principal);

    var audioFile = audioFileService.getAudioFileByVideoId(videoId);

    AudioFileDto audioFileDto = AudioFileDto.builder()
        .id(audioFile.getFileSize())
        .videoId(audioFile.getVideoId())
        .title(audioFile.getTitle())
        .artist(audioFile.getArtist())
        .fileName(audioFile.getFileName())
        .fileSize(audioFile.getFileSize())
        .mimeType(audioFile.getMimeType())
        .extension(audioFile.getExtension())
        .bitrate(audioFile.getBitrate())
        .durationSeconds(audioFile.getDurationSeconds())
        .thumbnailUrl(audioFile.getThumbnailUrl())
        .downloadUrl(audioFile.getDownloadUrl())
        .uploaderId(audioFile.getUploader() != null ? audioFile.getUploader().getId() : null)
        .uploaderName(audioFile.getUploader() != null ? audioFile.getUploader().getName() : null)
        .createdAt(audioFile.getCreatedAt())
        .updatedAt(audioFile.getUpdatedAt())
        .build();

    return ResponseEntity.ok(audioFileDto);
  }

  @Operation(summary = "오디오 파일 메타데이터 추출", description = "오디오 파일의 메타데이터(키, BPM 등)를 자동으로 추출합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "메타데이터 추출 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
  })
  @PostMapping("/{videoId}/extract-metadata")
  public ResponseEntity<AudioFileDto> extractMetadata(
      @PathVariable String videoId,
      Principal principal
  ) {
    User user = getUserFromPrincipal(principal);

    AudioFileDto updatedFile = audioFileService.extractAndUpdateMetadata(videoId, user);

    return ResponseEntity.ok(updatedFile);
  }

  private User getUserFromPrincipal(Principal principal) {
    if (principal == null) {
      throw new AuthenticationException("인증되지 않은 사용자입니다.");
    }

    return userRepository.findByEmail(principal.getName())
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
  }
}
