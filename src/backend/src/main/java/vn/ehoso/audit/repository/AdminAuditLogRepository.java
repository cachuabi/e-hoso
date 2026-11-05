package vn.ehoso.audit.repository;

import org.springframework.data.repository.Repository;
import vn.ehoso.audit.domain.AdminAuditLog;

/** Append only. */
public interface AdminAuditLogRepository extends Repository<AdminAuditLog, Long> {
  /** Spring Data query. */
  AdminAuditLog save(AdminAuditLog log);
}
