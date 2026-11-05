package vn.ehoso.dossier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vn.ehoso.account.repository.UserRepository;
import vn.ehoso.audit.service.AuditWriter;
import vn.ehoso.dossier.domain.Dossier;
import vn.ehoso.dossier.domain.DossierAction;
import vn.ehoso.dossier.domain.DossierStatus;
import vn.ehoso.dossier.service.DossierLifecycle;
import vn.ehoso.notification.service.NotificationQueue;
import vn.ehoso.security.ApiException;

/** Test Plan case CK-V02: every pair outside the table is refused. */
@Tag("BR-04")
@Tag("FR-10")
class DossierLifecycleTest {
  private final DossierLifecycle lifecycle =
      new DossierLifecycle(
          mock(AuditWriter.class), mock(NotificationQueue.class), mock(UserRepository.class));

  @Test
  void tableHasTheThirteenPairsOfTheDesign() {
    assertThat(DossierLifecycle.allowedPairs()).hasSize(13);
    assertThat(DossierLifecycle.isAllowed(DossierStatus.DRAFT, DossierAction.WITHDRAW)).isFalse();
  }

  @Test
  void withdrawOnlyFromSubmittedOrNeedSupplement() {
    for (DossierStatus status : DossierStatus.values()) {
      boolean expected =
          status == DossierStatus.SUBMITTED || status == DossierStatus.NEED_SUPPLEMENT;
      assertThat(DossierLifecycle.isAllowed(status, DossierAction.WITHDRAW)).isEqualTo(expected);
    }
  }

  @Test
  void everyOtherPairIsConflictWithCurrentStatus() {
    int refused = 0;
    for (DossierStatus status : DossierStatus.values()) {
      for (DossierAction action : DossierAction.values()) {
        if (!DossierLifecycle.isAllowed(status, action)) {
          Dossier dossier = new Dossier();
          dossier.setStatus(status);
          assertThatThrownBy(() -> lifecycle.check(dossier, action))
              .isInstanceOfSatisfying(
                  ApiException.class,
                  ex -> {
                    assertThat(ex.getStatus().value()).isEqualTo(409);
                    assertThat(ex.getExtra()).isEqualTo(Map.of("currentStatus", status.name()));
                  });
          refused++;
        }
      }
    }
    assertThat(refused)
        .isEqualTo(DossierStatus.values().length * DossierAction.values().length - 13);
  }
}
