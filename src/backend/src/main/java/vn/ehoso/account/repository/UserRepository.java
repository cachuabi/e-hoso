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
  /** Spring Data query. */
  Optional<User> findByEmailIgnoreCase(String email);

  /** Spring Data query. */
  boolean existsByEmailIgnoreCase(String email);

  /** Spring Data query. */
  boolean existsByNationalId(String nationalId);

  /** Spring Data query. */
  List<User> findByRoleNot(Role role);

  /** Spring Data query. */
  List<User> findByRoleAndStatus(Role role, UserStatus status);

  /** Spring Data query. */
  long countByRoleAndStatus(Role role, UserStatus status);
}
