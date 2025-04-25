package faithcoderlab.newdpraise.domain.song.controller;

import faithcoderlab.newdpraise.domain.song.dto.AudioDownloadResult;
import faithcoderlab.newdpraise.domain.song.dto.YoutubeDownloadRequest;
import faithcoderlab.newdpraise.domain.song.dto.YoutubeVideoInfo;
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
import java.io.File;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/songs/youtube")
@RequiredArgsConstructor
@Tag(name = "YoutubeDownload", description = "유튜브 음원 다운로드 관련 API")
public class YoutubeDownloadController {

  private final YoutubeDownloadService youtubeDownloadService;
  private final UserRepository userRepository;

  @Operation(summary = "유튜브 URL 유효성 검사", description = "유튜브 URL의 유효성을 검사합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "유효성 검사 성공"),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 URL"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/validate")
  public ResponseEntity<Boolean> validateYoutubeUrl(@RequestParam String url, Principal principal) {
    getUserFromPrincipal(principal);
    boolean isValid = youtubeDownloadService.isValidYoutubeUrl(url);
    return ResponseEntity.ok(isValid);
  }

  @Operation(summary = "유튜브 비디오 정보 조회", description = "유튜브 URL에서 비디오 정보를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공",
          content = @Content(schema = @Schema(implementation = YoutubeVideoInfo.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 URL"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "비디오를 찾을 수 없음")
  })
  @GetMapping("/info")
  public ResponseEntity<YoutubeVideoInfo> getVideoInfo(@RequestParam String url,
      Principal principal) {
    getUserFromPrincipal(principal);
    YoutubeVideoInfo videoInfo = youtubeDownloadService.getVideoInfo(url);
    return ResponseEntity.ok(videoInfo);
  }

  @Operation(summary = "유튜브 오디오 다운로드", description = "유튜브 URL에서 오디오를 다운로드합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "다운로드 성공",
          content = @Content(schema = @Schema(implementation = AudioDownloadResult.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 URL"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "비디오를 찾을 수 없음")
  })
  @PostMapping("/download")
  public ResponseEntity<AudioDownloadResult> downloadAudio(
      @Valid @RequestBody YoutubeDownloadRequest request, Principal principal) {
    getUserFromPrincipal(principal);
    AudioDownloadResult result = youtubeDownloadService.downloadAudio(request.getUrl());
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @Operation(summary = "다운로드된 오디오 파일 목록 조회", description = "다운로드된 오디오 파일 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
  })
  @GetMapping("/files")
  public ResponseEntity<List<String>> getDownloadFiles(Principal principal) {
    getUserFromPrincipal(principal);
    List<String> files = youtubeDownloadService.getDownloadedAudioFiles();
    return ResponseEntity.ok(files);
  }

  @Operation(summary = "다운로드된 오디오 파일 스트리밍", description = "다운로드된 오디오 파일을 스트리밍합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "스트리밍 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
  })
  @GetMapping("/stream/{videoId}")
  public ResponseEntity<Resource> streamAudio(@PathVariable String videoId, Principal principal) {
    getUserFromPrincipal(principal);

    if (!youtubeDownloadService.isAudioFileExists(videoId)) {
      return ResponseEntity.notFound().build();
    }

    String downloadDir = "uploads/audio";
    File directory = new File(downloadDir);
    File[] files = directory.listFiles((dir, name) -> name.startsWith(videoId + "."));

    if (files == null || files.length == 0) {
      return ResponseEntity.notFound().build();
    }

    FileSystemResource resource = new FileSystemResource(files[0]);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(
        "audio/" + files[0].getName().substring(files[0].getName().lastIndexOf("." + 1))));

    return ResponseEntity.ok()
        .headers(headers)
        .body(resource);
  }

  @Operation(summary = "다운로드된 오디오 파일 삭제", description = "다운로드된 오디오 파일을 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
      @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
  })
  @DeleteMapping("/{videoId}")
  public ResponseEntity<Void> deleteAudioFile(@PathVariable String videoId, Principal principal) {
    getUserFromPrincipal(principal);
    boolean deleted = youtubeDownloadService.deleteAudioFile(videoId);

    if (deleted) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  private User getUserFromPrincipal(Principal principal) {
    if (principal == null) {
      throw new AuthenticationException("인증되지 않은 사용자입니다.");
    }

    return userRepository.findByEmail(principal.getName())
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
  }
}
