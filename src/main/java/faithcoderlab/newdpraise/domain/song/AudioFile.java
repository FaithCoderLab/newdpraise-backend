package faithcoderlab.newdpraise.domain.song;

import faithcoderlab.newdpraise.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audio_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String videoId;

  @Column(nullable = false)
  private String title;

  private String artist;

  @Column(nullable = false)
  private String filePath;

  @Column(nullable = false)
  private String fileName;

  @Column(nullable = false)
  private Long fileSize;

  @Column(nullable = false)
  private String mimeType;

  private String extension;

  private Integer bitrate;

  private Long durationSeconds;

  private String thumbnailUrl;

  @Column(nullable = false)
  private String originalUrl;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User uploader;

  @Column(nullable = false, updatable = false)
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

  @Transient
  public String getDownloadUrl() {
    return "/api/songs/youtube/stream/" + videoId;
  }
}
