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
  Page<Dossier> findByApplicantId(UUID applicantId, Pageable pageable);

  Page<Dossier> findByApplicantIdAndStatus(
      UUID applicantId, DossierStatus status, Pageable pageable);

  Optional<Dossier> findByReferenceCode(String referenceCode);

  Optional<Dossier> findByDossierCode(String dossierCode);

  boolean existsByReferenceCode(String referenceCode);

  long countByAssignedOfficerIdAndStatusIn(UUID officerId, List<DossierStatus> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from Dossier d where d.id = :id")
  Optional<Dossier> findForUpdate(UUID id);

  @Query(
      "select d.status, count(d) from Dossier d where d.submittedAt >= :from"
          + " and d.submittedAt < :to group by d.status")
  List<Object[]> countByStatus(OffsetDateTime from, OffsetDateTime to);

  List<Dossier> findByResultAtGreaterThanEqualAndResultAtLessThan(
      OffsetDateTime from, OffsetDateTime to);

  long countByDueAtBeforeAndResultAtIsNull(OffsetDateTime now);
}
