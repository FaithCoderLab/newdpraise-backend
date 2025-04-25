package faithcoderlab.newdpraise.domain.song.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeDownloadRequest {

  @NotBlank(message = "YouTube URL은 필수 입력 항목입니다.")
  private String url;

  private String customTitle;

  private String customArtist;
}
