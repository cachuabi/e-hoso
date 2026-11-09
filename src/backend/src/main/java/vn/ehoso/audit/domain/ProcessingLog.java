package vn.ehoso.audit.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** One append-only log line (DR-06, FR-23). */
@Entity
@Table(name = "processing_log")
@Getter
@Setter
public class ProcessingLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private UUID dossierId;
  private UUID actorId;
  private String actorRole;
  private String action;
  private String fromStatus;
  private String toStatus;
  private String note;
  private OffsetDateTime occurredAt;
}
