package vn.ehoso.dossier.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** An application for a procedure (DR-04). Only DossierLifecycle changes its status. */
@Entity
@Table(name = "dossier")
@Getter
@Setter
public class Dossier {
  @Id private UUID id = UUID.randomUUID();

  @Column(columnDefinition = "bpchar")
  private String referenceCode;

  @Column(columnDefinition = "bpchar")
  private String dossierCode;

  private UUID procedureId;
  private UUID applicantId;
  private UUID assignedOfficerId;

  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> formData = new HashMap<>();

  @Enumerated(EnumType.STRING)
  private DossierStatus status = DossierStatus.DRAFT;

  private OffsetDateTime createdAt = OffsetDateTime.now();
  private OffsetDateTime submittedAt;
  private OffsetDateTime acceptedAt;
  private OffsetDateTime dueAt;
  private OffsetDateTime resultAt;
  private OffsetDateTime returnedAt;
  private boolean originalChecked;
  private boolean verificationFlag;
  private String verificationPlace;
  private int supplementCount;
  private String reviewOpinion;
  private String resultKind;
  private LocalDate resultExpiresOn;
  private String returnMethod;

  @Version private Long version;
}
