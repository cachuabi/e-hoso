package vn.ehoso.dossier.domain;

/** Lifecycle actions (SDD section 9). */
public enum DossierAction {
  SUBMIT,
  REQUEST_SUPPLEMENT,
  RESUBMIT,
  WITHDRAW,
  REJECT,
  ACCEPT,
  ASSIGN,
  SUBMIT_APPROVAL,
  SEND_BACK,
  APPROVE,
  RETURN_RESULT,
  MARK_VERIFICATION
}
