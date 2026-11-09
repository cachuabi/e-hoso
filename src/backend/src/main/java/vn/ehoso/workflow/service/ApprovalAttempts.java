package vn.ehoso.workflow.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.repository.UserRepository;

/** Counts wrong approval passwords in its own transaction, so a rollback keeps the count. */
@Component
public class ApprovalAttempts {
  private static final int LIMIT = 3;
  private static final Duration LOCK = Duration.ofMinutes(15);
  private final UserRepository users;

  /** Creates the counter. */
  public ApprovalAttempts(UserRepository users) {
    this.users = users;
  }

  /** Records a failure; the third in a row locks approvals for 15 minutes (UC-11). */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailure(UUID userId, OffsetDateTime now) {
    User user = users.findById(userId).orElseThrow();
    int failures = user.getFailedApprovals() + 1;
    if (failures >= LIMIT) {
      user.setFailedApprovals(0);
      user.setApprovalLockedUntil(now.plus(LOCK));
    } else {
      user.setFailedApprovals(failures);
    }
  }
}
