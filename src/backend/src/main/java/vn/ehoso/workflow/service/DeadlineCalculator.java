package vn.ehoso.workflow.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.ehoso.catalog.domain.DeadlineUnit;
import vn.ehoso.catalog.domain.Procedure;
import vn.ehoso.catalog.service.WorkingCalendar;

/** Computes due dates (BR-02, BR-03, BR-05; SDD 10.2). */
@Service
public class DeadlineCalculator {
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final WorkingCalendar calendar;
  private final int cutoffHour;
  private final int endHour;

  /** Creates the calculator. */
  public DeadlineCalculator(
      WorkingCalendar calendar,
      @Value("${ehoso.deadline.cutoff-hour}") int cutoffHour,
      @Value("${ehoso.deadline.end-hour}") int endHour) {
    this.calendar = calendar;
    this.cutoffHour = cutoffHour;
    this.endHour = endHour;
  }

  /** Returns the due date of a dossier accepted at the given time. */
  public OffsetDateTime dueAt(Procedure procedure, OffsetDateTime acceptedAt) {
    ZonedDateTime local = acceptedAt.atZoneSameInstant(ZONE);
    LocalDate day = local.toLocalDate();
    if (procedure.getDeadlineUnit() == DeadlineUnit.WORKING_DAYS) {
      return endOf(calendar.addWorkingDays(day, procedure.getDeadlineDays()));
    }
    int limit = procedure.isApplies15hCutoff() ? cutoffHour : endHour;
    boolean today = calendar.isWorkingDay(day) && local.toLocalTime().getHour() < limit;
    return endOf(today ? day : calendar.nextWorkingDay(day));
  }

  /** Adds working days to a due date (BR-05). */
  public OffsetDateTime extend(OffsetDateTime dueAt, int days) {
    return endOf(calendar.addWorkingDays(dueAt.atZoneSameInstant(ZONE).toLocalDate(), days));
  }

  private OffsetDateTime endOf(LocalDate day) {
    return day.atTime(LocalTime.of(endHour, 0)).atZone(ZONE).toOffsetDateTime();
  }
}
