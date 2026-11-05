package vn.ehoso.security;

import java.util.List;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Maps exceptions to problem responses with a stable code. */
@RestControllerAdvice
class ApiExceptionHandler {

  @ExceptionHandler(ApiException.class)
  ProblemDetail handleApi(ApiException ex) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    detail.setTitle(ex.getStatus().getReasonPhrase());
    detail.setProperty("code", ex.getCode());
    if (!ex.getErrors().isEmpty()) {
      detail.setProperty("errors", ex.getErrors());
    }
    ex.getExtra().forEach(detail::setProperty);
    return detail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    List<ApiException.FieldProblem> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new ApiException.FieldProblem(e.getField(), e.getDefaultMessage()))
            .toList();
    return handleApi(ApiException.invalidFields(errors));
  }

  @ExceptionHandler(ConcurrencyFailureException.class)
  ProblemDetail handleConcurrency(ConcurrencyFailureException ex) {
    return handleApi(
        ApiException.conflict("VERSION_CONFLICT", "Hồ sơ vừa được thay đổi, vui lòng tải lại."));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ProblemDetail handleTooLarge(MaxUploadSizeExceededException ex) {
    return handleApi(
        new ApiException(
            HttpStatus.UNPROCESSABLE_ENTITY, "FILE_TOO_LARGE", "Tệp vượt quá 10 MB."));
  }
}
