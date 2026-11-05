package vn.ehoso.workflow.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.catalog.domain.Procedure;
import vn.ehoso.catalog.repository.ProcedureRepository;
import vn.ehoso.dossier.domain.Dossier;
import vn.ehoso.dossier.domain.DossierAction;
import vn.ehoso.dossier.repository.DossierRepository;
import vn.ehoso.dossier.service.DossierLifecycle;
import vn.ehoso.file.domain.AttachmentKind;
import vn.ehoso.file.repository.AttachmentRepository;
import vn.ehoso.security.Actor;
import vn.ehoso.security.ApiException;

/** Officer actions on assigned dossiers (FR-19, FR-20, BR-05). */
@Service
public class ReviewService {
  private static final int VERIFICATION_DAYS = 3;

  private final DossierRepository dossiers;
  private final ProcedureRepository procedures;
  private final AttachmentRepository attachments;
  private final DossierLifecycle lifecycle;
  private final DeadlineCalculator deadlines;

  /** Creates the service. */
  public ReviewService(
      DossierRepository dossiers,
      ProcedureRepository procedures,
      AttachmentRepository attachments,
      DossierLifecycle lifecycle,
      DeadlineCalculator deadlines) {
    this.dossiers = dossiers;
    this.procedures = procedures;
    this.attachments = attachments;
    this.lifecycle = lifecycle;
    this.deadlines = deadlines;
  }

  /** Marks that verification is needed and extends the due date once (FR-20, BR-05). */
  @Transactional
  public Dossier markVerification(UUID id, String place, long version, Actor actor) {
    Dossier dossier = assigned(id, version, actor);
    lifecycle.check(dossier, DossierAction.MARK_VERIFICATION);
    Procedure procedure = procedures.findById(dossier.getProcedureId()).orElseThrow();
    if (!procedure.isAllowsVerification()) {
      throw ApiException.conflict("VERIFICATION_NOT_ALLOWED", "Thủ tục không có bước xác minh.");
    }
    if (dossier.isVerificationFlag()) {
      throw ApiException.conflict("ALREADY_EXTENDED", "Hồ sơ đã được gia hạn một lần.");
    }
    if (place == null || place.isBlank()) {
      throw ApiException.invalidField("place", "Nơi xác minh là bắt buộc.");
    }
    dossier.setVerificationFlag(true);
    dossier.setVerificationPlace(place);
    dossier.setDueAt(deadlines.extend(dossier.getDueAt(), VERIFICATION_DAYS));
    lifecycle.apply(
        dossier, DossierAction.MARK_VERIFICATION, actor, place, dossier.getDueAt().toString());
    return dossiers.saveAndFlush(dossier);
  }

  /** Submits for approval; opinion and draft are mandatory (FR-19, UC-10). */
  @Transactional
  public Dossier submitForApproval(
      UUID id, String opinion, String resultKind, long version, Actor actor) {
    Dossier dossier = assigned(id, version, actor);
    lifecycle.check(dossier, DossierAction.SUBMIT_APPROVAL);
    if (opinion == null || opinion.isBlank()) {
      throw ApiException.invalidField("opinion", "Ý kiến thẩm định là bắt buộc.");
    }
    if (!"ISSUED".equals(resultKind) && !"REFUSED".equals(resultKind)) {
      throw ApiException.invalidField("resultKind", "Loại kết quả không hợp lệ.");
    }
    if (!attachments.existsByDossierIdAndKind(id, AttachmentKind.DRAFT)) {
      throw ApiException.invalid("DRAFT_MISSING", "Cần đính kèm dự thảo kết quả.");
    }
    dossier.setReviewOpinion(opinion);
    dossier.setResultKind(resultKind);
    lifecycle.apply(dossier, DossierAction.SUBMIT_APPROVAL, actor, opinion, null);
    return dossiers.saveAndFlush(dossier);
  }

  private Dossier assigned(UUID id, long version, Actor actor) {
    Dossier dossier = StaffAccess.find(dossiers, id, version);
    if (!actor.id().equals(dossier.getAssignedOfficerId())) {
      throw ApiException.forbidden();
    }
    return dossier;
  }
}
