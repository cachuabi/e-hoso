package vn.ehoso.audit.repository;

import org.springframework.data.repository.Repository;
import vn.ehoso.audit.domain.AdminAuditLog;

/** Append only. */
public interface AdminAuditLogRepository extends Repository<AdminAuditLog, Long> {
  AdminAuditLog save(AdminAuditLog log);
}
