package vn.ehoso.dossier.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import vn.ehoso.dossier.domain.DossierStatus;

/** Data returned by the dossier and staff APIs. */
public final class DossierViews {
  private DossierViews() {}

  /** A dossier in a list. */
  public record Summary(
      UUID id,
      String referenceCode,
      String dossierCode,
      String procedureCode,
      String procedureName,
      DossierStatus status,
      OffsetDateTime submittedAt,
      OffsetDateTime dueAt,
      String deadlineLabel,
      String assignedOfficerName,
      String applicantName) {}

  /** One page of dossiers. */
  public record Page(List<Summary> items, long total) {}

  /** A file of a dossier. */
  public record FileView(
      UUID id, UUID componentId, String fileName, String mimeType, long sizeBytes, String kind) {}

  /** A missing component. */
  public record SupplementItemView(UUID componentId, String componentName, String note) {}

  /** The latest supplement request. */
  public record SupplementView(String instruction, List<SupplementItemView> items) {}

  /** Full data of a dossier. */
  public record Detail(
      Summary summary,
      Map<String, Object> formData,
      List<FileView> attachments,
      int supplementCount,
      SupplementView latestSupplement,
      boolean originalChecked,
      boolean requiresOriginalCheck,
      boolean allowsVerification,
      boolean verificationFlag,
      String verificationPlace,
      String reviewOpinion,
      String sendBackReason,
      String resultKind,
      OffsetDateTime resultAt,
      Boolean onTime,
      LocalDate resultExpiresOn,
      String returnMethod,
      long version) {}

  /** A milestone of the status history. */
  public record StatusEvent(DossierStatus status, OffsetDateTime occurredAt) {}

  /** What public lookup may reveal: no personal data (FR-12). */
  public record PublicStatus(
      String procedureName,
      DossierStatus status,
      List<StatusEvent> milestones,
      OffsetDateTime dueAt) {}
}
