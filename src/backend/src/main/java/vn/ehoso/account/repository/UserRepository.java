package vn.ehoso.account.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.ehoso.account.domain.Role;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.domain.UserStatus;

/** Access to users. */
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByNationalId(String nationalId);

  List<User> findByRoleNot(Role role);

  List<User> findByRoleAndStatus(Role role, UserStatus status);

  long countByRoleAndStatus(Role role, UserStatus status);
}
