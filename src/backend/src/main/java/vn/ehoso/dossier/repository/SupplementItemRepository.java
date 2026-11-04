package vn.ehoso.dossier.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.ehoso.dossier.domain.SupplementItem;

/** Access to supplement items. */
public interface SupplementItemRepository extends JpaRepository<SupplementItem, UUID> {
  List<SupplementItem> findByRequestId(UUID requestId);
}
