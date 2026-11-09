package vn.ehoso.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import vn.ehoso.file.domain.Attachment;
import vn.ehoso.file.domain.AttachmentKind;
import vn.ehoso.file.repository.AttachmentRepository;
import vn.ehoso.security.Actor;
import vn.ehoso.security.ApiException;

/** Stores files with compensation between the database and the store (SDD 6.3). */
@Service
public class AttachmentService {
  private static final Logger LOG = LoggerFactory.getLogger(AttachmentService.class);
  private static final long MAX_FILE = 10L * 1024 * 1024;
  private static final long MAX_DOSSIER = 30L * 1024 * 1024;
  private static final Duration LINK_TTL = Duration.ofMinutes(5);

  private final AttachmentRepository attachments;
  private final AttachmentGuard guard;
  private final FileStoragePort storage;
  private final FileTypeSniffer sniffer;
  private final TransactionTemplate transaction;
  private final Clock clock;

  /** Creates the service. */
  public AttachmentService(
      AttachmentRepository attachments,
      AttachmentGuard guard,
      FileStoragePort storage,
      FileTypeSniffer sniffer,
      TransactionTemplate transaction,
      Clock clock) {
    this.attachments = attachments;
    this.guard = guard;
    this.storage = storage;
    this.sniffer = sniffer;
    this.transaction = transaction;
    this.clock = clock;
  }

  /** A signed download link. */
  public record SignedUrl(URI url, OffsetDateTime expiresAt) {}

  /** Validates, stores, then records a file; removes the file if recording fails (Q-01). */
  public Attachment upload(
      UUID dossierId, UUID componentId, AttachmentKind kind, MultipartFile file, Actor actor) {
    guard.checkUpload(dossierId, kind, componentId, actor);
    String mimeType = validate(file, kind);
    checkTotal(dossierId, file.getSize());
    String key = "ho-so/" + dossierId + "/" + UUID.randomUUID();
    try (InputStream content = file.getInputStream()) {
      storage.put(key, content, file.getSize(), mimeType);
    } catch (IOException ex) {
      throw ApiException.invalid("FILE_UNREADABLE", "Không đọc được tệp.");
    }
    try {
      return transaction.execute(
          status -> {
            guard.lock(dossierId);
            guard.checkUpload(dossierId, kind, componentId, actor);
            checkTotal(dossierId, file.getSize());
            Attachment attachment = new Attachment();
            attachment.setDossierId(dossierId);
            attachment.setComponentId(componentId);
            attachment.setKind(kind);
            attachment.setFileName(safeName(file.getOriginalFilename()));
            attachment.setMimeType(mimeType);
            attachment.setSizeBytes(file.getSize());
            attachment.setStorageKey(key);
            attachment.setUploadedBy(actor.id());
            return attachments.save(attachment);
          });
    } catch (RuntimeException ex) {
      deleteQuietly(key);
      throw ex;
    }
  }

  /** Deletes a file while the dossier may still change. */
  public void delete(UUID attachmentId, Actor actor) {
    String key =
        transaction.execute(
            status -> {
              Attachment attachment = find(attachmentId);
              guard.checkDelete(attachment, actor);
              attachments.delete(attachment);
              return attachment.getStorageKey();
            });
    deleteQuietly(key);
  }

  /** Signs a link valid for 5 minutes, only after checking permission (NFR-04). */
  public SignedUrl downloadUrl(UUID attachmentId, Actor actor) {
    Attachment attachment = find(attachmentId);
    guard.checkDownload(attachment, actor);
    URI url = storage.presignGet(attachment.getStorageKey(), LINK_TTL, attachment.getFileName());
    return new SignedUrl(url, OffsetDateTime.now(clock).plus(LINK_TTL));
  }

  private Attachment find(UUID id) {
    return attachments.findById(id).orElseThrow(() -> ApiException.notFound("Không có tệp."));
  }

  private String validate(MultipartFile file, AttachmentKind kind) {
    if (file.isEmpty()) {
      throw ApiException.invalid("FILE_EMPTY", "Tệp rỗng.");
    }
    if (file.getSize() > MAX_FILE) {
      throw ApiException.invalid("FILE_TOO_LARGE", "Tệp vượt quá 10 MB.");
    }
    String mimeType;
    try (InputStream content = file.getInputStream()) {
      mimeType =
          sniffer
              .detect(content)
              .orElseThrow(
                  () -> ApiException.invalid("FILE_TYPE", "Chỉ nhận tệp PDF, JPG hoặc PNG."));
    } catch (IOException ex) {
      throw ApiException.invalid("FILE_UNREADABLE", "Không đọc được tệp.");
    }
    if (kind != AttachmentKind.DOSSIER && !"application/pdf".equals(mimeType)) {
      throw ApiException.invalid("FILE_TYPE", "Dự thảo kết quả phải là tệp PDF.");
    }
    return mimeType;
  }

  private void checkTotal(UUID dossierId, long extra) {
    if (attachments.totalSize(dossierId) + extra > MAX_DOSSIER) {
      throw ApiException.invalid("DOSSIER_TOO_LARGE", "Tổng dung lượng hồ sơ vượt quá 30 MB.");
    }
  }

  private void deleteQuietly(String key) {
    try {
      storage.delete(key);
    } catch (RuntimeException ex) {
      LOG.warn("Cannot delete object {}", key, ex);
    }
  }

  private String safeName(String name) {
    String base = name == null ? "tep" : name.replaceAll("[\\\\/]", "_");
    return base.length() > 255 ? base.substring(base.length() - 255) : base;
  }
}
