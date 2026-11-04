package vn.ehoso.security;

import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.domain.UserStatus;
import vn.ehoso.account.repository.UserRepository;

/** Loads role and status from the database, so changes apply on the next call (UC-15). */
@Component
class ActorAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
  private final UserRepository users;

  ActorAuthenticationConverter(UserRepository users) {
    this.users = users;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    User user =
        users
            .findById(UUID.fromString(jwt.getSubject()))
            .filter(u -> u.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(() -> new BadCredentialsException("Tài khoản không hoạt động"));
    Actor actor = new Actor(user.getId(), user.getRole(), user.getFullName());
    return new ActorToken(actor);
  }

  private static final class ActorToken extends AbstractAuthenticationToken {
    private static final long serialVersionUID = 1L;
    private final transient Actor actor;

    ActorToken(Actor actor) {
      super(List.of(new SimpleGrantedAuthority("ROLE_" + actor.role().name())));
      this.actor = actor;
      setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
      return "";
    }

    @Override
    public Object getPrincipal() {
      return actor;
    }
  }
}
