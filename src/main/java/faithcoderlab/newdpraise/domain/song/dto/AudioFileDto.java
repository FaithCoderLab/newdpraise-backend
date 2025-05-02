package faithcoderlab.newdpraise.domain.song.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioFileDto {
  private Long id;
  private String videoId;
  private String title;
  private String artist;
  private String fileName;
  private Long fileSize;
  private String mimeType;
  private String extension;
  private Integer bitrate;
  private Long durationSeconds;
  private String thumbnailUrl;
  private String downloadUrl;
  private Long uploaderId;
  private String uploaderName;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
