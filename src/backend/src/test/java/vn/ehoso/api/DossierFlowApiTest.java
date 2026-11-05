package vn.ehoso.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.ehoso.account.domain.Role;
import vn.ehoso.account.domain.User;
import vn.ehoso.support.IntegrationTest;

/** Whole lifecycle through the API: acceptance criterion 1 (CK-E01 to CK-E03) and rules. */
class DossierFlowApiTest extends IntegrationTest {
  private static final String STAFF = "/api/v1/staff/dossiers/";
  private User citizen;
  private User officer;
  private String citizenToken;
  private String cbmc;
  private String cctp;
  private String leader;

  @BeforeEach
  void setUp() throws Exception {
    clock.set("2026-11-05T10:00:00+07:00");
    citizen = user(Role.CITIZEN);
    officer = user(Role.CCTP);
    citizenToken = login(citizen);
    cbmc = login(user(Role.CBMC));
    cctp = login(officer);
    leader = login(user(Role.LEADER));
    mail.failing = false;
  }

  @ParameterizedTest
  @ValueSource(strings = {"CHUNG_THUC", "KHAI_SINH", "XAC_NHAN_HON_NHAN"})
  @Tag("FR-06")
  @Tag("FR-07")
  @Tag("FR-09")
  @Tag("FR-14")
  @Tag("FR-15")
  @Tag("FR-16")
  @Tag("FR-17")
  @Tag("FR-18")
  @Tag("FR-19")
  @Tag("FR-21")
  @Tag("FR-22")
  @Tag("FR-23")
  @Tag("BR-01")
  @Tag("BR-07")
  @Tag("NFR-05")
  void dossierGoesFromDraftToReturned(String code) throws Exception {
    String id = draft(citizenToken, code);
    JsonNode submitted = read(doPost(citizenToken, "/api/v1/dossiers/" + id + "/submit", Map.of()));
    assertThat(submitted.at("/summary/status").asText()).isEqualTo("SUBMITTED");
    String reference = submitted.at("/summary/referenceCode").asText();
    assertThat(reference).matches("TC-[A-Z0-9]{8}");

    accept(id);
    JsonNode accepted = read(doGet(cbmc, STAFF + id));
    assertThat(accepted.at("/summary/dossierCode").asText()).matches("HS-261105-\\d{4}");
    assertThat(accepted.at("/summary/dueAt").asText()).isNotEmpty();

    assign(id);
    submitForApproval(id, "ISSUED");
    approve(id);
    JsonNode approved = read(doGet(leader, STAFF + id));
    assertThat(approved.at("/summary/status").asText()).isEqualTo("APPROVED");
    assertThat(approved.get("onTime").asBoolean()).isTrue();

    doPost(
            cbmc,
            STAFF + id + "/return-result",
            Map.of("method", "ELECTRONIC", "version", version(cbmc, STAFF + id)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.status").value("RETURNED"));

    JsonNode logs = read(doGet(cbmc, STAFF + id + "/logs"));
    assertThat(logs).hasSize(6);
    dispatcher.dispatchPending();
    assertThat(mail.sentTo).contains(citizen.getEmail());
  }

  @Test
  @Tag("BR-08")
  void marriageResultExpiresAfterSixMonthsOnlyWhenIssued() throws Exception {
    String id = submitted("XAC_NHAN_HON_NHAN");
    accept(id);
    assign(id);
    submitForApproval(id, "ISSUED");
    approve(id);
    assertThat(read(doGet(leader, STAFF + id)).get("resultExpiresOn").asText())
        .isEqualTo("2027-05-05");

    String refused = submitted("XAC_NHAN_HON_NHAN");
    accept(refused);
    assign(refused);
    submitForApproval(refused, "REFUSED");
    approve(refused);
    assertThat(read(doGet(leader, STAFF + refused)).get("resultExpiresOn").isNull()).isTrue();
  }

  @Test
  @Tag("FR-20")
  @Tag("BR-05")
  void verificationExtendsOnceAndOnlyForAllowedProcedure() throws Exception {
    String id = submitted("XAC_NHAN_HON_NHAN");
    accept(id);
    assign(id);
    doPost(
            cctp,
            STAFF + id + "/verification",
            Map.of("place", "UBND xã B", "version", version(cctp, STAFF + id)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.verificationFlag").value(true));
    assertThat(read(doGet(cctp, STAFF + id)).at("/summary/dueAt").asText())
        .startsWith("2026-11-13T17:00");
    doPost(
            cctp,
            STAFF + id + "/verification",
            Map.of("place", "UBND xã B", "version", version(cctp, STAFF + id)))
        .andExpect(status().isConflict());

    String other = submitted("KHAI_SINH");
    accept(other);
    assign(other);
    doPost(
            cctp,
            STAFF + other + "/verification",
            Map.of("place", "X", "version", version(cctp, STAFF + other)))
        .andExpect(status().isConflict());
  }

  @Test
  @Tag("FR-11")
  @Tag("FR-16")
  @Tag("BR-09")
  void supplementRoundKeepsReferenceAndCountsRequests() throws Exception {
    String id = submitted("KHAI_SINH");
    String url = "/api/v1/dossiers/" + id;
    String reference = read(doGet(citizenToken, url)).at("/summary/referenceCode").asText();
    JsonNode procedure = read(doGet(null, "/api/v1/procedures/KHAI_SINH"));
    String component = procedure.at("/components/0/id").asText();

    doPost(
            cbmc,
            STAFF + id + "/request-supplement",
            Map.of(
                "instruction", "Thiếu", "items", List.of(), "version", version(cbmc, STAFF + id)))
        .andExpect(status().isUnprocessableEntity());
    doPost(
            cbmc,
            STAFF + id + "/request-supplement",
            Map.of(
                "instruction",
                "Chụp lại rõ nét",
                "items",
                List.of(Map.of("componentId", component, "note", "Mờ")),
                "version",
                version(cbmc, STAFF + id)))
        .andExpect(status().isOk());

    JsonNode waiting = read(doGet(citizenToken, url));
    assertThat(waiting.at("/summary/status").asText()).isEqualTo("NEED_SUPPLEMENT");
    assertThat(waiting.get("supplementCount").asInt()).isEqualTo(1);
    assertThat(waiting.at("/latestSupplement/items/0/note").asText()).isEqualTo("Mờ");

    doPost(citizenToken, url + "/resubmit", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.status").value("SUBMITTED"))
        .andExpect(jsonPath("$.summary.referenceCode").value(reference));
  }

  @Test
  @Tag("FR-10")
  @Tag("BR-04")
  void withdrawAllowedBeforeAcceptanceOnly() throws Exception {
    String first = submitted("KHAI_SINH");
    doPost(citizenToken, "/api/v1/dossiers/" + first + "/withdraw", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.status").value("WITHDRAWN"));

    String second = submitted("KHAI_SINH");
    accept(second);
    doPost(citizenToken, "/api/v1/dossiers/" + second + "/withdraw", Map.of())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.currentStatus").value("ACCEPTED"));
  }

  @Test
  @Tag("FR-16")
  void acceptingRequiresOriginalCheckForCertification() throws Exception {
    String id = submitted("CHUNG_THUC");
    doPost(
            cbmc,
            STAFF + id + "/accept",
            Map.of("originalChecked", false, "version", version(cbmc, STAFF + id)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ORIGINAL_NOT_CHECKED"));
    doPost(cbmc, STAFF + id + "/reject", Map.of("reason", " ", "version", 99))
        .andExpect(status().isConflict());
    doPost(
            cbmc,
            STAFF + id + "/reject",
            Map.of("reason", " ", "version", version(cbmc, STAFF + id)))
        .andExpect(status().isUnprocessableEntity());
    doPost(
            cbmc,
            STAFF + id + "/reject",
            Map.of("reason", "Sai thủ tục", "version", version(cbmc, STAFF + id)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.status").value("REJECTED"));
  }

  @Test
  @Tag("FR-09")
  void submitFailsWhenRequiredFileOrFieldMissing() throws Exception {
    JsonNode created =
        read(
            doPost(
                citizenToken,
                "/api/v1/dossiers",
                Map.of("procedureCode", "KHAI_SINH", "formData", Map.of())));
    String id = created.at("/summary/id").asText();
    doPost(citizenToken, "/api/v1/dossiers/" + id + "/submit", Map.of())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors").isArray());
    assertThat(read(doGet(citizenToken, "/api/v1/dossiers/" + id)).at("/summary/status").asText())
        .isEqualTo("DRAFT");
  }

  @Test
  @Tag("FR-19")
  void submitForApprovalNeedsOpinionAndDraft() throws Exception {
    String id = submitted("KHAI_SINH");
    accept(id);
    assign(id);
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
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("DRAFT_MISSING"));
  }

  @Test
  @Tag("FR-21")
  void wrongPasswordThreeTimesLocksApprovalAndSendBackNeedsReason() throws Exception {
    String id = submitted("KHAI_SINH");
    accept(id);
    assign(id);
    submitForApproval(id, "ISSUED");
    String bossToken = login(user(Role.LEADER));
    for (int i = 0; i < 3; i++) {
      doPost(
              bossToken,
              STAFF + id + "/approve",
              Map.of("password", "sai", "version", version(bossToken, STAFF + id)))
          .andExpect(status().isUnprocessableEntity());
    }
    doPost(
            bossToken,
            STAFF + id + "/approve",
            Map.of("password", PASSWORD, "version", version(bossToken, STAFF + id)))
        .andExpect(status().isLocked());
    assertThat(read(doGet(bossToken, STAFF + id)).at("/summary/status").asText())
        .isEqualTo("PENDING_APPROVAL");
    doPost(
            leader,
            STAFF + id + "/send-back",
            Map.of("reason", "", "version", version(leader, STAFF + id)))
        .andExpect(status().isUnprocessableEntity());
    doPost(
            leader,
            STAFF + id + "/send-back",
            Map.of("reason", "Sửa dự thảo", "version", version(leader, STAFF + id)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.sendBackReason").value("Sửa dự thảo"));
  }

  @Test
  @Tag("FR-12")
  @Tag("NFR-04")
  void publicLookupRevealsNoPersonalData() throws Exception {
    String id = submitted("KHAI_SINH");
    String code = read(doGet(cbmc, STAFF + id)).at("/summary/referenceCode").asText();
    String last4 = citizen.getNationalId().substring(8);
    JsonNode found =
        read(
            doPost(
                    null,
                    "/api/v1/public/lookup",
                    Map.of("code", code, "nationalIdLast4", last4))
                .andExpect(status().isOk()));
    assertThat(found.fieldNames())
        .toIterable()
        .containsExactlyInAnyOrder("procedureName", "status", "milestones", "dueAt");
    assertThat(found.toString()).doesNotContain(citizen.getFullName()).doesNotContain(last4);
    String wrongDigits = "0000".equals(last4) ? "1111" : "0000";
    String wrong =
        read(
                doPost(
                        null,
                        "/api/v1/public/lookup",
                        Map.of("code", code, "nationalIdLast4", wrongDigits))
                    .andExpect(status().isNotFound()))
            .get("detail")
            .asText();
    String absent =
        read(
                doPost(
                        null,
                        "/api/v1/public/lookup",
                        Map.of("code", "TC-00000000", "nationalIdLast4", last4))
                    .andExpect(status().isNotFound()))
            .get("detail")
            .asText();
    assertThat(wrong).isEqualTo(absent);
  }

  @Test
  @Tag("FR-13")
  @Tag("FR-15")
  void listsAreScopedAndQueueIsOldestFirst() throws Exception {
    String first = submitted("KHAI_SINH");
    doGet(citizenToken, "/api/v1/dossiers/" + first + "/history")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("SUBMITTED"));
    clock.advance(java.time.Duration.ofMinutes(5));
    String second = submitted("KHAI_SINH");
    JsonNode mine = read(doGet(citizenToken, "/api/v1/dossiers?status=SUBMITTED"));
    assertThat(mine.get("total").asInt()).isEqualTo(2);
    JsonNode other = read(doGet(login(user(Role.CITIZEN)), "/api/v1/dossiers"));
    assertThat(other.get("total").asInt()).isZero();

    List<String> queue =
        read(doGet(cbmc, STAFF.substring(0, STAFF.length() - 1) + "?status=SUBMITTED&size=100"))
            .get("items")
            .findValuesAsText("id");
    assertThat(queue.indexOf(first)).isLessThan(queue.indexOf(second));
  }

  @Test
  @Tag("FR-05")
  void otherOwnersAndUnassignedOfficersAreRefused() throws Exception {
    String id = submitted("KHAI_SINH");
    doGet(login(user(Role.CITIZEN)), "/api/v1/dossiers/" + id).andExpect(status().isNotFound());
    accept(id);
    assign(id);
    doGet(login(user(Role.CCTP)), STAFF + id).andExpect(status().isForbidden());
  }

  private String submitted(String code) throws Exception {
    String id = draft(citizenToken, code);
    doPost(citizenToken, "/api/v1/dossiers/" + id + "/submit", Map.of())
        .andExpect(status().isOk());
    return id;
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

  private void submitForApproval(String id, String kind) throws Exception {
    upload(cctp, id, "DRAFT", null, "du-thao.pdf", PDF).andExpect(status().isCreated());
    doPost(
            cctp,
            STAFF + id + "/submit-approval",
            Map.of("opinion", "Hợp lệ", "resultKind", kind, "version", version(cctp, STAFF + id)))
        .andExpect(status().isOk());
  }

  private void approve(String id) throws Exception {
    doPost(
            leader,
            STAFF + id + "/approve",
            Map.of("password", PASSWORD, "version", version(leader, STAFF + id)))
        .andExpect(status().isOk());
  }
}
