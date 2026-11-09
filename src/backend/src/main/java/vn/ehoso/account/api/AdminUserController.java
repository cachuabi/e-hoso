package vn.ehoso.account.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ehoso.account.domain.Role;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.domain.UserStatus;
import vn.ehoso.account.service.StaffUserService;
import vn.ehoso.security.Actor;

/** Staff account administration. */
@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController {
  private final StaffUserService service;

  AdminUserController(StaffUserService service) {
    this.service = service;
  }

  record StaffUser(UUID id, String fullName, String email, Role role, UserStatus status) {
    static StaffUser of(User u) {
      return new StaffUser(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getStatus());
    }
  }

  record CreateRequest(
      @NotBlank String fullName,
      @NotBlank String email,
      @NotNull Role role,
      @NotBlank String password) {}

  record UpdateRequest(Role role, UserStatus status) {}

  @GetMapping
  List<StaffUser> list() {
    return service.list().stream().map(StaffUser::of).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  StaffUser create(
      @Valid @RequestBody CreateRequest body, @AuthenticationPrincipal Actor actor) {
    return StaffUser.of(
        service.create(body.fullName(), body.email(), body.role(), body.password(), actor));
  }

  @PatchMapping("/{id}")
  StaffUser update(
      @PathVariable UUID id,
      @RequestBody UpdateRequest body,
      @AuthenticationPrincipal Actor actor) {
    return StaffUser.of(service.update(id, body.role(), body.status(), actor));
  }
}
