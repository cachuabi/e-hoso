package vn.ehoso.notification.infra;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import vn.ehoso.notification.service.MailGateway;

/** Sends through SMTP, or writes to the log when SMTP is off (assumption A5). */
@Component
class SmtpMailGateway implements MailGateway {
  private static final Logger LOG = LoggerFactory.getLogger(SmtpMailGateway.class);
  private final JavaMailSender sender;
  private final boolean enabled;
  private final String from;

  SmtpMailGateway(
      JavaMailSender sender,
      @Value("${ehoso.mail.enabled}") boolean enabled,
      @Value("${ehoso.mail.from}") String from) {
    this.sender = sender;
    this.enabled = enabled;
    this.from = from;
  }

  @Override
  public void send(String to, String subject, String text, String html) {
    if (!enabled) {
      LOG.info("Email to {}: {} | {}", to, subject, text);
      return;
    }
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(text, html);
      sender.send(message);
    } catch (MessagingException ex) {
      throw new MailSendException("Cannot build email", ex);
    }
  }
}
