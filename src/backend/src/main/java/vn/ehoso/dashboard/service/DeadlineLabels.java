package vn.ehoso.dashboard.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import vn.ehoso.catalog.domain.DeadlineUnit;

/** Deadline labels (BR-06); no label before a due date exists or after a result (V-07). */
@Component
public class DeadlineLabels {
  public static final String ON_TIME = "ON_TIME";
  public static final String DUE_SOON = "DUE_SOON";
  public static final String OVERDUE = "OVERDUE";

  /** Returns the label, or null when none applies. */
  public String labelOf(
      OffsetDateTime now, OffsetDateTime dueAt, OffsetDateTime resultAt, DeadlineUnit unit) {
    if (dueAt == null || resultAt != null) {
      return null;
    }
    if (now.isAfter(dueAt)) {
      return OVERDUE;
    }
    Duration soon = unit == DeadlineUnit.SAME_DAY ? Duration.ofHours(2) : Duration.ofHours(24);
    return Duration.between(now, dueAt).compareTo(soon) < 0 ? DUE_SOON : ON_TIME;
  }
}
