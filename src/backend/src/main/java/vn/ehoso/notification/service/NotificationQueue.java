package vn.ehoso.notification.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.ehoso.notification.domain.Notification;
import vn.ehoso.notification.domain.NotificationType;
import vn.ehoso.notification.repository.NotificationRepository;

/** Adds emails to the queue inside the business transaction (TK-04). */
@Service
public class NotificationQueue {
  private final NotificationRepository repository;
  private final Clock clock;

  /** Creates the queue. */
  public NotificationQueue(NotificationRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Queues one email. */
  public void enqueue(
      UUID dossierId, String email, NotificationType type, Map<String, Object> payload) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Notification notification = new Notification();
    notification.setDossierId(dossierId);
    notification.setRecipientEmail(email);
    notification.setType(type);
    notification.setPayload(payload);
    notification.setCreatedAt(now);
    notification.setNextAttemptAt(now);
    repository.save(notification);
  }
}
