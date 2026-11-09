package vn.ehoso.dossier;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vn.ehoso.dossier.service.FormValidator;
import vn.ehoso.dossier.service.LookupThrottle;
import vn.ehoso.support.TestBeans;

/** Form validation (FR-07, FR-09) and lookup throttling (UC-07, CK-T02). */
class UnitChecksTest {
  private static final Map<String, Object> SCHEMA =
      Map.of(
          "fields",
          List.of(
              Map.of("key", "ten", "label", "Tên", "type", "text", "required", true),
              Map.of("key", "so", "label", "Số", "type", "number", "required", true),
              Map.of("key", "ngay", "label", "Ngày", "type", "date", "required", false),
              Map.of(
                  "key",
                  "loai",
                  "label",
                  "Loại",
                  "type",
                  "select",
                  "required",
                  false,
                  "options",
                  List.of("A", "B"))));

  @Test
  @Tag("FR-07")
  @Tag("FR-09")
  void reportsEachInvalidField() {
    var problems =
        new FormValidator()
            .validate(SCHEMA, Map.of("so", "abc", "ngay", "05/11/2026", "loai", "C"));
    assertThat(problems)
        .extracting(p -> p.field())
        .containsExactlyInAnyOrder("ten", "so", "ngay", "loai");
  }

  @Test
  @Tag("FR-07")
  void acceptsValidForm() {
    var problems =
        new FormValidator()
            .validate(SCHEMA, Map.of("ten", "A", "so", 2, "ngay", "2026-11-05", "loai", "B"));
    assertThat(problems).isEmpty();
  }

  @Test
  @Tag("FR-12")
  void blocksAddressAfterMoreThanTenFailuresForTenMinutes() {
    TestBeans.MutableClock clock = new TestBeans.MutableClock();
    LookupThrottle throttle = new LookupThrottle(clock);
    for (int i = 0; i < 10; i++) {
      throttle.recordFailure("1.2.3.4");
    }
    assertThat(throttle.isBlocked("1.2.3.4")).isFalse();
    throttle.recordFailure("1.2.3.4");
    assertThat(throttle.isBlocked("1.2.3.4")).isTrue();
    assertThat(throttle.isBlocked("5.6.7.8")).isFalse();
    clock.advance(Duration.ofMinutes(10).plusSeconds(1));
    assertThat(throttle.isBlocked("1.2.3.4")).isFalse();
  }
}
