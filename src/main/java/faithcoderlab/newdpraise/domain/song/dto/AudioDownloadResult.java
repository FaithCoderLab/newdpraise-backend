package faithcoderlab.newdpraise.domain.song.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioDownloadResult {
  private String videoId;
  private String title;
  private String artist;
  private String filePath;
  private String fileName;
  private long fileSize;
  private String mimeType;
  private String extension;
  private int bitrate;
  private long durationSeconds;
  private String thumbnailUrl;
  private String originalKey;
  private String performanceKey;
  private String bpm;
}
