package vn.ehoso.dossier.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.ehoso.dossier.domain.SupplementRequest;

/** Access to supplement requests. */
public interface SupplementRequestRepository extends JpaRepository<SupplementRequest, UUID> {
  Optional<SupplementRequest> findFirstByDossierIdOrderByCreatedAtDesc(UUID dossierId);
}
