package vn.ehoso.catalog.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.ehoso.catalog.domain.Procedure;

/** Access to procedures. */
public interface ProcedureRepository extends JpaRepository<Procedure, UUID> {
  /** Spring Data query. */
  Optional<Procedure> findByCode(String code);
}
