package vn.ehoso.file.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.ehoso.file.domain.Attachment;
import vn.ehoso.file.domain.AttachmentKind;

/** Access to attachment metadata. */
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
  /** Spring Data query. */
  List<Attachment> findByDossierIdOrderByCreatedAtAsc(UUID dossierId);

  /** Spring Data query. */
  Optional<Attachment> findFirstByDossierIdAndKindOrderByCreatedAtDesc(
      UUID dossierId, AttachmentKind kind);

  /** Spring Data query. */
  boolean existsByDossierIdAndKind(UUID dossierId, AttachmentKind kind);

  /** Spring Data query. */
  @Query("select coalesce(sum(a.sizeBytes), 0) from Attachment a where a.dossierId = :dossierId")
  long totalSize(UUID dossierId);
}
