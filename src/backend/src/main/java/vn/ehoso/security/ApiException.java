package vn.ehoso.security;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** A business error translated to an RFC 9457 problem response. */
@Getter
public class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final String code;
  private final List<FieldProblem> errors;
  private final Map<String, Object> extra;

  /** One invalid input field. */
  public record FieldProblem(String field, String message) {}

  /** Creates an error without details. */
  public ApiException(HttpStatus status, String code, String message) {
    this(status, code, message, List.of(), Map.of());
  }

  /** Creates an error with field errors and extra properties. */
  public ApiException(
      HttpStatus status,
      String code,
      String message,
      List<FieldProblem> errors,
      Map<String, Object> extra) {
    super(message);
    this.status = status;
    this.code = code;
    this.errors = errors;
    this.extra = extra;
  }

  /** Builds a 404 error. */
  public static ApiException notFound(String message) {
    return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
  }

  /** Builds a 403 error. */
  public static ApiException forbidden() {
    return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện.");
  }

  /** Builds a 422 error. */
  public static ApiException invalid(String code, String message) {
    return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
  }

  /** Builds a 422 error for one field. */
  public static ApiException invalidField(String field, String message) {
    return invalidFields(List.of(new FieldProblem(field, message)));
  }

  /** Builds a 422 error for several fields. */
  public static ApiException invalidFields(List<FieldProblem> errors) {
    return new ApiException(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "VALIDATION",
        "Dữ liệu không hợp lệ.",
        errors,
        Map.of());
  }

  /** Builds a 409 error. */
  public static ApiException conflict(String code, String message) {
    return new ApiException(HttpStatus.CONFLICT, code, message);
  }
}
