package vn.ehoso.catalog.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.ehoso.catalog.domain.Holiday;

/** Access to the holiday calendar. */
public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {
  /** Spring Data query. */
  List<Holiday> findAllByOrderByDayAsc();
}
