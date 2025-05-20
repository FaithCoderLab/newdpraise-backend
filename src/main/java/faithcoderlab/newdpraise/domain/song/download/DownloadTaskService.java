package faithcoderlab.newdpraise.domain.song.download;

import faithcoderlab.newdpraise.domain.song.AudioFile;
import faithcoderlab.newdpraise.domain.song.AudioFileRepository;
import faithcoderlab.newdpraise.domain.song.download.dto.DownloadTaskDto;
import faithcoderlab.newdpraise.domain.song.dto.AudioDownloadResult;
import faithcoderlab.newdpraise.domain.song.service.YoutubeDownloadService;
import faithcoderlab.newdpraise.domain.user.User;
import faithcoderlab.newdpraise.global.exception.ResourceAlreadyExistsException;
import faithcoderlab.newdpraise.global.exception.ResourceNotFoundException;
import faithcoderlab.newdpraise.global.exception.YoutubeDownloadException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadTaskService {

  private final DownloadTaskRepository downloadTaskRepository;
  private final AudioFileRepository audioFileRepository;
  private final YoutubeDownloadService youtubeDownloadService;
  private final Executor taskExecutor;

  private final ConcurrentHashMap<String, Float> activeDownloads = new ConcurrentHashMap<>();

  @Transactional
  public DownloadTask createDownloadTask(String youtubeUrl, String customTitle, String customArtist,
      User user) {
    String videoId = youtubeDownloadService.extractVideoId(youtubeUrl);
    if (videoId == null) {
      throw new YoutubeDownloadException("유효하지 않은 YouTube URL입니다: " + youtubeUrl);
    }

    if (downloadTaskRepository.existsByVideoIdAndUser(videoId, user)) {
      throw new ResourceAlreadyExistsException("이미 다운로드 중이거나 다운로드된 비디오입니다: " + videoId);
    }

    DownloadTask downloadTask = DownloadTask.builder()
        .videoId(videoId)
        .youtubeUrl(youtubeUrl)
        .title(customTitle)
        .artist(customArtist)
        .status(DownloadStatus.PENDING)
        .progress(0.0f)
        .user(user)
        .build();

    return downloadTaskRepository.save(downloadTask);
  }

  @Async
  @Transactional
  public CompletableFuture<DownloadTask> startDownload(Long taskId) {
    DownloadTask task = downloadTaskRepository.findById(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("다운로드 작업을 찾을 수 없습니다: " + taskId));

    if (task.getStatus() != DownloadStatus.PENDING) {
      throw new IllegalArgumentException("이미 처리 중이거나 완료된 다운로드입니다: " + taskId);
    }

    task.markAsInProgress();
    downloadTaskRepository.save(task);

    activeDownloads.put(task.getVideoId(), 0.0f);

    try {
      AudioDownloadResult result = youtubeDownloadService.downloadAudio(task.getYoutubeUrl());

      if (task.getTitle() != null && !task.getTitle().isBlank()) {
        result.setTitle(task.getTitle());
      }

      if (task.getArtist() != null && !task.getArtist().isBlank()) {
        result.setArtist(task.getArtist());
      }

      AudioFile audioFile = AudioFile.builder()
          .videoId(result.getVideoId())
          .title(result.getTitle())
          .artist(result.getArtist())
          .filePath(result.getFilePath())
          .fileName(result.getFileName())
          .fileSize(result.getFileSize())
          .mimeType(result.getMimeType())
          .extension(result.getExtension())
          .bitrate(result.getBitrate())
          .durationSeconds(result.getDurationSeconds())
          .thumbnailUrl(result.getThumbnailUrl())
          .originalUrl(task.getYoutubeUrl())
          .uploader(task.getUser())
          .build();

      AudioFile savedAudioFile = audioFileRepository.save(audioFile);

      task.markAsCompleted(savedAudioFile);
      DownloadTask completedTask = downloadTaskRepository.save(task);

      activeDownloads.remove(task.getVideoId());

      return CompletableFuture.completedFuture(completedTask);
    } catch (Exception e) {
      task.markAsFailed(e.getMessage());
      DownloadTask failedTask = downloadTaskRepository.save(task);

      activeDownloads.remove(task.getVideoId());

      log.error("다운로드 실패: {} - {}", taskId, e.getMessage(), e);
      return CompletableFuture.completedFuture(failedTask);
    }
  }

  public void updateDownloadProgress(String videoId, float progress) {
    activeDownloads.put(videoId, progress);

    DownloadTask task = downloadTaskRepository.findByVideoId(videoId)
        .orElse(null);

    if (task != null && task.getStatus() == DownloadStatus.IN_PROGRESS) {
      task.updateProgress(progress);
      downloadTaskRepository.save(task);
    }
  }

  @Transactional
  public DownloadTask updateDownloadTask(DownloadTask task) {
    return downloadTaskRepository.save(task);
  }

  @Transactional
  public DownloadTask cancelDownload(Long taskId, User user) {
    DownloadTask task = downloadTaskRepository.findById(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("다운로드 작업을 찾을 수 없습니다: " + taskId));

    if (!task.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("해당 다운로드 작업을 취소할 권한이 없습니다");
    }

    if (task.getStatus() == DownloadStatus.COMPLETED) {
      throw new IllegalArgumentException("이미 완료된 다운로드는 취소할 수 없습니다");
    }

    task.markAsCancelled();

    activeDownloads.remove(task.getVideoId());

    return downloadTaskRepository.save(task);
  }

  @Transactional
  public void deleteDownloadTask(Long taskId, User user) {
    DownloadTask task = downloadTaskRepository.findById(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("다운로드 작업을 찾을 수 없습니다: " + taskId));

    if (!task.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("해당 다운로드 작업을 삭제할 권한이 없습니다");
    }

    if (task.getStatus() == DownloadStatus.IN_PROGRESS) {
      throw new IllegalArgumentException("진행 중인 다운로드는 삭제할 수 없습니다. 먼저 취소하세요.");
    }

    downloadTaskRepository.delete(task);
  }

  @Transactional(readOnly = true)
  public DownloadTask getDownloadTask(Long taskId) {
    return downloadTaskRepository.findById(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("다운로드 작업을 찾을 수 없습니다: " + taskId));
  }

  @Transactional(readOnly = true)
  public List<DownloadTask> getUserDownloadTasks(User user) {
    return downloadTaskRepository.findByUser(user);
  }

  @Transactional(readOnly = true)
  public Page<DownloadTask> getUserDownloadTasks(User user, Pageable pageable) {
    return downloadTaskRepository.findByUser(user, pageable);
  }

  @Transactional(readOnly = true)
  public List<DownloadTask> getUserActiveDownloadTasks(User user) {
    List<DownloadStatus> activeStatuses = List.of(DownloadStatus.PENDING, DownloadStatus.IN_PROGRESS);
    return downloadTaskRepository.findByUserAndStatusIn(user.getId(), activeStatuses);
  }

  public DownloadTaskDto mapToDto(DownloadTask task) {
    return DownloadTaskDto.builder()
        .id(task.getId())
        .videoId(task.getVideoId())
        .youtubeUrl(task.getYoutubeUrl())
        .title(task.getTitle())
        .artist(task.getArtist())
        .status(task.getStatus())
        .progress(task.getProgress())
        .errorMessage(task.getErrorMessage())
        .userId(task.getUser() != null ? task.getUser().getId() : null)
        .userName(task.getUser().getName() != null ? task.getUser().getName() : null)
        .audioFileId(task.getAudioFile() != null ? task.getAudioFile().getId() : null)
        .createdAt(task.getCreatedAt())
        .updatedAt(task.getUpdatedAt())
        .completedAt(task.getCompletedAt())
        .build();
  }

  public List<DownloadTaskDto> mapToDtoList(List<DownloadTask> tasks) {
    return tasks.stream()
        .map(this::mapToDto)
        .collect(Collectors.toList());
  }

  public float getDownloadProgress(String videoId) {
    return activeDownloads.getOrDefault(videoId, 0.0f);
  }
}
