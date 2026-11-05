package vn.ehoso.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** One document type of a procedure (DR-03). */
@Entity
@Table(name = "dossier_component")
@Getter
@Setter
public class DossierComponent {
  @Id private UUID id = UUID.randomUUID();
  private UUID procedureId;
  private String name;
  private String requirement;
  private String allowedTypes = "application/pdf,image/jpeg,image/png";
  private int sortOrder;
}
