package vn.ehoso.notification.domain;

/** Why an email is sent (FR-14, UC-11, UC-12). */
public enum NotificationType {
  VERIFY_EMAIL,
  PASSWORD_RESET,
  SUBMITTED,
  NEED_SUPPLEMENT,
  REJECTED,
  ACCEPTED,
  RESULT_READY,
  RESULT_RETURNED,
  EXTENDED,
  SENT_BACK
}
