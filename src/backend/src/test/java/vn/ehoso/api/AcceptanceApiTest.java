package vn.ehoso.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import vn.ehoso.account.domain.Role;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.domain.UserStatus;
import vn.ehoso.notification.domain.Notification;
import vn.ehoso.notification.domain.NotificationType;
import vn.ehoso.notification.repository.NotificationRepository;
import vn.ehoso.support.IntegrationTest;

/** Acceptance cases of the Test Plan that span several features. */
class AcceptanceApiTest extends IntegrationTest {
  private static final String STAFF = "/api/v1/staff/dossiers/";
  private static final String MINE = "/api/v1/dossiers/";

  @Autowired private NotificationRepository notifications;
  @Autowired private JdbcTemplate jdbc;

  private User citizenUser;
  private User officer;
  private User cbmcUser;
  private User leaderUser;
  private String citizen;
  private String cbmc;
  private String cctp;
  private String leader;

  @BeforeEach
  void setUp() throws Exception {
    clock.set("2031-03-03T09:00:00+07:00");
    citizenUser = user(Role.CITIZEN);
    officer = user(Role.CCTP);
    cbmcUser = user(Role.CBMC);
    leaderUser = user(Role.LEADER);
    signIn();
    mail.failing = false;
  }

  /** CK-B01. */
  @Test
  @Tag("BR-01")
  void dossierCodeCountsPerDayAndAFailedAcceptanceBurnsNoNumber() throws Exception {
    clock.set("2031-03-05T09:00:00+07:00");
    signIn();
    assertThat(acceptAndCode(submitted("KHAI_SINH"))).isEqualTo("HS-310305-0001");

    String refused = submitted("KHAI_SINH");
    doPost(cbmc, STAFF + refused + "/accept", Map.of("version", 99))
        .andExpect(status().isConflict());
    assertThat(acceptAndCode(refused)).isEqualTo("HS-310305-0002");

    clock.set("2031-03-06T09:00:00+07:00");
    signIn();
    assertThat(acceptAndCode(submitted("KHAI_SINH"))).isEqualTo("HS-310306-0001");
  }

  /** CK-B07: a result exactly at the due time is on time, one second later is late. */
  @Test
  @Tag("BR-07")
  void resultAtTheDueTimeIsOnTimeAndOneSecondLaterIsLate() throws Exception {
    String exact = pendingApproval("KHAI_SINH");
    final String late = pendingApproval("KHAI_SINH");
    String due = read(doGet(cbmc, STAFF + exact)).at("/summary/dueAt").asText();
    clock.set(due);
    signIn();
    approve(exact);
    assertThat(read(doGet(leader, STAFF + exact)).get("onTime").asBoolean()).isTrue();
    clock.advance(Duration.ofSeconds(1));
    approve(late);
    assertThat(read(doGet(leader, STAFF + late)).get("onTime").asBoolean()).isFalse();
  }

  /** CK-V01 and CK-N01: every action writes exactly one log line and the right email. */
  @Test
  @Tag("FR-14")
  @Tag("FR-23")
  @Tag("NFR-05")
  void everyActionLogsOnceAndEveryEventIsEmailed() throws Exception {
    String id = draft(citizen, "XAC_NHAN_HON_NHAN");
    final String component =
        read(doGet(null, "/api/v1/procedures/XAC_NHAN_HON_NHAN")).at("/components/0/id").asText();
    int logs = 0;

    doPost(citizen, MINE + id + "/submit", Map.of()).andExpect(status().isOk());
    logs = expectOneMoreLog(id, logs);
    requestSupplement(id, component);
    logs = expectOneMoreLog(id, logs);
    doPost(citizen, MINE + id + "/resubmit", Map.of()).andExpect(status().isOk());
    logs = expectOneMoreLog(id, logs);
    accept(id);
    logs = expectOneMoreLog(id, logs);
    assign(id);
    logs = expectOneMoreLog(id, logs);
    doPost(
            cctp,
            STAFF + id + "/verification",
            Map.of("place", "UBND xã B", "version", version(cctp, STAFF + id)))
        .andExpect(status().isOk());
    logs = expectOneMoreLog(id, logs);
    upload(cctp, id, "DRAFT", null, "du-thao.pdf", PDF).andExpect(status().isCreated());
    submitForApproval(id);
    logs = expectOneMoreLog(id, logs);
    doPost(
            leader,
            STAFF + id + "/send-back",
            Map.of("reason", "Sửa ý kiến", "version", version(leader, STAFF + id)))
        .andExpect(status().isOk());
    logs = expectOneMoreLog(id, logs);
    submitForApproval(id);
    logs = expectOneMoreLog(id, logs);
    approve(id);
    logs = expectOneMoreLog(id, logs);
    returnResult(id, "COUNTER");
    expectOneMoreLog(id, logs);

    assertThat(typesSentTo(id, citizenUser.getEmail()))
        .contains(
            NotificationType.SUBMITTED,
            NotificationType.NEED_SUPPLEMENT,
            NotificationType.ACCEPTED,
            NotificationType.EXTENDED,
            NotificationType.RESULT_READY,
            NotificationType.RESULT_RETURNED);
    assertThat(typesSentTo(id, officer.getEmail())).containsExactly(NotificationType.SENT_BACK);

    String rejected = submitted("KHAI_SINH");
    doPost(
            cbmc,
            STAFF + rejected + "/reject",
            Map.of("reason", "Sai thẩm quyền", "version", version(cbmc, STAFF + rejected)))
        .andExpect(status().isOk());
    expectOneMoreLog(rejected, 1);
    assertThat(typesSentTo(rejected, citizenUser.getEmail())).contains(NotificationType.REJECTED);

    String withdrawn = submitted("KHAI_SINH");
    doPost(citizen, MINE + withdrawn + "/withdraw", Map.of()).andExpect(status().isOk());
    expectOneMoreLog(withdrawn, 1);
  }

  /** CK-V04: the two requests race; exactly one of them wins. */
  @Test
  @Tag("BR-04")
  @Tag("FR-10")
  @Tag("FR-16")
  void acceptanceAndWithdrawalRacingLetExactlyOneWin() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      for (int round = 0; round < 5; round++) {
        String id = submitted("KHAI_SINH");
        long version = version(cbmc, STAFF + id);
        CountDownLatch start = new CountDownLatch(2);
        Future<Integer> accepted =
            pool.submit(
                race(
                    start,
                    () -> doPost(cbmc, STAFF + id + "/accept", Map.of("version", version))));
        Future<Integer> withdrawn =
            pool.submit(race(start, () -> doPost(citizen, MINE + id + "/withdraw", Map.of())));
        assertThat(List.of(accepted.get(), withdrawn.get())).containsExactlyInAnyOrder(200, 409);
        String status = read(doGet(cbmc, STAFF + id)).at("/summary/status").asText();
        assertThat(status).isEqualTo(accepted.get() == 200 ? "ACCEPTED" : "WITHDRAWN");
      }
    } finally {
      pool.shutdownNow();
    }
  }

  /** CK-C02. */
  @Test
  @Tag("FR-06")
  void catalogShowsThreeProceduresWithComponents() throws Exception {
    JsonNode procedures = read(doGet(null, "/api/v1/procedures"));
    assertThat(procedures).hasSize(3);
    assertThat(procedures.findValuesAsText("code"))
        .containsExactlyInAnyOrder("CHUNG_THUC", "KHAI_SINH", "XAC_NHAN_HON_NHAN");
    for (JsonNode procedure : procedures) {
      assertThat(procedure.get("name").asText()).isNotBlank();
      assertThat(procedure.get("deadlineUnit").asText()).isNotBlank();
      assertThat(procedure.get("components")).isNotEmpty();
    }
  }

  /** CK-C03. */
  @Test
  @Tag("FR-07")
  void draftKeepsItsDataWhenEditedAndReopened() throws Exception {
    JsonNode created =
        read(
            doPost(
                citizen,
                "/api/v1/dossiers",
                Map.of("procedureCode", "CHUNG_THUC", "formData", validForm("CHUNG_THUC"))));
    String id = created.at("/summary/id").asText();
    assertThat(created.at("/summary/status").asText()).isEqualTo("DRAFT");
    Map<String, Object> edited =
        Map.of("hoTen", "Nguyễn Văn Sửa", "tenGiayTo", "Học bạ", "soBan", 3);
    mvc.perform(
            put(MINE + id)
                .header("Authorization", citizen)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("formData", edited, "version", created.get("version")))))
        .andExpect(status().isOk());
    JsonNode reopened = read(doGet(citizen, MINE + id));
    assertThat(reopened.at("/formData/hoTen").asText()).isEqualTo("Nguyễn Văn Sửa");
    assertThat(reopened.at("/formData/soBan").asInt()).isEqualTo(3);
    assertThat(reopened.at("/summary/status").asText()).isEqualTo("DRAFT");
  }

  /** CK-C08. */
  @Test
  @Tag("FR-18")
  void assignmentShowsTheLoadAndRefusesALockedOfficer() throws Exception {
    String first = submitted("KHAI_SINH");
    accept(first);
    assign(first);
    JsonNode officers = read(doGet(cbmc, "/api/v1/staff/officers"));
    JsonNode mine = null;
    for (JsonNode row : officers) {
      if (officer.getId().toString().equals(row.get("id").asText())) {
        mine = row;
      }
    }
    assertThat(mine).isNotNull();
    assertThat(mine.get("activeDossiers").asLong()).isEqualTo(1);

    User locked = user(Role.CCTP);
    locked.setStatus(UserStatus.LOCKED);
    users.save(locked);
    String second = submitted("KHAI_SINH");
    accept(second);
    doPost(
            cbmc,
            STAFF + second + "/assign",
            Map.of(
                "officerId", locked.getId().toString(), "version", version(cbmc, STAFF + second)))
        .andExpect(status().isUnprocessableEntity());
    assertThat(read(doGet(cbmc, STAFF + second)).at("/summary/status").asText())
        .isEqualTo("ACCEPTED");
  }

  /** CK-C10. */
  @Test
  @Tag("FR-22")
  void resultIsReturnedEitherWayAndElectronicCopyIsDownloadable() throws Exception {
    String id = approved("KHAI_SINH");
    String result = resultFile(id);
    doGet(citizen, "/api/v1/attachments/" + result + "/download-url")
        .andExpect(status().isForbidden());
    returnResult(id, "ELECTRONIC");
    assertThat(read(doGet(citizen, MINE + id)).get("returnMethod").asText())
        .isEqualTo("ELECTRONIC");
    doGet(citizen, "/api/v1/attachments/" + result + "/download-url")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").isNotEmpty());

    String counter = approved("KHAI_SINH");
    returnResult(counter, "COUNTER");
    assertThat(read(doGet(cbmc, STAFF + counter)).get("returnMethod").asText())
        .isEqualTo("COUNTER");
    doGet(citizen, "/api/v1/attachments/" + resultFile(counter) + "/download-url")
        .andExpect(status().isForbidden());
  }

  /** CK-C12: a new deadline applies to later dossiers only. */
  @Test
  @Tag("FR-27")
  void newDeadlineAppliesOnlyToDossiersAcceptedAfterwards() throws Exception {
    final String admin = login(user(Role.ADMIN));
    String before = submitted("KHAI_SINH");
    accept(before);
    String dueBefore = read(doGet(cbmc, STAFF + before)).at("/summary/dueAt").asText();
    configure(admin, "WORKING_DAYS", 2, false);
    try {
      String after = submitted("KHAI_SINH");
      accept(after);
      String dueAfter = read(doGet(cbmc, STAFF + after)).at("/summary/dueAt").asText();
      assertThat(OffsetDateTime.parse(dueAfter)).isAfter(OffsetDateTime.parse(dueBefore));
      assertThat(read(doGet(cbmc, STAFF + before)).at("/summary/dueAt").asText())
          .isEqualTo(dueBefore);
    } finally {
      configure(admin, "SAME_DAY", null, true);
    }
  }

  /** CK-Z01. */
  @Test
  @Tag("NFR-10")
  void timesCarryAnOffsetInTheApiAndInTheDatabase() throws Exception {
    String id = submitted("KHAI_SINH");
    accept(id);
    assertThat(read(doGet(cbmc, STAFF + id)).at("/summary/dueAt").asText())
        .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})");
    Integer naive =
        jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'public' "
                + "AND data_type = 'timestamp without time zone' "
                + "AND table_name <> 'flyway_schema_history'",
            Integer.class);
    assertThat(naive).isZero();
  }

  private void signIn() throws Exception {
    citizen = login(citizenUser);
    cctp = login(officer);
    cbmc = login(cbmcUser);
    leader = login(leaderUser);
  }

  private String submitted(String code) throws Exception {
    String id = draft(citizen, code);
    doPost(citizen, MINE + id + "/submit", Map.of()).andExpect(status().isOk());
    return id;
  }

  private String pendingApproval(String code) throws Exception {
    String id = submitted(code);
    accept(id);
    assign(id);
    upload(cctp, id, "DRAFT", null, "du-thao.pdf", PDF).andExpect(status().isCreated());
    submitForApproval(id);
    return id;
  }

  private String approved(String code) throws Exception {
    String id = pendingApproval(code);
    approve(id);
    return id;
  }

  private String acceptAndCode(String id) throws Exception {
    accept(id);
    return read(doGet(cbmc, STAFF + id)).at("/summary/dossierCode").asText();
  }

  private void accept(String id) throws Exception {
    doPost(
            cbmc,
            STAFF + id + "/accept",
            Map.of("originalChecked", true, "version", version(cbmc, STAFF + id)))
        .andExpect(status().isOk());
  }

  private void assign(String id) throws Exception {
    doPost(
            cbmc,
            STAFF + id + "/assign",
            Map.of("officerId", officer.getId().toString(), "version", version(cbmc, STAFF + id)))
        .andExpect(status().isOk());
  }

  private void requestSupplement(String id, String component) throws Exception {
    doPost(
            cbmc,
            STAFF + id + "/request-supplement",
            Map.of(
                "instruction",
                "Chụp lại",
                "items",
                List.of(Map.of("componentId", component, "note", "Mờ")),
                "version",
                version(cbmc, STAFF + id)))
        .andExpect(status().isOk());
  }

  private void submitForApproval(String id) throws Exception {
    doPost(
            cctp,
            STAFF + id + "/submit-approval",
            Map.of(
                "opinion",
                "Hợp lệ",
                "resultKind",
                "ISSUED",
                "version",
                version(cctp, STAFF + id)))
        .andExpect(status().isOk());
  }

  private void approve(String id) throws Exception {
    doPost(
            leader,
            STAFF + id + "/approve",
            Map.of("password", PASSWORD, "version", version(leader, STAFF + id)))
        .andExpect(status().isOk());
  }

  private void returnResult(String id, String method) throws Exception {
    doPost(
            cbmc,
            STAFF + id + "/return-result",
            Map.of("method", method, "version", version(cbmc, STAFF + id)))
        .andExpect(status().isOk());
  }

  private void configure(String admin, String unit, Integer days, boolean cutoff)
      throws Exception {
    Map<String, Object> config = new HashMap<>();
    config.put("deadlineUnit", unit);
    config.put("deadlineDays", days);
    config.put("applies15hCutoff", cutoff);
    mvc.perform(
            put("/api/v1/admin/procedures/KHAI_SINH")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(config)))
        .andExpect(status().isOk());
  }

  private String resultFile(String id) throws Exception {
    for (JsonNode file : read(doGet(cbmc, STAFF + id)).get("attachments")) {
      if ("RESULT".equals(file.get("kind").asText())) {
        return file.get("id").asText();
      }
    }
    throw new AssertionError("Không có tệp kết quả");
  }

  private int expectOneMoreLog(String id, int previous) throws Exception {
    int now = read(doGet(cbmc, STAFF + id + "/logs")).size();
    assertThat(now).isEqualTo(previous + 1);
    return now;
  }

  private List<NotificationType> typesSentTo(String id, String email) {
    List<NotificationType> types = new ArrayList<>();
    for (Notification n : notifications.findAll()) {
      if (id.equals(String.valueOf(n.getDossierId())) && email.equals(n.getRecipientEmail())) {
        types.add(n.getType());
      }
    }
    return types;
  }

  private Callable<Integer> race(CountDownLatch start, Callable<ResultActions> call) {
    return () -> {
      start.countDown();
      start.await();
      return call.call().andReturn().getResponse().getStatus();
    };
  }
}
