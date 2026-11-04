package vn.ehoso.catalog.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.ehoso.catalog.domain.DossierComponent;

/** Access to dossier components. */
public interface DossierComponentRepository extends JpaRepository<DossierComponent, UUID> {
  List<DossierComponent> findByProcedureIdOrderBySortOrderAsc(UUID procedureId);
}
