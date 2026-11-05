package vn.ehoso.catalog.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import vn.ehoso.catalog.domain.Holiday;
import vn.ehoso.catalog.repository.HolidayRepository;

/** Decides which days are working days (SDD 10.1; a make-up day counts, V-10). */
@Service
public class WorkingCalendar {
  private final HolidayRepository holidays;

  /** Creates the calendar. */
  public WorkingCalendar(HolidayRepository holidays) {
    this.holidays = holidays;
  }

  /** Tells whether a date is a working day. */
  public boolean isWorkingDay(LocalDate date) {
    return holidays
        .findById(date)
        .map(h -> Holiday.MAKEUP.equals(h.getType()))
        .orElse(date.getDayOfWeek() != DayOfWeek.SATURDAY
            && date.getDayOfWeek() != DayOfWeek.SUNDAY);
  }

  /** Returns the first working day after a date. */
  public LocalDate nextWorkingDay(LocalDate date) {
    LocalDate next = date.plusDays(1);
    while (!isWorkingDay(next)) {
      next = next.plusDays(1);
    }
    return next;
  }

  /** Returns the N-th working day after a date. */
  public LocalDate addWorkingDays(LocalDate date, int days) {
    LocalDate result = date;
    for (int i = 0; i < days; i++) {
      result = nextWorkingDay(result);
    }
    return result;
  }
}
