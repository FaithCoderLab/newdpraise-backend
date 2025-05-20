package faithcoderlab.newdpraise.domain.song.download.dto;

import faithcoderlab.newdpraise.domain.song.download.DownloadStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadTaskDto {
  private Long id;
  private String videoId;
  private String youtubeUrl;
  private String title;
  private String artist;
  private DownloadStatus status;
  private Float progress;
  private String errorMessage;
  private Long userId;
  private String userName;
  private Long audioFileId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime completedAt;
}
