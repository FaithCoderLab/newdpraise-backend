package faithcoderlab.newdpraise.domain.song.download;

import faithcoderlab.newdpraise.domain.song.AudioFile;
import faithcoderlab.newdpraise.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "download_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadTask {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String videoId;

  @Column(nullable = false)
  private String youtubeUrl;

  @Column
  private String title;

  @Column
  private String artist;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DownloadStatus status;

  @Column
  private Float progress;

  @Column
  private String errorMessage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "audio_file_id")
  private AudioFile audioFile;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime updatedAt;

  @Column
  private LocalDateTime completedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null) {
      status = DownloadStatus.PENDING;
    }
    if (progress == null) {
      progress = 0.0f;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
    if (status == DownloadStatus.COMPLETED && completedAt == null) {
      completedAt = LocalDateTime.now();
    }
  }

  public void markAsInProgress() {
    this.status = DownloadStatus.IN_PROGRESS;
  }

  public void updateProgress(float progress) {
    this.progress = progress;
  }

  public void markAsCompleted(AudioFile audioFile) {
    this.status = DownloadStatus.COMPLETED;
    this.audioFile = audioFile;
    this.completedAt = LocalDateTime.now();
  }

  public void markAsFailed(String errorMessage) {
    this.status = DownloadStatus.FAILED;
    this.errorMessage = errorMessage;
  }

  public void markAsCancelled() {
    this.status = DownloadStatus.CANCELLED;
  }
}
