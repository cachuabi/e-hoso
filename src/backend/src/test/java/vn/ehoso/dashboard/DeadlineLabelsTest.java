package vn.ehoso.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.ehoso.catalog.domain.DeadlineUnit;
import vn.ehoso.dashboard.service.DeadlineLabels;

/** Test Plan cases CK-B05, CK-B06. */
@Tag("BR-06")
@Tag("FR-24")
class DeadlineLabelsTest {
  private final DeadlineLabels labels = new DeadlineLabels();

  @ParameterizedTest(name = "{0} luc {1} -> {2}")
  @CsvSource({
    "SAME_DAY, 2026-11-05T15:00:00+07:00, ON_TIME",
    "SAME_DAY, 2026-11-05T15:00:01+07:00, DUE_SOON",
    "SAME_DAY, 2026-11-05T17:00:00+07:00, DUE_SOON",
    "SAME_DAY, 2026-11-05T17:00:01+07:00, OVERDUE",
    "WORKING_DAYS, 2026-11-04T17:00:00+07:00, ON_TIME",
    "WORKING_DAYS, 2026-11-04T17:00:01+07:00, DUE_SOON"
  })
  void labelsAroundBoundaries(DeadlineUnit unit, String now, String expected) {
    OffsetDateTime due = OffsetDateTime.parse("2026-11-05T17:00:00+07:00");
    assertThat(labels.labelOf(OffsetDateTime.parse(now), due, null, unit)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"SAME_DAY"})
  void noLabelWithoutDueDateOrAfterResult(DeadlineUnit unit) {
    OffsetDateTime now = OffsetDateTime.parse("2026-11-05T15:00:00+07:00");
    assertThat(labels.labelOf(now, null, null, unit)).isNull();
    assertThat(labels.labelOf(now, now, now, unit)).isNull();
  }
}
