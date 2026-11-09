package vn.ehoso.security;

import java.util.UUID;
import vn.ehoso.account.domain.Role;

/** The authenticated caller, loaded from the database on every request (UC-15). */
public record Actor(UUID id, Role role, String fullName) {}
