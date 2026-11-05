package vn.ehoso.notification.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.ehoso.notification.domain.Notification;

/** Access to the notification queue. */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  /** Spring Data query. */
  List<Notification> findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
      Notification.Status status, OffsetDateTime now);
}
