package vn.ehoso.notification.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.notification.domain.Notification;
import vn.ehoso.notification.repository.NotificationRepository;

/** Sends queued emails; retries at most 3 times after the first attempt (UC-17). */
@Service
public class NotificationDispatcher {
  private static final Logger LOG = LoggerFactory.getLogger(NotificationDispatcher.class);
  private static final int MAX_RETRIES = 3;

  private final NotificationRepository repository;
  private final EmailComposer composer;
  private final MailGateway gateway;
  private final Clock clock;

  /** Creates the dispatcher. */
  public NotificationDispatcher(
      NotificationRepository repository,
      EmailComposer composer,
      MailGateway gateway,
      Clock clock) {
    this.repository = repository;
    this.composer = composer;
    this.gateway = gateway;
    this.clock = clock;
  }

  /** Sends the emails that are due; called on a schedule. */
  @Scheduled(fixedDelayString = "${ehoso.notification.delay-ms}")
  @Transactional
  public void dispatchPending() {
    OffsetDateTime now = OffsetDateTime.now(clock);
    repository
        .findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Notification.Status.PENDING, now)
        .forEach(n -> deliver(n, now));
  }

  private void deliver(Notification notification, OffsetDateTime now) {
    try {
      EmailComposer.Email email = composer.compose(notification);
      gateway.send(notification.getRecipientEmail(), email.subject(), email.text(), email.html());
      notification.setStatus(Notification.Status.SENT);
      notification.setSentAt(now);
      notification.setPayload(null);
    } catch (RuntimeException ex) {
      int attempts = notification.getAttempts() + 1;
      notification.setAttempts(attempts);
      notification.setLastError(ex.getMessage());
      if (attempts > MAX_RETRIES) {
        notification.setStatus(Notification.Status.FAILED);
        notification.setPayload(null);
        LOG.error("Email {} failed after {} retries", notification.getId(), MAX_RETRIES);
      } else {
        notification.setNextAttemptAt(now.plus(Duration.ofMinutes(attempts)));
      }
    }
  }
}
