package vn.ehoso.dossier.domain;

/** Lifecycle states (SRS section 5). */
public enum DossierStatus {
  DRAFT,
  SUBMITTED,
  NEED_SUPPLEMENT,
  REJECTED,
  WITHDRAWN,
  ACCEPTED,
  IN_PROGRESS,
  PENDING_APPROVAL,
  APPROVED,
  RETURNED
}
