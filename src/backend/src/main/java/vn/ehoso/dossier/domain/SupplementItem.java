package vn.ehoso.dossier.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** One missing component of a supplement request. */
@Entity
@Table(name = "supplement_item")
@Getter
@Setter
public class SupplementItem {
  @Id private UUID id = UUID.randomUUID();
  private UUID requestId;
  private UUID componentId;
  private String note;
}
