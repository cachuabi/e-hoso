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

/** One append-only administration log line (UC-15). */
@Entity
@Table(name = "admin_audit_log")
@Getter
@Setter
public class AdminAuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private UUID actorId;
  private String action;
  private UUID targetUserId;
  private String note;
  private OffsetDateTime occurredAt;
}
