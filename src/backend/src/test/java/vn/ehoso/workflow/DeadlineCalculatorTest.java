package vn.ehoso.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.ehoso.catalog.domain.DeadlineUnit;
import vn.ehoso.catalog.domain.Holiday;
import vn.ehoso.catalog.domain.Procedure;
import vn.ehoso.catalog.repository.HolidayRepository;
import vn.ehoso.catalog.service.WorkingCalendar;
import vn.ehoso.workflow.service.DeadlineCalculator;

/** Test Plan cases CK-H01 to CK-H17 and CK-B04: acceptance criterion 2. */
@Tag("BR-02")
@Tag("BR-03")
@Tag("BR-05")
@Tag("FR-17")
@Tag("FR-20")
@Tag("NFR-10")
class DeadlineCalculatorTest {
  private DeadlineCalculator calculator;

  @BeforeEach
  void setUp() {
    HolidayRepository holidays = mock(HolidayRepository.class);
    when(holidays.findById(any())).thenReturn(Optional.empty());
    when(holidays.findById(LocalDate.parse("2027-01-01")))
        .thenReturn(Optional.of(day(Holiday.HOLIDAY)));
    when(holidays.findById(LocalDate.parse("2026-11-14")))
        .thenReturn(Optional.of(day(Holiday.MAKEUP)));
    calculator = new DeadlineCalculator(new WorkingCalendar(holidays), 15, 17);
  }

  @ParameterizedTest(name = "{0} tiep nhan {1} -> {2}")
  @CsvSource({
    "CK-H01, 2026-11-05T14:59, 2026-11-05",
    "CK-H02, 2026-11-05T15:00, 2026-11-06",
    "CK-H03, 2026-11-06T14:59, 2026-11-06",
    "CK-H04, 2026-11-06T15:00, 2026-11-09",
    "CK-H05, 2026-11-07T09:00, 2026-11-09",
    "CK-H06, 2026-11-08T14:00, 2026-11-09",
    "CK-H07, 2026-12-31T14:00, 2026-12-31",
    "CK-H08, 2026-12-31T15:00, 2027-01-04",
    "CK-H09, 2027-01-01T10:00, 2027-01-04",
    "CK-H10, 2026-11-14T10:00, 2026-11-14",
    "CK-H11, 2026-11-13T15:30, 2026-11-14"
  })
  void sameDayProcedure(String id, String accepted, String expected) {
    assertThat(calculator.dueAt(sameDay(), at(accepted)).toString())
        .isEqualTo(expected + "T17:00+07:00");
  }

  @ParameterizedTest(name = "{0} tiep nhan {1} -> {2}")
  @CsvSource({
    "CK-H12, 2026-11-04T10:00, 2026-11-09",
    "CK-H13, 2026-11-04T16:00, 2026-11-09",
    "CK-H14, 2026-11-06T10:00, 2026-11-11",
    "CK-H15, 2026-11-07T10:00, 2026-11-11",
    "CK-H16, 2026-12-30T10:00, 2027-01-05",
    "CK-H17, 2026-11-12T10:00, 2026-11-16"
  })
  void threeWorkingDayProcedure(String id, String accepted, String expected) {
    Procedure procedure = new Procedure();
    procedure.setDeadlineUnit(DeadlineUnit.WORKING_DAYS);
    procedure.setDeadlineDays(3);
    assertThat(calculator.dueAt(procedure, at(accepted)).toString())
        .isEqualTo(expected + "T17:00+07:00");
  }

  @Test
  void sameDayWithoutCutoffUsesEndOfDay() {
    Procedure procedure = sameDay();
    procedure.setApplies15hCutoff(false);
    assertThat(calculator.dueAt(procedure, at("2026-11-05T16:30")).toString())
        .isEqualTo("2026-11-05T17:00+07:00");
    assertThat(calculator.dueAt(procedure, at("2026-11-05T17:00")).toString())
        .isEqualTo("2026-11-06T17:00+07:00");
  }

  @Test
  void acceptedTimeIsReadInVietnamTimeZone() {
    OffsetDateTime utc = OffsetDateTime.parse("2026-11-05T07:59:00Z");
    assertThat(calculator.dueAt(sameDay(), utc).toString()).isEqualTo("2026-11-05T17:00+07:00");
  }

  @Test
  void verificationAddsThreeWorkingDays() {
    assertThat(calculator.extend(at("2026-11-09T17:00"), 3).toString())
        .isEqualTo("2026-11-12T17:00+07:00");
  }

  private static OffsetDateTime at(String local) {
    return OffsetDateTime.parse(local + ":00+07:00");
  }

  private static Procedure sameDay() {
    Procedure procedure = new Procedure();
    procedure.setDeadlineUnit(DeadlineUnit.SAME_DAY);
    procedure.setApplies15hCutoff(true);
    return procedure;
  }

  private static Holiday day(String type) {
    Holiday holiday = new Holiday();
    holiday.setType(type);
    return holiday;
  }
}
