package vn.ehoso.notification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One queued email (DR-08). */
@Entity
@Table(name = "notification")
@Getter
@Setter
public class Notification {
  /** Delivery state. */
  public enum Status {
    PENDING,
    SENT,
    FAILED
  }

  @Id private UUID id = UUID.randomUUID();
  private UUID dossierId;
  private String recipientEmail;

  @Enumerated(EnumType.STRING)
  private NotificationType type;

  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> payload;

  @Enumerated(EnumType.STRING)
  private Status status = Status.PENDING;

  private int attempts;
  private OffsetDateTime nextAttemptAt;
  private String lastError;
  private OffsetDateTime createdAt;
  private OffsetDateTime sentAt;
}
