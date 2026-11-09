package vn.ehoso.audit.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.ehoso.audit.domain.AdminAuditLog;
import vn.ehoso.audit.repository.AdminAuditLogRepository;

/** Records administrator actions (UC-15). */
@Service
public class AdminAudit {
  private final AdminAuditLogRepository logs;
  private final Clock clock;

  /** Creates the recorder. */
  public AdminAudit(AdminAuditLogRepository logs, Clock clock) {
    this.logs = logs;
    this.clock = clock;
  }

  /** Appends one line. */
  public void record(UUID actorId, String action, UUID target, String note) {
    AdminAuditLog log = new AdminAuditLog();
    log.setActorId(actorId);
    log.setAction(action);
    log.setTargetUserId(target);
    log.setNote(note);
    log.setOccurredAt(OffsetDateTime.now(clock));
    logs.save(log);
  }
}
