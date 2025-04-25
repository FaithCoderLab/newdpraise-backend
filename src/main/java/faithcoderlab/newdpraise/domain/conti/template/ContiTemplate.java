package faithcoderlab.newdpraise.domain.conti.template;

import faithcoderlab.newdpraise.domain.conti.Conti;
import faithcoderlab.newdpraise.domain.song.Song;
import faithcoderlab.newdpraise.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "conti_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContiTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_id")
  private User creator;

  @ManyToMany
  @JoinTable(
      name = "conti_template_songs",
      joinColumns = @JoinColumn(name = "template_id"),
      inverseJoinColumns = @JoinColumn(name = "song_id")
  )
  @OrderColumn(name = "position")
  private List<Song> songs = new ArrayList<>();

  @Column(nullable = false)
  private boolean isPublic;
  @Column(name = "usage_count", nullable = false)
  private Integer usageCount;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (usageCount == null) {
      usageCount = 0;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public Conti toConti(User creator) {
    Conti conti = Conti.builder()
        .title(this.name)
        .description("템플릿에서 생성됨: " + this.name)
        .creator(creator)
        .songs(new ArrayList<>(this.songs))
        .version("1.0")
        .build();

    this.usageCount++;

    return conti;
  }

  public static ContiTemplate fromConti(Conti conti, String templateName, boolean isPublic) {
    return ContiTemplate.builder()
        .name(templateName)
        .description("콘티에서 생성됨: " + conti.getTitle())
        .creator(conti.getCreator())
        .songs(new ArrayList<>(conti.getSongs()))
        .isPublic(isPublic)
        .usageCount(0)
        .build();
  }
}
