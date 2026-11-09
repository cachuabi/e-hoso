package vn.ehoso.notification.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import vn.ehoso.notification.domain.Notification;

/** Builds subject and body of each email type (UC-17). */
@Component
public class EmailComposer {
  private final String publicUrl;

  /** Creates the composer. */
  public EmailComposer(@Value("${ehoso.public-url}") String publicUrl) {
    this.publicUrl = publicUrl;
  }

  /** The rendered email. */
  public record Email(String subject, String text, String html) {}

  /** Renders a notification. */
  public Email compose(Notification notification) {
    Map<String, Object> data =
        notification.getPayload() == null ? Map.of() : notification.getPayload();
    String code = String.valueOf(data.getOrDefault("code", ""));
    String detail = String.valueOf(data.getOrDefault("detail", ""));
    String token = String.valueOf(data.getOrDefault("token", ""));
    String text =
        switch (notification.getType()) {
          case VERIFY_EMAIL ->
              "Bấm liên kết sau trong 24 giờ để kích hoạt tài khoản: "
                  + publicUrl
                  + "/xac-thuc?token="
                  + token;
          case PASSWORD_RESET ->
              "Bấm liên kết sau trong 30 phút để đặt lại mật khẩu: "
                  + publicUrl
                  + "/dat-lai-mat-khau?token="
                  + token;
          case SUBMITTED -> "Hồ sơ " + code + " đã được nộp thành công.";
          case NEED_SUPPLEMENT -> "Hồ sơ " + code + " cần bổ sung: " + detail;
          case REJECTED -> "Hồ sơ " + code + " bị từ chối tiếp nhận. Lý do: " + detail;
          case ACCEPTED -> "Hồ sơ " + code + " đã được tiếp nhận. Ngày hẹn trả: " + detail;
          case RESULT_READY -> "Hồ sơ " + code + " đã có kết quả.";
          case RESULT_RETURNED -> "Hồ sơ " + code + " đã được trả kết quả.";
          case EXTENDED -> "Hồ sơ " + code + " được gia hạn do xác minh. Hạn mới: " + detail;
          case SENT_BACK -> "Hồ sơ " + code + " bị lãnh đạo trả lại. Lý do: " + detail;
        };
    String subject = "[e-HoSo] " + subjectOf(notification, code);
    return new Email(subject, text, "<p>" + HtmlUtils.htmlEscape(text) + "</p>");
  }

  private String subjectOf(Notification notification, String code) {
    return switch (notification.getType()) {
      case VERIFY_EMAIL -> "Xác thực tài khoản";
      case PASSWORD_RESET -> "Đặt lại mật khẩu";
      default -> "Cập nhật hồ sơ " + code;
    };
  }
}
