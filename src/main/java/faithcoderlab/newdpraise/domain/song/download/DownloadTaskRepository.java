package faithcoderlab.newdpraise.domain.song.download;

import faithcoderlab.newdpraise.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DownloadTaskRepository extends JpaRepository<DownloadTask, Long> {

  Optional<DownloadTask> findByVideoId(String videoId);

  List<DownloadTask> findByUser(User user);

  Page<DownloadTask> findByUser(User user, Pageable pageable);

  List<DownloadTask> findByStatus(DownloadStatus status);

  List<DownloadTask> findByUserAndStatus(User user, DownloadStatus status);

  @Query("SELECT dt from DownloadTask dt WHERE dt.user.id = :userId AND dt.status IN :statuses")
  List<DownloadTask> findByUserAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<DownloadStatus> statuses);

  @Query("SELECT dt FROM DownloadTask dt WHERE dt.status = 'IN_PROGRESS'")
  List<DownloadTask> findActiveDownloads();

  Optional<DownloadTask> findByVideoIdAndUser(String videoId, User user);

  boolean existsByVideoIdAndUser(String videoId, User user);

}
