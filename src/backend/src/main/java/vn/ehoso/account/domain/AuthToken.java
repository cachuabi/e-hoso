package vn.ehoso.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** A one-time or refresh token; only its SHA-256 is stored. */
@Entity
@Table(name = "auth_token")
@Getter
@Setter
public class AuthToken {
  /** What the token may be used for. */
  public enum Purpose {
    VERIFY,
    RESET,
    REFRESH
  }

  @Id private UUID id = UUID.randomUUID();
  private UUID userId;

  @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
  private Purpose purpose;

  @Column(columnDefinition = "bpchar")
  private String tokenHash;

  private OffsetDateTime expiresAt;
  private OffsetDateTime usedAt;
  private OffsetDateTime createdAt = OffsetDateTime.now();
}
