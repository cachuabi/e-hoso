package vn.ehoso.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** A person who can sign in (DR-01). */
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class User {
  @Id private UUID id = UUID.randomUUID();
  private String fullName;
  private String email;
  private String phone;

  @Column(columnDefinition = "bpchar")
  private String nationalId;

  private String passwordHash;

  @Enumerated(EnumType.STRING)
  private Role role;

  @Enumerated(EnumType.STRING)
  private UserStatus status = UserStatus.PENDING;

  private int failedLogins;
  private OffsetDateTime lockedUntil;
  private int failedApprovals;
  private OffsetDateTime approvalLockedUntil;
  private OffsetDateTime createdAt = OffsetDateTime.now();
}
