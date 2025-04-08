package faithcoderlab.newdpraise.domain.conti;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContiRepository extends JpaRepository<Conti, Long> {
}
