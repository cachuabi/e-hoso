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
  List<Attachment> findByDossierIdOrderByCreatedAtAsc(UUID dossierId);

  Optional<Attachment> findFirstByDossierIdAndKindOrderByCreatedAtDesc(
      UUID dossierId, AttachmentKind kind);

  boolean existsByDossierIdAndKind(UUID dossierId, AttachmentKind kind);

  @Query("select coalesce(sum(a.sizeBytes), 0) from Attachment a where a.dossierId = :dossierId")
  long totalSize(UUID dossierId);
}
