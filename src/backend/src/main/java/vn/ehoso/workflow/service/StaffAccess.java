package vn.ehoso.workflow.service;

import java.util.UUID;
import vn.ehoso.dossier.domain.Dossier;
import vn.ehoso.dossier.repository.DossierRepository;
import vn.ehoso.security.ApiException;

/** Shared checks of the staff services. */
final class StaffAccess {
  private StaffAccess() {}

  static Dossier find(DossierRepository dossiers, UUID id, long version) {
    Dossier dossier =
        dossiers.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ."));
    if (dossier.getVersion() != version) {
      throw ApiException.conflict("VERSION_CONFLICT", "Hồ sơ vừa được thay đổi, vui lòng tải lại.");
    }
    return dossier;
  }

  static void requireReason(String reason) {
    if (reason == null || reason.isBlank()) {
      throw ApiException.invalidField("reason", "Lý do là bắt buộc.");
    }
  }
}
