package vn.ehoso.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import vn.ehoso.account.domain.Role;
import vn.ehoso.file.repository.AttachmentRepository;
import vn.ehoso.support.IntegrationTest;

/** Files (CK-F01, CK-F02, CK-F03). */
class AttachmentApiTest extends IntegrationTest {
  private static final int MEGABYTE = 1024 * 1024;

  @MockitoSpyBean private AttachmentRepository attachments;
  private String citizen;
  private String dossierId;
  private String component;

  @BeforeEach
  void setUp() throws Exception {
    citizen = login(user(Role.CITIZEN));
    JsonNode procedure = read(doGet(null, "/api/v1/procedures/KHAI_SINH"));
    component = procedure.at("/components/0/id").asText();
    dossierId =
        read(
                doPost(
                    citizen,
                    "/api/v1/dossiers",
                    Map.of("procedureCode", "KHAI_SINH", "formData", validForm("KHAI_SINH"))))
            .at("/summary/id")
            .asText();
  }

  @Test
  @Tag("FR-08")
  void contentDecidesTheTypeAndLimitsApply() throws Exception {
    upload(citizen, dossierId, "DOSSIER", component, "virus.pdf", "MZ not a pdf".getBytes())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("FILE_TYPE"));
    upload(citizen, dossierId, "DOSSIER", component, "ok.pdf", pdf(MEGABYTE))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.mimeType").value("application/pdf"));
    upload(citizen, dossierId, "DOSSIER", component, "tren.pdf", pdf(10 * MEGABYTE + 1))
        .andExpect(status().isUnprocessableEntity());
    upload(citizen, dossierId, "DOSSIER", "not-used", "x.pdf", PDF)
        .andExpect(status().isBadRequest());
  }

  @Test
  @Tag("FR-08")
  void totalIsLimitedToThirtyMegabytes() throws Exception {
    for (int i = 0; i < 3; i++) {
      upload(citizen, dossierId, "DOSSIER", component, "a.pdf", pdf(10 * MEGABYTE))
          .andExpect(status().isCreated());
    }
    upload(citizen, dossierId, "DOSSIER", component, "b.pdf", pdf(1))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("DOSSIER_TOO_LARGE"));
  }

  @Test
  @Tag("NFR-04")
  void downloadLinkLastsFiveMinutesAndNeedsPermission() throws Exception {
    String id =
        read(upload(citizen, dossierId, "DOSSIER", component, "a.pdf", PDF)).get("id").asText();
    doGet(citizen, "/api/v1/attachments/" + id + "/download-url")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.containsString("ttl=300")));
    doGet(login(user(Role.CITIZEN)), "/api/v1/attachments/" + id + "/download-url")
        .andExpect(status().isForbidden());
    doGet(login(user(Role.CCTP)), "/api/v1/attachments/" + id + "/download-url")
        .andExpect(status().isForbidden());
    doGet(login(user(Role.ADMIN)), "/api/v1/attachments/" + id + "/download-url")
        .andExpect(status().isForbidden());
  }

  @Test
  @Tag("FR-08")
  void objectIsRemovedWhenTheDatabaseFails() throws Exception {
    int before = storage.objects.size();
    doThrow(new IllegalStateException("db down")).when(attachments).save(any());
    try {
      int code =
          upload(citizen, dossierId, "DOSSIER", component, "a.pdf", PDF)
              .andReturn()
              .getResponse()
              .getStatus();
      assertThat(code).isGreaterThanOrEqualTo(500);
    } catch (Exception expected) {
      assertThat(expected).hasRootCauseMessage("db down");
    }
    assertThat(storage.objects).hasSize(before);
  }

  @Test
  @Tag("FR-11")
  void filesCanBeRemovedOnlyWhileEditable() throws Exception {
    String id =
        read(upload(citizen, dossierId, "DOSSIER", component, "a.pdf", PDF)).get("id").asText();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                    "/api/v1/attachments/" + id)
                .header("Authorization", citizen))
        .andExpect(status().isNoContent());
    String second =
        read(upload(citizen, dossierId, "DOSSIER", component, "b.pdf", PDF)).get("id").asText();
    doPost(citizen, "/api/v1/dossiers/" + dossierId + "/submit", Map.of())
        .andExpect(status().isOk());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                    "/api/v1/attachments/" + second)
                .header("Authorization", citizen))
        .andExpect(status().isConflict());
  }

  private byte[] pdf(int size) {
    byte[] data = new byte[size];
    Arrays.fill(data, (byte) ' ');
    System.arraycopy(PDF, 0, data, 0, Math.min(PDF.length, size));
    return data;
  }
}
