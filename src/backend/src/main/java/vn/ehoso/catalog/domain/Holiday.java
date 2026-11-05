package vn.ehoso.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** A holiday or a make-up working day (DR-07). */
@Entity
@Table(name = "holiday")
@Getter
@Setter
public class Holiday {
  public static final String HOLIDAY = "HOLIDAY";
  public static final String MAKEUP = "MAKEUP_WORKDAY";

  @Id private LocalDate day;
  private String type;
  private String description;
}
