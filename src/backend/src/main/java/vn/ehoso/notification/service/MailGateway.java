package vn.ehoso.notification.service;

/** Port to the external SMTP service. */
public interface MailGateway {
  /** Sends an HTML email with a plain text alternative. */
  void send(String to, String subject, String text, String html);
}
