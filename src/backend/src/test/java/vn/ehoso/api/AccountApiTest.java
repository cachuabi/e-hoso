package vn.ehoso.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vn.ehoso.account.domain.Role;
import vn.ehoso.account.domain.User;
import vn.ehoso.support.IntegrationTest;

/** Accounts and sessions (CK-Q03, CK-Q05, CK-Q06, CK-C11). */
class AccountApiTest extends IntegrationTest {

  @BeforeEach
  void setUp() {
    clock.set("2026-11-05T10:00:00+07:00");
    mail.failing = false;
  }

  @Test
  @Tag("FR-01")
  @Tag("NFR-03")
  void registerVerifyThenLogin() throws Exception {
    Map<String, Object> form = registration();
    String email = (String) form.get("email");
    doPost(null, "/api/v1/auth/register", form).andExpect(status().isCreated());
    doPost(null, "/api/v1/auth/register", form).andExpect(status().isUnprocessableEntity());
    login(email, "Matkhau123").andExpect(status().isForbidden());

    String token = linkToken(email);
    doPost(null, "/api/v1/auth/verify-email", Map.of("token", token))
        .andExpect(status().isNoContent());
    doPost(null, "/api/v1/auth/verify-email", Map.of("token", token))
        .andExpect(status().isUnprocessableEntity());
    login(email, "Matkhau123").andExpect(status().isOk());
    User saved = users.findByEmailIgnoreCase(email).orElseThrow();
    assertThat(saved.getPasswordHash()).startsWith("$2");
  }

  @Test
  @Tag("FR-01")
  void verificationLinkExpiresAfterOneDay() throws Exception {
    Map<String, Object> form = registration();
    doPost(null, "/api/v1/auth/register", form).andExpect(status().isCreated());
    String token = linkToken((String) form.get("email"));
    clock.advance(Duration.ofHours(24).plusSeconds(1));
    doPost(null, "/api/v1/auth/verify-email", Map.of("token", token))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @Tag("NFR-03")
  void weakRegistrationIsRefusedFieldByField() throws Exception {
    doPost(
            null,
            "/api/v1/auth/register",
            Map.of(
                "fullName", "A",
                "nationalId", "123",
                "email", "sai",
                "phone", "09",
                "password", "abc"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.length()").value(4));
  }

  @Test
  @Tag("FR-02")
  void fiveWrongPasswordsLockForFifteenMinutes() throws Exception {
    User user = user(Role.CITIZEN);
    for (int i = 0; i < 4; i++) {
      login(user.getEmail(), "Sai12345").andExpect(status().isUnauthorized());
    }
    login(user.getEmail(), "Sai12345").andExpect(status().isLocked());
    login(user.getEmail(), PASSWORD).andExpect(status().isLocked());
    clock.advance(Duration.ofMinutes(15).plusSeconds(1));
    login(user.getEmail(), PASSWORD).andExpect(status().isOk());
    login("khong-co@test.local", PASSWORD).andExpect(status().isUnauthorized());
  }

  @Test
  @Tag("FR-03")
  void passwordResetLinkWorksOnce() throws Exception {
    User user = user(Role.CITIZEN);
    doPost(null, "/api/v1/auth/password-reset", Map.of("email", user.getEmail()))
        .andExpect(status().isAccepted());
    doPost(null, "/api/v1/auth/password-reset", Map.of("email", "khong-co@test.local"))
        .andExpect(status().isAccepted());
    String token = linkToken(user.getEmail());
    Map<String, String> body = Map.of("token", token, "newPassword", "MatkhauMoi1");
    doPost(null, "/api/v1/auth/password-reset/confirm", body).andExpect(status().isNoContent());
    doPost(null, "/api/v1/auth/password-reset/confirm", body)
        .andExpect(status().isUnprocessableEntity());
    login(user.getEmail(), "MatkhauMoi1").andExpect(status().isOk());
  }

  @Test
  @Tag("NFR-03")
  void refreshTokenRotates() throws Exception {
    User user = user(Role.CITIZEN);
    var cookie =
        login(user.getEmail(), PASSWORD).andReturn().getResponse().getCookie("ehoso_refresh");
    assertThat(cookie).isNotNull();
    assertThat(cookie.isHttpOnly()).isTrue();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/v1/auth/refresh")
                .cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/v1/auth/refresh")
                .cookie(cookie))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Tag("FR-04")
  @Tag("FR-05")
  void administratorManagesStaffAndChangesApplyImmediately() throws Exception {
    User adminUser = user(Role.ADMIN);
    String admin = login(adminUser);
    String email = UUID.randomUUID() + "@test.local";
    String id =
        read(
                doPost(
                        admin,
                        "/api/v1/admin/users",
                        Map.of(
                            "fullName", "Cán bộ",
                            "email", email,
                            "role", "CBMC",
                            "password", PASSWORD))
                    .andExpect(status().isCreated()))
            .get("id")
            .asText();
    doPost(
            admin,
            "/api/v1/admin/users",
            Map.of("fullName", "B", "email", email, "role", "CBMC", "password", PASSWORD))
        .andExpect(status().isUnprocessableEntity());

    String staff =
        "Bearer " + read(login(email, PASSWORD)).get("accessToken").asText();
    doGet(staff, "/api/v1/staff/officers").andExpect(status().isOk());

    patch(admin, id, Map.of("role", "LEADER")).andExpect(status().isOk());
    doGet(staff, "/api/v1/staff/officers").andExpect(status().isForbidden());

    patch(admin, id, Map.of("status", "LOCKED")).andExpect(status().isOk());
    doGet(staff, "/api/v1/dashboard?from=2026-11-01&to=2026-11-30")
        .andExpect(status().isUnauthorized());

    patch(admin, adminUser.getId().toString(), Map.of("status", "LOCKED"))
        .andExpect(status().isConflict());
    assertThat(read(doGet(admin, "/api/v1/admin/users")).findValuesAsText("email"))
        .contains(email)
        .doesNotContainNull();
  }

  @Test
  @Tag("FR-05")
  void matrixOfRolesIsEnforced() throws Exception {
    Map<Role, String> tokens = new HashMap<>();
    for (Role role : Role.values()) {
      tokens.put(role, login(user(role)));
    }
    String anyDossier = "/api/v1/staff/dossiers/" + UUID.randomUUID();
    List<Role> staff = List.of(Role.CBMC, Role.CCTP, Role.LEADER);
    for (Role role : Role.values()) {
      expectGet(tokens.get(role), "/api/v1/dossiers", role == Role.CITIZEN);
      expectGet(tokens.get(role), "/api/v1/staff/dossiers", staff.contains(role));
      expectGet(tokens.get(role), "/api/v1/staff/officers", role == Role.CBMC);
      expectGet(
          tokens.get(role), "/api/v1/dashboard?from=2026-11-01&to=2026-11-02", role == Role.LEADER);
      expectGet(tokens.get(role), "/api/v1/admin/users", role == Role.ADMIN);
      expectPostForbiddenUnless(tokens.get(role), anyDossier + "/accept", role == Role.CBMC);
      expectPostForbiddenUnless(tokens.get(role), anyDossier + "/approve", role == Role.LEADER);
      expectPostForbiddenUnless(
          tokens.get(role), anyDossier + "/submit-approval", role == Role.CCTP);
    }
    doGet(null, "/api/v1/dossiers").andExpect(status().isUnauthorized());
    doGet(null, "/api/v1/procedures").andExpect(status().isOk());
  }

  private void expectGet(String token, String url, boolean allowed) throws Exception {
    int code = doGet(token, url).andReturn().getResponse().getStatus();
    if (allowed) {
      assertThat(code).as(url).isEqualTo(200);
    } else {
      assertThat(code).as(url).isEqualTo(403);
    }
  }

  private void expectPostForbiddenUnless(String token, String url, boolean allowed)
      throws Exception {
    int code =
        doPost(token, url, Map.of("version", 0, "password", "x"))
            .andReturn()
            .getResponse()
            .getStatus();
    if (allowed) {
      assertThat(code).as(url).isEqualTo(404);
    } else {
      assertThat(code).as(url).isEqualTo(403);
    }
  }

  private org.springframework.test.web.servlet.ResultActions patch(
      String token, String id, Map<String, String> body) throws Exception {
    return mvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/v1/admin/users/" + id)
            .header("Authorization", token)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(toJson(body)));
  }

  private org.springframework.test.web.servlet.ResultActions login(String email, String password)
      throws Exception {
    return doPost(null, "/api/v1/auth/login", Map.of("email", email, "password", password));
  }

  private Map<String, Object> registration() {
    long digits = Math.abs(UUID.randomUUID().getMostSignificantBits()) % 1_000_000_000_000L;
    return Map.of(
        "fullName", "Nguyễn Văn A",
        "nationalId", String.format("%012d", digits),
        "email", UUID.randomUUID() + "@test.local",
        "phone", "0900000000",
        "password", "Matkhau123");
  }

  private String linkToken(String email) {
    dispatcher.dispatchPending();
    int index = mail.sentTo.lastIndexOf(email);
    Matcher matcher = Pattern.compile("token=(\\S+)").matcher(mail.texts.get(index));
    assertThat(matcher.find()).isTrue();
    return matcher.group(1);
  }
}
