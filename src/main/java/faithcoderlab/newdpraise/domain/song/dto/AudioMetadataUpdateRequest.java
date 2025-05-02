package faithcoderlab.newdpraise.domain.song.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioMetadataUpdateRequest {

  @NotNull(message = "제목은 필수 입력 항목입니다.")
  private String title;

  private String artist;
}
