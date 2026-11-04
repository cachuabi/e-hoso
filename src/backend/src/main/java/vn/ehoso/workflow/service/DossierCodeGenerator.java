package vn.ehoso.workflow.service;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** Issues HS-YYMMDD-NNNN inside the acceptance transaction (BR-01). */
@Component
public class DossierCodeGenerator {
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd");
  private final EntityManager entityManager;

  /** Creates the generator. */
  public DossierCodeGenerator(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** Returns the next code of a day; a rollback does not consume a number. */
  public String next(LocalDate day) {
    Number sequence =
        (Number)
            entityManager
                .createNativeQuery(
                    "INSERT INTO dossier_code_counter (code_date, last_seq) VALUES (:day, 1) "
                        + "ON CONFLICT (code_date) DO UPDATE "
                        + "SET last_seq = dossier_code_counter.last_seq + 1 RETURNING last_seq")
                .setParameter("day", day)
                .getSingleResult();
    return String.format("HS-%s-%04d", day.format(DAY), sequence.intValue());
  }
}
