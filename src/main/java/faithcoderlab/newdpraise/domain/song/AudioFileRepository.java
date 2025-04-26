package faithcoderlab.newdpraise.domain.song;

import faithcoderlab.newdpraise.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {

  Optional<AudioFile> findByVideoId(String videoId);

  List<AudioFile> findByUploader(User uploader);

  Page<AudioFile> findByUploader(User uploader, Pageable pageable);

  @Query("SELECT a FROM AudioFile a WHERE a.title LIKE %:keyword% OR a.artist LIKE %:keyword%")
  Page<AudioFile> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

  boolean existsByVideoId(String videoId);

  @Query("SELECT a FROM AudioFile a WHERE a.videoId = :videoId AND a.uploader.id = :uploaderId")
  Optional<AudioFile> findByVideoIdAndUploaderId(@Param("videoId") String videoId,
      @Param("uploaderId") Long uploaderId);
}
