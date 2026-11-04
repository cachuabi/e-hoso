package vn.ehoso.dossier.service;

import static vn.ehoso.dossier.domain.DossierAction.ACCEPT;
import static vn.ehoso.dossier.domain.DossierAction.APPROVE;
import static vn.ehoso.dossier.domain.DossierAction.ASSIGN;
import static vn.ehoso.dossier.domain.DossierAction.MARK_VERIFICATION;
import static vn.ehoso.dossier.domain.DossierAction.REJECT;
import static vn.ehoso.dossier.domain.DossierAction.REQUEST_SUPPLEMENT;
import static vn.ehoso.dossier.domain.DossierAction.RESUBMIT;
import static vn.ehoso.dossier.domain.DossierAction.RETURN_RESULT;
import static vn.ehoso.dossier.domain.DossierAction.SEND_BACK;
import static vn.ehoso.dossier.domain.DossierAction.SUBMIT;
import static vn.ehoso.dossier.domain.DossierAction.SUBMIT_APPROVAL;
import static vn.ehoso.dossier.domain.DossierAction.WITHDRAW;
import static vn.ehoso.dossier.domain.DossierStatus.ACCEPTED;
import static vn.ehoso.dossier.domain.DossierStatus.APPROVED;
import static vn.ehoso.dossier.domain.DossierStatus.DRAFT;
import static vn.ehoso.dossier.domain.DossierStatus.IN_PROGRESS;
import static vn.ehoso.dossier.domain.DossierStatus.NEED_SUPPLEMENT;
import static vn.ehoso.dossier.domain.DossierStatus.PENDING_APPROVAL;
import static vn.ehoso.dossier.domain.DossierStatus.REJECTED;
import static vn.ehoso.dossier.domain.DossierStatus.RETURNED;
import static vn.ehoso.dossier.domain.DossierStatus.SUBMITTED;
import static vn.ehoso.dossier.domain.DossierStatus.WITHDRAWN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.repository.UserRepository;
import vn.ehoso.audit.service.AuditWriter;
import vn.ehoso.dossier.domain.Dossier;
import vn.ehoso.dossier.domain.DossierAction;
import vn.ehoso.dossier.domain.DossierStatus;
import vn.ehoso.notification.domain.NotificationType;
import vn.ehoso.notification.service.NotificationQueue;
import vn.ehoso.security.Actor;
import vn.ehoso.security.ApiException;

/** The only place that changes a dossier's status (SRS section 5, FR-23, NFR-05). */
@Service
public class DossierLifecycle {
  private record Key(DossierStatus from, DossierAction action) {}

  private static final Map<Key, DossierStatus> ALLOWED = new HashMap<>();

  static {
    ALLOWED.put(new Key(DRAFT, SUBMIT), SUBMITTED);
    ALLOWED.put(new Key(SUBMITTED, REQUEST_SUPPLEMENT), NEED_SUPPLEMENT);
    ALLOWED.put(new Key(NEED_SUPPLEMENT, RESUBMIT), SUBMITTED);
    ALLOWED.put(new Key(SUBMITTED, WITHDRAW), WITHDRAWN);
    ALLOWED.put(new Key(NEED_SUPPLEMENT, WITHDRAW), WITHDRAWN);
    ALLOWED.put(new Key(SUBMITTED, REJECT), REJECTED);
    ALLOWED.put(new Key(SUBMITTED, ACCEPT), ACCEPTED);
    ALLOWED.put(new Key(ACCEPTED, ASSIGN), IN_PROGRESS);
    ALLOWED.put(new Key(IN_PROGRESS, SUBMIT_APPROVAL), PENDING_APPROVAL);
    ALLOWED.put(new Key(PENDING_APPROVAL, SEND_BACK), IN_PROGRESS);
    ALLOWED.put(new Key(PENDING_APPROVAL, APPROVE), APPROVED);
    ALLOWED.put(new Key(APPROVED, RETURN_RESULT), RETURNED);
    ALLOWED.put(new Key(IN_PROGRESS, MARK_VERIFICATION), IN_PROGRESS);
  }

  private static final Map<DossierAction, NotificationType> CITIZEN_EMAILS =
      Map.of(
          SUBMIT, NotificationType.SUBMITTED,
          RESUBMIT, NotificationType.SUBMITTED,
          REQUEST_SUPPLEMENT, NotificationType.NEED_SUPPLEMENT,
          REJECT, NotificationType.REJECTED,
          ACCEPT, NotificationType.ACCEPTED,
          APPROVE, NotificationType.RESULT_READY,
          RETURN_RESULT, NotificationType.RESULT_RETURNED,
          MARK_VERIFICATION, NotificationType.EXTENDED);

  private final AuditWriter audit;
  private final NotificationQueue notifications;
  private final UserRepository users;

  /** Creates the state machine. */
  public DossierLifecycle(
      AuditWriter audit, NotificationQueue notifications, UserRepository users) {
    this.audit = audit;
    this.notifications = notifications;
    this.users = users;
  }

  /** Tells whether an action is allowed from a status. */
  public static boolean isAllowed(DossierStatus from, DossierAction action) {
    return ALLOWED.containsKey(new Key(from, action));
  }

  /** Lists every allowed status and action pair, for tests and documentation. */
  public static List<Map.Entry<DossierStatus, DossierAction>> allowedPairs() {
    return ALLOWED.keySet().stream().map(k -> Map.entry(k.from(), k.action())).toList();
  }

  /** Checks without changing anything; a wrong status gives 409 with the current status. */
  public void check(Dossier dossier, DossierAction action) {
    if (!isAllowed(dossier.getStatus(), action)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "INVALID_TRANSITION",
          "Hồ sơ đang ở trạng thái không cho phép thao tác này.",
          List.of(),
          Map.of("currentStatus", dossier.getStatus().name()));
    }
  }

  /** Applies a transition, writes the log and queues the email in the caller's transaction. */
  public void apply(Dossier dossier, DossierAction action, Actor actor, String note, String detail) {
    check(dossier, action);
    DossierStatus from = dossier.getStatus();
    DossierStatus to = ALLOWED.get(new Key(from, action));
    dossier.setStatus(to);
    audit.append(dossier.getId(), action.name(), from.name(), to.name(), actor, note);
    Map<String, Object> payload = new HashMap<>();
    payload.put("code", code(dossier));
    payload.put("detail", detail == null ? "" : detail);
    NotificationType type = CITIZEN_EMAILS.get(action);
    if (type != null) {
      notify(dossier.getId(), dossier.getApplicantId(), type, payload);
    }
    if (action == SEND_BACK) {
      notify(
          dossier.getId(), dossier.getAssignedOfficerId(), NotificationType.SENT_BACK, payload);
    }
  }

  private void notify(
      UUID dossierId, UUID userId, NotificationType type, Map<String, Object> payload) {
    User user = users.findById(userId).orElseThrow();
    notifications.enqueue(dossierId, user.getEmail(), type, payload);
  }

  private String code(Dossier dossier) {
    return dossier.getDossierCode() != null ? dossier.getDossierCode() : dossier.getReferenceCode();
  }
}
