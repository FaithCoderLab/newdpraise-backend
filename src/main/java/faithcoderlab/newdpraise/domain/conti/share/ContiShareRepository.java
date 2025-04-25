package faithcoderlab.newdpraise.domain.conti.share;

import faithcoderlab.newdpraise.domain.conti.Conti;
import faithcoderlab.newdpraise.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContiShareRepository extends JpaRepository<ContiShare, Long> {

  List<ContiShare> findByUser(User user);

  List<ContiShare> findByUserAndAcceptedTrue(User user);

  List<ContiShare> findByUserAndAcceptedFalse(User user);

  List<ContiShare> findByConti(Conti conti);

  Optional<ContiShare> findByContiAndUser(Conti conti, User user);

  @Query("SELECT cs FROM ContiShare cs WHERE cs.conti.id = :contiId AND cs.user.id = :userId")
  Optional<ContiShare> findByContiIdAndUserId(@Param("contiId") Long contiId, @Param("userId") Long userId);

  @Query("SELECT cs FROM ContiShare cs WHERE cs.conti.id = :contiId AND cs.user.id = :userId AND cs.accepted = true")
  Optional<ContiShare> findAcceptedShareByContiIdAndUserId(@Param("contiId") Long contiId, @Param("userId") Long userId);

  @Query("SELECT COUNT(cs) > 0 FROM ContiShare cs WHERE cs.conti.id = :contiId AND cs.user.id = :userId AND cs.permission IN :permissions AND cs.accepted = true")
  boolean hasPermission(@Param("contiId") Long contiId, @Param("userId") Long userId, @Param("permissions") List<ContiSharePermission> permissions);

  void deleteByConti(Conti conti);

  void deleteByContiAndUser(Conti conti, User user);
}
