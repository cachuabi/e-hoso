package vn.ehoso.dossier.repository;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import vn.ehoso.dossier.domain.Dossier;
import vn.ehoso.dossier.domain.DossierStatus;

/** Access to dossiers. */
public interface DossierRepository
    extends JpaRepository<Dossier, UUID>, JpaSpecificationExecutor<Dossier> {
  /** Spring Data query. */
  Page<Dossier> findByApplicantId(UUID applicantId, Pageable pageable);

  /** Spring Data query. */
  Page<Dossier> findByApplicantIdAndStatus(
      UUID applicantId, DossierStatus status, Pageable pageable);

  /** Spring Data query. */
  Optional<Dossier> findByReferenceCode(String referenceCode);

  /** Spring Data query. */
  Optional<Dossier> findByDossierCode(String dossierCode);

  /** Spring Data query. */
  boolean existsByReferenceCode(String referenceCode);

  /** Spring Data query. */
  long countByAssignedOfficerIdAndStatusIn(UUID officerId, List<DossierStatus> statuses);

  /** Spring Data query. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from Dossier d where d.id = :id")
  Optional<Dossier> findForUpdate(UUID id);

  /** Spring Data query. */
  @Query(
      "select d.status, count(d) from Dossier d where d.submittedAt >= :from"
          + " and d.submittedAt < :to group by d.status")
  List<Object[]> countByStatus(OffsetDateTime from, OffsetDateTime to);

  /** Spring Data query. */
  List<Dossier> findByResultAtGreaterThanEqualAndResultAtLessThan(
      OffsetDateTime from, OffsetDateTime to);

  /** Spring Data query. */
  long countByDueAtBeforeAndResultAtIsNull(OffsetDateTime now);
}
