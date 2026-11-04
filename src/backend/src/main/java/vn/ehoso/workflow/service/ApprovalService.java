package vn.ehoso.workflow.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.repository.UserRepository;
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

/** Leader decisions (FR-21, BR-07, BR-08). */
@Service
public class ApprovalService {
  private final DossierRepository dossiers;
  private final ProcedureRepository procedures;
  private final AttachmentRepository attachments;
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final ApprovalAttempts attempts;
  private final DossierLifecycle lifecycle;
  private final Clock clock;

  /** Creates the service. */
  public ApprovalService(
      DossierRepository dossiers,
      ProcedureRepository procedures,
      AttachmentRepository attachments,
      UserRepository users,
      PasswordEncoder encoder,
      ApprovalAttempts attempts,
      DossierLifecycle lifecycle,
      Clock clock) {
    this.dossiers = dossiers;
    this.procedures = procedures;
    this.attachments = attachments;
    this.users = users;
    this.encoder = encoder;
    this.attempts = attempts;
    this.lifecycle = lifecycle;
    this.clock = clock;
  }

  /** Approves with a simulated signature: the leader re-enters the password. */
  @Transactional
  public Dossier approve(UUID id, String password, long version, Actor actor) {
    Dossier dossier = StaffAccess.find(dossiers, id, version);
    lifecycle.check(dossier, DossierAction.APPROVE);
    OffsetDateTime now = OffsetDateTime.now(clock);
    User leader = users.findById(actor.id()).orElseThrow();
    if (leader.getApprovalLockedUntil() != null && leader.getApprovalLockedUntil().isAfter(now)) {
      throw new ApiException(
          HttpStatus.LOCKED,
          "APPROVAL_LOCKED",
          "Thao tác phê duyệt đang bị khoá.",
          List.of(),
          Map.of("lockedUntil", leader.getApprovalLockedUntil().toString()));
    }
    if (password == null || !encoder.matches(password, leader.getPasswordHash())) {
      attempts.recordFailure(actor.id(), now);
      throw ApiException.invalid("WRONG_PASSWORD", "Mật khẩu không đúng.");
    }
    leader.setFailedApprovals(0);
    attachments
        .findFirstByDossierIdAndKindOrderByCreatedAtDesc(id, AttachmentKind.DRAFT)
        .orElseThrow(() -> ApiException.invalid("DRAFT_MISSING", "Thiếu dự thảo kết quả."))
        .setKind(AttachmentKind.RESULT);
    dossier.setResultAt(now);
    Procedure procedure = procedures.findById(dossier.getProcedureId()).orElseThrow();
    if ("ISSUED".equals(dossier.getResultKind()) && procedure.getResultValidityMonths() != null) {
      dossier.setResultExpiresOn(
          now.atZoneSameInstant(clock.getZone())
              .toLocalDate()
              .plusMonths(procedure.getResultValidityMonths()));
    }
    lifecycle.apply(dossier, DossierAction.APPROVE, actor, null, null);
    return dossiers.saveAndFlush(dossier);
  }

  /** Sends the dossier back to the officer; a reason is mandatory (FR-21). */
  @Transactional
  public Dossier sendBack(UUID id, String reason, long version, Actor actor) {
    Dossier dossier = StaffAccess.find(dossiers, id, version);
    StaffAccess.requireReason(reason);
    lifecycle.apply(dossier, DossierAction.SEND_BACK, actor, reason, reason);
    return dossiers.saveAndFlush(dossier);
  }
}
