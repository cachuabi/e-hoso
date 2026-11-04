package vn.ehoso.audit.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;
import vn.ehoso.audit.domain.ProcessingLog;

/** Read and append only: no update or delete method exists (FR-23). */
public interface ProcessingLogRepository extends Repository<ProcessingLog, Long> {
  ProcessingLog save(ProcessingLog log);

  List<ProcessingLog> findByDossierIdOrderByOccurredAtAscIdAsc(UUID dossierId);
}
