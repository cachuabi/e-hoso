package vn.ehoso.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import vn.ehoso.account.domain.Role;
import vn.ehoso.audit.service.AuditWriter;
import vn.ehoso.notification.domain.Notification;
import vn.ehoso.notification.repository.NotificationRepository;
import vn.ehoso.support.IntegrationTest;

/** Log, transactions, retry, dashboard and configuration. */
class IntegrityApiTest extends IntegrationTest {
  @Autowired private JdbcTemplate jdbc;
  @Autowired private NotificationRepository notifications;
  @MockitoSpyBean private AuditWriter audit;

  private String citizen;

  @BeforeEach
  void setUp() throws Exception {
    reset(audit);
    clock.set("2030-03-04T09:00:00+07:00");
    mail.failing = false;
    citizen = login(user(Role.CITIZEN));
  }

  @Test
  @Tag("FR-23")
  void databaseRefusesLogChanges() throws Exception {
    String id = draft(citizen, "KHAI_SINH");
    doPost(citizen, "/api/v1/dossiers/" + id + "/submit", Map.of()).andExpect(status().isOk());
    assertThatThrownBy(() -> jdbc.update("UPDATE processing_log SET note = 'x'"))
        .hasMessageContaining("FR-23");
    assertThatThrownBy(() -> jdbc.update("DELETE FROM processing_log"))
        .hasMessageContaining("FR-23");
  }

  @Test
  @Tag("NFR-05")
  void statusRollsBackWhenLogFails() throws Exception {
    String id = draft(citizen, "KHAI_SINH");
    doThrow(new IllegalStateException("log down"))
        .when(audit)
        .append(any(), any(), any(), any(), any(), any());
    try {
      doPost(citizen, "/api/v1/dossiers/" + id + "/submit", Map.of());
    } catch (Exception expected) {
      assertThat(expected).hasRootCauseMessage("log down");
    }
    reset(audit);
    assertThat(read(doGet(citizen, "/api/v1/dossiers/" + id)).at("/summary/status").asText())
        .isEqualTo("DRAFT");
  }

  @Test
  @Tag("FR-14")
  void emailIsRetriedThreeTimesThenFailsWithoutHurtingTheDossier() throws Exception {
    String id = draft(citizen, "KHAI_SINH");
    mail.failing = true;
    doPost(citizen, "/api/v1/dossiers/" + id + "/submit", Map.of()).andExpect(status().isOk());
    for (int i = 0; i < 4; i++) {
      dispatcher.dispatchPending();
      clock.advance(java.time.Duration.ofMinutes(10));
    }
    Notification last =
        notifications.findAll().stream()
            .filter(n -> id.equals(String.valueOf(n.getDossierId())))
            .findFirst()
            .orElseThrow();
    assertThat(last.getStatus()).isEqualTo(Notification.Status.FAILED);
    assertThat(last.getAttempts()).isEqualTo(4);
    mail.failing = false;
    assertThat(read(doGet(citizen, "/api/v1/dossiers/" + id)).at("/summary/status").asText())
        .isEqualTo("SUBMITTED");
  }

  @Test
  @Tag("FR-24")
  @Tag("FR-25")
  @Tag("FR-26")
  @Tag("BR-06")
  @Tag("BR-07")
  void dashboardCountsOnTimeAndLateResults() throws Exception {
    String leader = login(user(Role.LEADER));
    String cbmc = login(user(Role.CBMC));
    var officer = user(Role.CCTP);
    String cctp = login(officer);
    String onTime = processToPending(cbmc, cctp, officer.getId().toString());
    String late = processToPending(cbmc, cctp, officer.getId().toString());
    approve(leader, onTime);
    JsonNode queue =
        read(doGet(cctp, "/api/v1/staff/dossiers?status=PENDING_APPROVAL&deadlineLabel=ON_TIME"));
    assertThat(queue.get("total").asInt()).isEqualTo(1);
    clock.set("2030-03-05T09:00:00+07:00");
    approve(leader, late);

    JsonNode data = read(doGet(leader, "/api/v1/dashboard?from=2030-03-04&to=2030-03-05"));
    assertThat(data.at("/byStatus/APPROVED").asInt()).isEqualTo(2);
    JsonNode stat = data.at("/byProcedure/0");
    assertThat(stat.get("onTime").asInt()).isEqualTo(1);
    assertThat(stat.get("late").asInt()).isEqualTo(1);
    assertThat(stat.get("onTimeRate").asDouble()).isEqualTo(0.5);

    String csv =
        doGet(leader, "/api/v1/dashboard/export.csv?from=2030-03-04&to=2030-03-05")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(csv).contains("Đăng ký khai sinh").contains("50.0%");
    doGet(leader, "/api/v1/dashboard?from=2040-01-01&to=2040-01-02")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.byProcedure.length()").value(0));
  }

  @Test
  @Tag("FR-06")
  @Tag("FR-27")
  @Tag("FR-28")
  void administratorConfiguresProcedureAndCalendar() throws Exception {
    String admin = login(user(Role.ADMIN));
    assertThat(read(doGet(null, "/api/v1/procedures"))).hasSize(3);
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/admin/procedures/XAC_NHAN_HON_NHAN")
                .header("Authorization", admin)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        Map.of(
                            "deadlineUnit", "WORKING_DAYS",
                            "deadlineDays", 3,
                            "applies15hCutoff", false))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deadlineDays").value(3));
    LocalDate day = LocalDate.parse("2035-06-01");
    doPost(admin, "/api/v1/admin/holidays", Map.of("day", day.toString(), "type", "HOLIDAY"))
        .andExpect(status().isCreated());
    assertThat(read(doGet(admin, "/api/v1/admin/holidays")).findValuesAsText("day"))
        .contains(day.toString());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                    "/api/v1/admin/holidays/" + day)
                .header("Authorization", admin))
        .andExpect(status().isNoContent());
    doPost(admin, "/api/v1/admin/holidays", Map.of("day", day.toString(), "type", "X"))
        .andExpect(status().isUnprocessableEntity());
  }

  private String processToPending(String cbmc, String cctp, String officerId) throws Exception {
    String base = "/api/v1/staff/dossiers/";
    String id = draft(citizen, "KHAI_SINH");
    doPost(citizen, "/api/v1/dossiers/" + id + "/submit", Map.of()).andExpect(status().isOk());
    doPost(cbmc, base + id + "/accept", Map.of("version", version(cbmc, base + id)))
        .andExpect(status().isOk());
    doPost(
            cbmc,
            base + id + "/assign",
            Map.of("officerId", officerId, "version", version(cbmc, base + id)))
        .andExpect(status().isOk());
    upload(cctp, id, "DRAFT", null, "d.pdf", PDF).andExpect(status().isCreated());
    doPost(
            cctp,
            base + id + "/submit-approval",
            Map.of("opinion", "Ok", "resultKind", "ISSUED", "version", version(cctp, base + id)))
        .andExpect(status().isOk());
    return id;
  }

  private void approve(String leader, String id) throws Exception {
    String base = "/api/v1/staff/dossiers/";
    doPost(
            leader,
            base + id + "/approve",
            Map.of("password", PASSWORD, "version", version(leader, base + id)))
        .andExpect(status().isOk());
    assertThat(List.of(id)).isNotEmpty();
  }
}
