package faithcoderlab.newdpraise.domain.conti;

import faithcoderlab.newdpraise.domain.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContiRepository extends JpaRepository<Conti, Long> {

  List<Conti> findByCreatorOrderByScheduledAtDesc(User creator);
}
