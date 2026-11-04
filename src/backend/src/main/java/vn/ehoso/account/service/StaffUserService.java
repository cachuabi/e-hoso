package vn.ehoso.account.service;

import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.account.domain.Role;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.domain.UserStatus;
import vn.ehoso.account.repository.UserRepository;
import vn.ehoso.audit.service.AdminAudit;
import vn.ehoso.security.Actor;
import vn.ehoso.security.ApiException;

/** Staff accounts managed by the administrator (FR-04, UC-15). */
@Service
public class StaffUserService {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final AuthService auth;
  private final AdminAudit audit;

  /** Creates the service. */
  public StaffUserService(
      UserRepository users, PasswordEncoder encoder, AuthService auth, AdminAudit audit) {
    this.users = users;
    this.encoder = encoder;
    this.auth = auth;
    this.audit = audit;
  }

  /** Lists every account except citizens. */
  @Transactional(readOnly = true)
  public List<User> list() {
    return users.findByRoleNot(Role.CITIZEN);
  }

  /** Creates an active staff account. */
  @Transactional
  public User create(String fullName, String email, Role role, String password, Actor actor) {
    if (role == Role.CITIZEN) {
      throw ApiException.invalidField("role", "Vai trò không hợp lệ.");
    }
    if (users.existsByEmailIgnoreCase(email)) {
      throw ApiException.invalidField("email", "Email đã tồn tại.");
    }
    if (!AuthService.isStrong(password)) {
      throw ApiException.invalidField("password", "Mật khẩu tối thiểu 8 ký tự, gồm chữ và số.");
    }
    User user = new User();
    user.setFullName(fullName);
    user.setEmail(email.trim().toLowerCase());
    user.setRole(role);
    user.setStatus(UserStatus.ACTIVE);
    user.setPasswordHash(encoder.encode(password));
    users.save(user);
    audit.record(actor.id(), "CREATE_STAFF", user.getId(), role.name());
    return user;
  }

  /** Locks, unlocks or changes the role; takes effect on the next call (UC-15). */
  @Transactional
  public User update(UUID id, Role role, UserStatus status, Actor actor) {
    User user = users.findById(id).orElseThrow(() -> ApiException.notFound("Không có tài khoản."));
    if (user.getRole() == Role.CITIZEN) {
      throw ApiException.forbidden();
    }
    boolean losesAdmin =
        user.getRole() == Role.ADMIN
            && ((role != null && role != Role.ADMIN) || status == UserStatus.LOCKED);
    if (losesAdmin && id.equals(actor.id())) {
      throw ApiException.conflict("SELF_LOCK", "Không thể tự khoá hoặc tự hạ quyền.");
    }
    if (losesAdmin && users.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE) <= 1) {
      throw ApiException.conflict("LAST_ADMIN", "Không thể khoá quản trị viên cuối cùng.");
    }
    if (role != null) {
      if (role == Role.CITIZEN) {
        throw ApiException.invalidField("role", "Vai trò không hợp lệ.");
      }
      user.setRole(role);
      audit.record(actor.id(), "CHANGE_ROLE", id, role.name());
    }
    if (status != null) {
      user.setStatus(status);
      audit.record(actor.id(), "CHANGE_STATUS", id, status.name());
      if (status == UserStatus.LOCKED) {
        auth.revokeRefreshTokens(user);
      }
    }
    return user;
  }
}
