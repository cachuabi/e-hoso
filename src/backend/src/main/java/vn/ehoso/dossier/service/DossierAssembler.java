package vn.ehoso.dossier.service;

import java.util.List;
import org.springframework.stereotype.Component;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.repository.UserRepository;
import vn.ehoso.audit.domain.ProcessingLog;
import vn.ehoso.audit.service.AuditWriter;
import vn.ehoso.catalog.domain.DossierComponent;
import vn.ehoso.catalog.domain.Procedure;
import vn.ehoso.catalog.repository.DossierComponentRepository;
import vn.ehoso.catalog.repository.ProcedureRepository;
import vn.ehoso.dossier.domain.Dossier;
import vn.ehoso.dossier.domain.DossierAction;
import vn.ehoso.dossier.domain.DossierStatus;
import vn.ehoso.dossier.domain.SupplementRequest;
import vn.ehoso.dossier.repository.SupplementItemRepository;
import vn.ehoso.dossier.repository.SupplementRequestRepository;
import vn.ehoso.file.repository.AttachmentRepository;

/** Builds the API views of a dossier. */
@Component
public class DossierAssembler {
  private final ProcedureRepository procedures;
  private final DossierComponentRepository components;
  private final UserRepository users;
  private final AttachmentRepository attachments;
  private final SupplementRequestRepository supplements;
  private final SupplementItemRepository supplementItems;
  private final AuditWriter audit;

  /** Creates the assembler. */
  public DossierAssembler(
      ProcedureRepository procedures,
      DossierComponentRepository components,
      UserRepository users,
      AttachmentRepository attachments,
      SupplementRequestRepository supplements,
      SupplementItemRepository supplementItems,
      AuditWriter audit) {
    this.procedures = procedures;
    this.components = components;
    this.users = users;
    this.attachments = attachments;
    this.supplements = supplements;
    this.supplementItems = supplementItems;
    this.audit = audit;
  }

  /** Builds the list view. */
  public DossierViews.Summary summary(Dossier d, String label) {
    Procedure procedure = procedures.findById(d.getProcedureId()).orElseThrow();
    return new DossierViews.Summary(
        d.getId(),
        d.getReferenceCode(),
        d.getDossierCode(),
        procedure.getCode(),
        procedure.getName(),
        d.getStatus(),
        d.getSubmittedAt(),
        d.getDueAt(),
        label,
        d.getAssignedOfficerId() == null ? null : nameOf(d.getAssignedOfficerId()),
        nameOf(d.getApplicantId()));
  }

  /** Builds the full view. */
  public DossierViews.Detail detail(Dossier d, String label) {
    Procedure procedure = procedures.findById(d.getProcedureId()).orElseThrow();
    List<DossierViews.FileView> files =
        attachments.findByDossierIdOrderByCreatedAtAsc(d.getId()).stream()
            .map(
                a ->
                    new DossierViews.FileView(
                        a.getId(),
                        a.getComponentId(),
                        a.getFileName(),
                        a.getMimeType(),
                        a.getSizeBytes(),
                        a.getKind().name()))
            .toList();
    Boolean onTime =
        d.getResultAt() == null || d.getDueAt() == null
            ? null
            : !d.getResultAt().isAfter(d.getDueAt());
    return new DossierViews.Detail(
        summary(d, label),
        d.getFormData(),
        files,
        d.getSupplementCount(),
        latestSupplement(d),
        d.isOriginalChecked(),
        procedure.isRequiresOriginalCheck(),
        procedure.isAllowsVerification(),
        d.isVerificationFlag(),
        d.getVerificationPlace(),
        d.getReviewOpinion(),
        d.getStatus() == DossierStatus.IN_PROGRESS ? lastSendBack(d) : null,
        d.getResultKind(),
        d.getResultAt(),
        onTime,
        d.getResultExpiresOn(),
        d.getReturnMethod(),
        d.getVersion());
  }

  private String nameOf(java.util.UUID id) {
    return users.findById(id).map(User::getFullName).orElse(null);
  }

  private DossierViews.SupplementView latestSupplement(Dossier d) {
    if (d.getStatus() != DossierStatus.NEED_SUPPLEMENT) {
      return null;
    }
    SupplementRequest request =
        supplements.findFirstByDossierIdOrderByCreatedAtDesc(d.getId()).orElse(null);
    if (request == null) {
      return null;
    }
    List<DossierViews.SupplementItemView> items =
        supplementItems.findByRequestId(request.getId()).stream()
            .map(
                i ->
                    new DossierViews.SupplementItemView(
                        i.getComponentId(),
                        components
                            .findById(i.getComponentId())
                            .map(DossierComponent::getName)
                            .orElse(""),
                        i.getNote()))
            .toList();
    return new DossierViews.SupplementView(request.getInstruction(), items);
  }

  private String lastSendBack(Dossier d) {
    List<ProcessingLog> logs = audit.read(d.getId());
    if (logs.isEmpty()) {
      return null;
    }
    ProcessingLog last = logs.get(logs.size() - 1);
    return DossierAction.SEND_BACK.name().equals(last.getAction()) ? last.getNote() : null;
  }
}
