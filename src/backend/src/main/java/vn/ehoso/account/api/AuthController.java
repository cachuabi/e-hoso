package vn.ehoso.account.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ehoso.account.domain.Role;
import vn.ehoso.account.service.AuthService;

/** Authentication endpoints. */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
  private static final String COOKIE = "ehoso_refresh";
  private final AuthService auth;
  private final boolean secureCookie;
  private final long refreshDays;

  AuthController(
      AuthService auth,
      @Value("${ehoso.cookie.secure}") boolean secureCookie,
      @Value("${ehoso.refresh.ttl-days}") long refreshDays) {
    this.auth = auth;
    this.secureCookie = secureCookie;
    this.refreshDays = refreshDays;
  }

  record TokenRequest(@NotBlank String token) {}

  record EmailRequest(@NotBlank String email) {}

  record LoginRequest(@NotBlank String email, @NotBlank String password) {}

  record ResetConfirm(@NotBlank String token, @NotBlank String newPassword) {}

  record Session(String accessToken, long expiresIn, Role role, String fullName) {}

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  void register(@RequestBody AuthService.Registration form) {
    auth.register(form);
  }

  @PostMapping("/verify-email")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void verify(@Valid @RequestBody TokenRequest body) {
    auth.verifyEmail(body.token());
  }

  @PostMapping("/resend-verification")
  @ResponseStatus(HttpStatus.ACCEPTED)
  void resend(@Valid @RequestBody EmailRequest body) {
    auth.resendVerification(body.email());
  }

  @PostMapping("/login")
  ResponseEntity<Session> login(@Valid @RequestBody LoginRequest body) {
    return withCookie(auth.login(body.email(), body.password()));
  }

  @PostMapping("/refresh")
  ResponseEntity<Session> refresh(@CookieValue(name = COOKIE, required = false) String cookie) {
    return withCookie(auth.refresh(cookie));
  }

  @PostMapping("/logout")
  ResponseEntity<Void> logout(@CookieValue(name = COOKIE, required = false) String cookie) {
    auth.logout(cookie);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
        .build();
  }

  @PostMapping("/password-reset")
  @ResponseStatus(HttpStatus.ACCEPTED)
  void requestReset(@Valid @RequestBody EmailRequest body) {
    auth.requestPasswordReset(body.email());
  }

  @PostMapping("/password-reset/confirm")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void confirmReset(@Valid @RequestBody ResetConfirm body) {
    auth.confirmPasswordReset(body.token(), body.newPassword());
  }

  private ResponseEntity<Session> withCookie(AuthService.LoginResult result) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            cookie(result.refreshToken(), Duration.ofDays(refreshDays)).toString())
        .body(
            new Session(
                result.accessToken(), result.expiresIn(), result.role(), result.fullName()));
  }

  private ResponseCookie cookie(String value, Duration maxAge) {
    return ResponseCookie.from(COOKIE, value)
        .httpOnly(true)
        .secure(secureCookie)
        .sameSite("Strict")
        .path("/api/v1/auth")
        .maxAge(maxAge)
        .build();
  }
}
