package vn.ehoso.account.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.ehoso.account.domain.AuthToken;

/** Access to tokens. */
public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {
  Optional<AuthToken> findByTokenHash(String tokenHash);

  List<AuthToken> findByUserIdAndPurposeAndUsedAtIsNull(UUID userId, AuthToken.Purpose purpose);
}
