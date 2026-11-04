package vn.ehoso.dossier.service;

import java.util.UUID;
import org.springframework.stereotype.Component;
import vn.ehoso.account.domain.Role;
import vn.ehoso.catalog.domain.DossierComponent;
import vn.ehoso.catalog.repository.DossierComponentRepository;
import vn.ehoso.dossier.domain.Dossier;
import vn.ehoso.dossier.domain.DossierAction;
import vn.ehoso.dossier.domain.DossierStatus;
import vn.ehoso.dossier.repository.DossierRepository;
import vn.ehoso.file.domain.Attachment;
import vn.ehoso.file.domain.AttachmentKind;
import vn.ehoso.file.service.AttachmentGuard;
import vn.ehoso.security.Actor;
import vn.ehoso.security.ApiException;

/** File rules of SDD section 12.2. */
@Component
class DossierAttachmentGuard implements AttachmentGuard {
  private final DossierRepository dossiers;
  private final DossierComponentRepository components;
  private final DossierLifecycle lifecycle;

  DossierAttachmentGuard(
      DossierRepository dossiers,
      DossierComponentRepository components,
      DossierLifecycle lifecycle) {
    this.dossiers = dossiers;
    this.components = components;
    this.lifecycle = lifecycle;
  }

  @Override
  public void checkUpload(UUID dossierId, AttachmentKind kind, UUID componentId, Actor actor) {
    Dossier dossier = find(dossierId);
    checkChange(dossier, kind, actor);
    if (kind == AttachmentKind.DOSSIER) {
      boolean valid =
          componentId != null
              && components
                  .findById(componentId)
                  .map(DossierComponent::getProcedureId)
                  .filter(dossier.getProcedureId()::equals)
                  .isPresent();
      if (!valid) {
        throw ApiException.invalidField("componentId", "Thành phần hồ sơ không hợp lệ.");
      }
    }
  }

  @Override
  public void lock(UUID dossierId) {
    dossiers.findForUpdate(dossierId).orElseThrow();
  }

  @Override
  public void checkDelete(Attachment attachment, Actor actor) {
    checkChange(find(attachment.getDossierId()), attachment.getKind(), actor);
  }

  @Override
  public void checkDownload(Attachment attachment, Actor actor) {
    Dossier dossier = find(attachment.getDossierId());
    boolean allowed =
        switch (actor.role()) {
          case CBMC, LEADER -> true;
          case CCTP -> actor.id().equals(dossier.getAssignedOfficerId());
          case CITIZEN -> dossier.getApplicantId().equals(actor.id())
              && citizenMaySee(dossier, attachment);
          default -> false;
        };
    if (!allowed) {
      throw ApiException.forbidden();
    }
  }

  private boolean citizenMaySee(Dossier dossier, Attachment attachment) {
    return switch (attachment.getKind()) {
      case DOSSIER -> true;
      case DRAFT -> false;
      case RESULT -> dossier.getStatus() == DossierStatus.RETURNED
          && "ELECTRONIC".equals(dossier.getReturnMethod());
    };
  }

  private void checkChange(Dossier dossier, AttachmentKind kind, Actor actor) {
    if (actor.role() == Role.CITIZEN && kind == AttachmentKind.DOSSIER) {
      if (!dossier.getApplicantId().equals(actor.id())) {
        throw ApiException.notFound("Không tìm thấy hồ sơ.");
      }
      if (dossier.getStatus() != DossierStatus.DRAFT
          && dossier.getStatus() != DossierStatus.NEED_SUPPLEMENT) {
        lifecycle.check(dossier, DossierAction.SUBMIT);
      }
    } else if (actor.role() == Role.CCTP && kind == AttachmentKind.DRAFT) {
      if (!actor.id().equals(dossier.getAssignedOfficerId())) {
        throw ApiException.forbidden();
      }
      lifecycle.check(dossier, DossierAction.SUBMIT_APPROVAL);
    } else {
      throw ApiException.forbidden();
    }
  }

  private Dossier find(UUID id) {
    return dossiers.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ."));
  }
}
