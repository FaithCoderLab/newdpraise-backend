package faithcoderlab.newdpraise.domain.conti;

import faithcoderlab.newdpraise.domain.user.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContiRepository extends JpaRepository<Conti, Long> {

  List<Conti> findByCreatorOrderByScheduledAtDesc(User creator);

  List<Conti> findByScheduledAtBetweenOrderByScheduledAtDesc(LocalDate startDate,
      LocalDate endDate);

  List<Conti> findByScheduledAtGreaterThanEqualOrderByScheduledAtAsc(LocalDate date);

  List<Conti> findByScheduledAtLessThanEqualOrderByScheduledAtDesc(LocalDate date);

  List<Conti> findByTitleContainingIgnoreCaseOrderByScheduledAtDesc(String keyword);

  @Query("SELECT c FROM Conti c WHERE c.creator = :creator AND UPPER(c.title) LIKE UPPER(CONCAT('%', :keyword, '%')) ORDER BY c.scheduledAt DESC")
  List<Conti> findByCreatorAndTitleContainingIgnoreCaseOrderByScheduledAtDesc(User creator,
      String keyword);

  List<Conti> findByScheduledAtBetweenAndTitleContainingIgnoreCaseOrderByScheduledAtDesc(
      LocalDate startDate, LocalDate endDate, String keyword
  );

  Page<Conti> findByCreator(User creator, Pageable pageable);

  Page<Conti> findAll(Pageable pageable);

  List<Conti> findByStatus(ContiStatus status);

  List<Conti> findByCreatorAndStatus(User creator, ContiStatus status);

  @Query("SELECT c FROM Conti c WHERE "
      + "(:startDate IS NULL OR c.scheduledAt >= :startDate) AND "
      + "(:endDate IS NULL OR c.scheduledAt <= :endDate) AND "
      + "(:title IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND "
      + "(:creatorId IS NULL OR c.creator.id = :creatorId) AND "
      + "(:status IS NULL OR c.status = :status)")
  Page<Conti> searchContis(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("title") String title,
      @Param("creatorId") Long creatorId,
      @Param("status") ContiStatus status,
      Pageable pageable
  );
}
