package vn.ehoso.account.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.account.domain.AuthToken;
import vn.ehoso.account.domain.Role;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.domain.UserStatus;
import vn.ehoso.account.repository.AuthTokenRepository;
import vn.ehoso.account.repository.UserRepository;
import vn.ehoso.notification.domain.NotificationType;
import vn.ehoso.notification.service.NotificationQueue;
import vn.ehoso.security.ApiException;
import vn.ehoso.security.ApiException.FieldProblem;

/** Registration, sign-in and password reset (FR-01, FR-02, FR-03, NFR-03). */
@Service
public class AuthService {
  private static final int MAX_FAILED_LOGINS = 5;
  private static final Duration LOCK = Duration.ofMinutes(15);
  private static final Duration VERIFY_TTL = Duration.ofHours(24);
  private static final Duration RESET_TTL = Duration.ofMinutes(30);
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserRepository users;
  private final AuthTokenRepository tokens;
  private final PasswordEncoder encoder;
  private final JwtEncoder jwtEncoder;
  private final NotificationQueue notifications;
  private final Clock clock;
  private final long jwtTtlMinutes;
  private final Duration refreshTtl;
  private final String dummyHash;

  /** Creates the service. */
  public AuthService(
      UserRepository users,
      AuthTokenRepository tokens,
      PasswordEncoder encoder,
      JwtEncoder jwtEncoder,
      NotificationQueue notifications,
      Clock clock,
      @Value("${ehoso.jwt.ttl-minutes}") long jwtTtlMinutes,
      @Value("${ehoso.refresh.ttl-days}") long refreshTtlDays) {
    this.users = users;
    this.tokens = tokens;
    this.encoder = encoder;
    this.jwtEncoder = jwtEncoder;
    this.notifications = notifications;
    this.clock = clock;
    this.jwtTtlMinutes = jwtTtlMinutes;
    this.refreshTtl = Duration.ofDays(refreshTtlDays);
    this.dummyHash = encoder.encode("timing-only");
  }

  /** The access token plus the raw refresh token destined for the cookie. */
  public record LoginResult(
      String accessToken, long expiresIn, Role role, String fullName, String refreshToken) {}

  /** Input of the registration form. */
  public record Registration(
      String fullName, String nationalId, String email, String phone, String password) {}

  /** Creates a pending citizen account and sends the verification link (FR-01). */
  @Transactional
  public void register(Registration form) {
    List<FieldProblem> errors = new ArrayList<>();
    if (form.nationalId() == null || !form.nationalId().matches("\\d{12}")) {
      errors.add(new FieldProblem("nationalId", "Số định danh phải gồm đúng 12 chữ số."));
    } else if (users.existsByNationalId(form.nationalId())) {
      errors.add(new FieldProblem("nationalId", "Số định danh đã tồn tại."));
    }
    if (form.email() == null || !form.email().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
      errors.add(new FieldProblem("email", "Email không đúng định dạng."));
    } else if (users.existsByEmailIgnoreCase(form.email())) {
      errors.add(new FieldProblem("email", "Email đã tồn tại."));
    }
    if (form.fullName() == null || form.fullName().isBlank()) {
      errors.add(new FieldProblem("fullName", "Họ tên là bắt buộc."));
    }
    if (form.phone() == null || !form.phone().matches("[0-9+]{8,15}")) {
      errors.add(new FieldProblem("phone", "Số điện thoại không hợp lệ."));
    }
    if (!isStrong(form.password())) {
      errors.add(new FieldProblem("password", "Mật khẩu tối thiểu 8 ký tự, gồm chữ và số."));
    }
    if (!errors.isEmpty()) {
      throw ApiException.invalidFields(errors);
    }
    User user = new User();
    user.setFullName(form.fullName().trim());
    user.setNationalId(form.nationalId());
    user.setEmail(form.email().trim().toLowerCase());
    user.setPhone(form.phone());
    user.setPasswordHash(encoder.encode(form.password()));
    user.setRole(Role.CITIZEN);
    user.setStatus(UserStatus.PENDING);
    users.save(user);
    sendVerification(user);
  }

  /** Activates an account from a link valid for 24 hours (FR-01). */
  @Transactional
  public void verifyEmail(String rawToken) {
    AuthToken token = useToken(rawToken, AuthToken.Purpose.VERIFY);
    User user = users.findById(token.getUserId()).orElseThrow();
    user.setStatus(UserStatus.ACTIVE);
  }

  /** Sends a new verification link without revealing whether the email exists. */
  @Transactional
  public void resendVerification(String email) {
    users
        .findByEmailIgnoreCase(email)
        .filter(u -> u.getStatus() == UserStatus.PENDING)
        .ifPresent(this::sendVerification);
  }

  /** Signs in; five wrong passwords lock the account for 15 minutes (FR-02). */
  @Transactional(noRollbackFor = ApiException.class)
  public LoginResult login(String email, String password) {
    User user = users.findByEmailIgnoreCase(email == null ? "" : email).orElse(null);
    if (user == null) {
      encoder.matches(password == null ? "" : password, dummyHash);
      throw badCredentials();
    }
    OffsetDateTime now = now();
    if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
      throw locked(user.getLockedUntil());
    }
    if (password == null || !encoder.matches(password, user.getPasswordHash())) {
      user.setFailedLogins(user.getFailedLogins() + 1);
      if (user.getFailedLogins() >= MAX_FAILED_LOGINS) {
        user.setFailedLogins(0);
        user.setLockedUntil(now.plus(LOCK));
        throw locked(user.getLockedUntil());
      }
      throw badCredentials();
    }
    if (user.getStatus() == UserStatus.PENDING) {
      throw new ApiException(
          HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Tài khoản chưa xác thực email.");
    }
    if (user.getStatus() == UserStatus.LOCKED) {
      throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Tài khoản đã bị khoá.");
    }
    user.setFailedLogins(0);
    user.setLockedUntil(null);
    return session(user);
  }

  /** Exchanges a refresh token for a new session; the old token is consumed (NFR-03). */
  @Transactional
  public LoginResult refresh(String rawToken) {
    AuthToken token;
    try {
      token = useToken(rawToken, AuthToken.Purpose.REFRESH);
    } catch (ApiException ex) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_REFRESH", "Phiên đã hết hạn.");
    }
    User user = users.findById(token.getUserId()).orElseThrow();
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_REFRESH", "Phiên đã hết hạn.");
    }
    return session(user);
  }

  /** Revokes a refresh token. */
  @Transactional
  public void logout(String rawToken) {
    if (rawToken != null) {
      tokens.findByTokenHash(hash(rawToken)).ifPresent(t -> t.setUsedAt(now()));
    }
  }

  /** Sends a reset link valid for 30 minutes, once (FR-03). */
  @Transactional
  public void requestPasswordReset(String email) {
    users
        .findByEmailIgnoreCase(email)
        .ifPresent(
            user -> {
              String raw = issue(user, AuthToken.Purpose.RESET, RESET_TTL);
              notifications.enqueue(
                  null, user.getEmail(), NotificationType.PASSWORD_RESET, Map.of("token", raw));
            });
  }

  /** Sets the new password from a reset link (FR-03). */
  @Transactional
  public void confirmPasswordReset(String rawToken, String newPassword) {
    if (!isStrong(newPassword)) {
      throw ApiException.invalidField("newPassword", "Mật khẩu tối thiểu 8 ký tự, gồm chữ và số.");
    }
    AuthToken token = useToken(rawToken, AuthToken.Purpose.RESET);
    User user = users.findById(token.getUserId()).orElseThrow();
    user.setPasswordHash(encoder.encode(newPassword));
    user.setFailedLogins(0);
    user.setLockedUntil(null);
    revokeRefreshTokens(user);
  }

  /** Revokes every refresh token of a user. */
  @Transactional
  public void revokeRefreshTokens(User user) {
    tokens
        .findByUserIdAndPurposeAndUsedAtIsNull(user.getId(), AuthToken.Purpose.REFRESH)
        .forEach(t -> t.setUsedAt(now()));
  }

  /** Tells whether a password meets the NFR-03 rule. */
  public static boolean isStrong(String password) {
    return password != null
        && password.length() >= 8
        && password.matches(".*\\p{L}.*")
        && password.matches(".*\\d.*");
  }

  private LoginResult session(User user) {
    Instant issued = clock.instant();
    Instant expires = issued.plus(Duration.ofMinutes(jwtTtlMinutes));
    JwtClaimsSet claims =
        JwtClaimsSet.builder().subject(user.getId().toString()).issuedAt(issued).expiresAt(expires)
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String access = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    String refresh = issue(user, AuthToken.Purpose.REFRESH, refreshTtl);
    return new LoginResult(
        access, jwtTtlMinutes * 60, user.getRole(), user.getFullName(), refresh);
  }

  private void sendVerification(User user) {
    String raw = issue(user, AuthToken.Purpose.VERIFY, VERIFY_TTL);
    notifications.enqueue(
        null, user.getEmail(), NotificationType.VERIFY_EMAIL, Map.of("token", raw));
  }

  private String issue(User user, AuthToken.Purpose purpose, Duration ttl) {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    AuthToken token = new AuthToken();
    token.setUserId(user.getId());
    token.setPurpose(purpose);
    token.setTokenHash(hash(raw));
    token.setExpiresAt(now().plus(ttl));
    tokens.save(token);
    return raw;
  }

  private AuthToken useToken(String rawToken, AuthToken.Purpose purpose) {
    AuthToken token =
        tokens
            .findByTokenHash(hash(rawToken == null ? "" : rawToken))
            .filter(t -> t.getPurpose() == purpose)
            .filter(t -> t.getUsedAt() == null)
            .filter(t -> t.getExpiresAt().isAfter(now()))
            .orElseThrow(
                () -> ApiException.invalid("BAD_TOKEN", "Liên kết không hợp lệ hoặc đã hết hạn."));
    token.setUsedAt(now());
    return token;
  }

  private OffsetDateTime now() {
    return OffsetDateTime.now(clock);
  }

  private static String hash(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static ApiException badCredentials() {
    return new ApiException(
        HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Email hoặc mật khẩu không đúng.");
  }

  private static ApiException locked(OffsetDateTime until) {
    return new ApiException(
        HttpStatus.LOCKED,
        "ACCOUNT_TEMP_LOCKED",
        "Tài khoản tạm khoá do đăng nhập sai nhiều lần.",
        List.of(),
        Map.of("lockedUntil", until.toString()));
  }
}
