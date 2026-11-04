package vn.ehoso.file.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Metadata of a stored file; the content lives in the S3 store (DR-05). */
@Entity
@Table(name = "attachment")
@Getter
@Setter
public class Attachment {
  @Id private UUID id = UUID.randomUUID();
  private UUID dossierId;
  private UUID componentId;

  @Enumerated(EnumType.STRING)
  private AttachmentKind kind;

  private String fileName;
  private String mimeType;
  private long sizeBytes;
  private String storageKey;
  private UUID uploadedBy;
  private OffsetDateTime createdAt = OffsetDateTime.now();
}
