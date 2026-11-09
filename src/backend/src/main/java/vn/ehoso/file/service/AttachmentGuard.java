package vn.ehoso.file.service;

import java.util.UUID;
import vn.ehoso.file.domain.Attachment;
import vn.ehoso.file.domain.AttachmentKind;
import vn.ehoso.security.Actor;

/** Permission and state checks for files, implemented by the dossier module (TK-10). */
public interface AttachmentGuard {
  /** Fails unless the actor may add a file of this kind to the dossier now. */
  void checkUpload(UUID dossierId, AttachmentKind kind, UUID componentId, Actor actor);

  /** Takes a row lock on the dossier for the current transaction. */
  void lock(UUID dossierId);

  /** Fails unless the actor may delete the file now. */
  void checkDelete(Attachment attachment, Actor actor);

  /** Fails unless the actor may download the file. */
  void checkDownload(Attachment attachment, Actor actor);
}
