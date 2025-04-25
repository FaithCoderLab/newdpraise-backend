package faithcoderlab.newdpraise.domain.conti.template;

import faithcoderlab.newdpraise.domain.user.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContiTemplateRepository extends JpaRepository<ContiTemplate, Long> {

  List<ContiTemplate> findByCreator(User creator);

  Page<ContiTemplate> findByCreator(User creator, Pageable pageable);

  List<ContiTemplate> findByIsPublicTrue();

  Page<ContiTemplate> findByIsPublicTrue(Pageable pageable);

  List<ContiTemplate> findByNameContainingIgnoreCase(String name);

  @Query("SELECT t FROM ContiTemplate t WHERE t.creator.id = :userId OR t.isPublic = true")
  List<ContiTemplate> findAccessibleTemplates(@Param("userId") Long userId);

  @Query("SELECT t FROM ContiTemplate t WHERE t.creator.id = :userId OR t.isPublic = true")
  Page<ContiTemplate> findAccessibleTemplates(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT t FROM ContiTemplate t WHERE t.isPublic = true ORDER BY t.usageCount DESC")
  List<ContiTemplate> findPopularTemplates(Pageable pageable);
}
