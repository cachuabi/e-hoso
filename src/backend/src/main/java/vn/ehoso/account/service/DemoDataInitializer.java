package vn.ehoso.account.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.account.domain.Role;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.domain.UserStatus;
import vn.ehoso.account.repository.UserRepository;

/** Creates fake demo accounts for each role (NFR-04); enabled only for the demo. */
@Component
@ConditionalOnProperty(name = "ehoso.demo.enabled", havingValue = "true")
class DemoDataInitializer implements ApplicationRunner {
  private static final Map<String, Role> ACCOUNTS =
      Map.of(
          "admin@ehoso.local", Role.ADMIN,
          "motcua@ehoso.local", Role.CBMC,
          "tuphap@ehoso.local", Role.CCTP,
          "lanhdao@ehoso.local", Role.LEADER,
          "dan@ehoso.local", Role.CITIZEN);

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final String password;

  DemoDataInitializer(
      UserRepository users,
      PasswordEncoder encoder,
      @Value("${ehoso.demo.password}") String password) {
    this.users = users;
    this.encoder = encoder;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    ACCOUNTS.forEach(
        (email, role) -> {
          if (!users.existsByEmailIgnoreCase(email)) {
            User user = new User();
            user.setFullName("Tài khoản demo " + role.name());
            user.setEmail(email);
            user.setRole(role);
            user.setStatus(UserStatus.ACTIVE);
            user.setPasswordHash(encoder.encode(password));
            if (role == Role.CITIZEN) {
              user.setNationalId("001234561234");
              user.setPhone("0900000000");
            }
            users.save(user);
          }
        });
  }
}
