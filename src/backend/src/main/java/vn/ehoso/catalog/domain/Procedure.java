package vn.ehoso.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** An administrative procedure (DR-02). */
@Entity
@Table(name = "procedure")
@Getter
@Setter
public class Procedure {
  @Id private UUID id = UUID.randomUUID();
  private String code;
  private String name;

  @Enumerated(EnumType.STRING)
  private DeadlineUnit deadlineUnit;

  private Integer deadlineDays;

  @Column(name = "applies_15h_cutoff")
  private boolean applies15hCutoff;

  private boolean allowsVerification;
  private boolean requiresOriginalCheck;
  private Integer resultValidityMonths;
  private String documentsToPresent;

  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> formSchema;
}
