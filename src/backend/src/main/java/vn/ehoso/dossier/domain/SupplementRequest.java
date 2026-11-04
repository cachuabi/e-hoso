package vn.ehoso.dossier.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** What the citizen must add (FR-16, BR-09). */
@Entity
@Table(name = "supplement_request")
@Getter
@Setter
public class SupplementRequest {
  @Id private UUID id = UUID.randomUUID();
  private UUID dossierId;
  private String instruction;
  private UUID requestedBy;
  private OffsetDateTime createdAt = OffsetDateTime.now();
}
