package faithcoderlab.newdpraise.domain.song;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "songs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Song {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  private String originalKey;

  private String performanceKey;

  private String artist;

  private String youtubeUrl;

  private String referenceUrl;

  private String urlType;

  private Integer durationSeconds;

  @Column(columnDefinition = "TEXT")
  private String specialInstructions;

  private String bpm;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public String getUrl() {
    if (youtubeUrl != null && !youtubeUrl.isEmpty()) {
      return youtubeUrl;
    } else {
      return referenceUrl;
    }
  }
}
