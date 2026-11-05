package vn.ehoso.audit.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.ehoso.audit.domain.ProcessingLog;
import vn.ehoso.audit.repository.ProcessingLogRepository;
import vn.ehoso.security.Actor;

/** Writes the processing log inside the caller's transaction (FR-23, NFR-05). */
@Service
public class AuditWriter {
  private final ProcessingLogRepository logs;
  private final Clock clock;

  /** Creates the writer. */
  public AuditWriter(ProcessingLogRepository logs, Clock clock) {
    this.logs = logs;
    this.clock = clock;
  }

  /** Appends one line. */
  public void append(
      UUID dossierId, String action, String from, String to, Actor actor, String note) {
    ProcessingLog log = new ProcessingLog();
    log.setDossierId(dossierId);
    log.setActorId(actor.id());
    log.setActorRole(actor.role().name());
    log.setAction(action);
    log.setFromStatus(from);
    log.setToStatus(to);
    log.setNote(note);
    log.setOccurredAt(OffsetDateTime.now(clock));
    logs.save(log);
  }

  /** Reads the log of one dossier, oldest first. */
  public List<ProcessingLog> read(UUID dossierId) {
    return logs.findByDossierIdOrderByOccurredAtAscIdAsc(dossierId);
  }
}
