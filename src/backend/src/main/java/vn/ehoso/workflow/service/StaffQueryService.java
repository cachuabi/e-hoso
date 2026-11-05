package vn.ehoso.workflow.service;

import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.account.domain.Role;
import vn.ehoso.audit.domain.ProcessingLog;
import vn.ehoso.audit.service.AuditWriter;
import vn.ehoso.catalog.domain.DeadlineUnit;
import vn.ehoso.catalog.domain.Procedure;
import vn.ehoso.catalog.repository.ProcedureRepository;
import vn.ehoso.dashboard.service.DeadlineLabels;
import vn.ehoso.dossier.domain.Dossier;
import vn.ehoso.dossier.domain.DossierStatus;
import vn.ehoso.dossier.repository.DossierRepository;
import vn.ehoso.dossier.service.DossierAssembler;
import vn.ehoso.dossier.service.DossierViews;
import vn.ehoso.security.Actor;
import vn.ehoso.security.ApiException;

/** Staff lists and detail within the scope of each role (FR-15, FR-24, UC-13). */
@Service
public class StaffQueryService {
  private final DossierRepository dossiers;
  private final ProcedureRepository procedures;
  private final DossierAssembler assembler;
  private final DeadlineLabels labels;
  private final AuditWriter audit;
  private final Clock clock;

  /** Creates the service. */
  public StaffQueryService(
      DossierRepository dossiers,
      ProcedureRepository procedures,
      DossierAssembler assembler,
      DeadlineLabels labels,
      AuditWriter audit,
      Clock clock) {
    this.dossiers = dossiers;
    this.procedures = procedures;
    this.assembler = assembler;
    this.labels = labels;
    this.audit = audit;
    this.clock = clock;
  }

  /** A log line for staff. */
  public record LogView(
      String actorRole,
      String action,
      String fromStatus,
      String toStatus,
      String note,
      OffsetDateTime occurredAt) {}

  /** Lists dossiers, oldest submission first; officers see only their own (UC-13). */
  @Transactional(readOnly = true)
  public DossierViews.Page list(
      DossierStatus status, String label, int page, int size, Actor actor) {
    Specification<Dossier> scope =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.notEqual(root.get("status"), DossierStatus.DRAFT));
          if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
          }
          if (actor.role() == Role.CCTP) {
            predicates.add(cb.equal(root.get("assignedOfficerId"), actor.id()));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    Map<UUID, DeadlineUnit> units =
        procedures.findAll().stream()
            .collect(Collectors.toMap(Procedure::getId, Procedure::getDeadlineUnit));
    OffsetDateTime now = OffsetDateTime.now(clock);
    Function<Dossier, String> labelOf =
        d -> labels.labelOf(now, d.getDueAt(), d.getResultAt(), units.get(d.getProcedureId()));
    List<Dossier> matching =
        dossiers.findAll(scope, Sort.by("submittedAt").ascending()).stream()
            .filter(d -> label == null || label.equals(labelOf.apply(d)))
            .toList();
    List<DossierViews.Summary> items =
        matching.stream()
            .skip((long) page * size)
            .limit(size)
            .map(d -> assembler.summary(d, labelOf.apply(d)))
            .toList();
    return new DossierViews.Page(items, matching.size());
  }

  /** Reads a dossier if it is in the actor's scope (FR-05). */
  @Transactional(readOnly = true)
  public DossierViews.Detail detail(UUID id, Actor actor) {
    return view(find(id, actor));
  }

  /** Builds the response of a staff action. */
  @Transactional(readOnly = true)
  public DossierViews.Detail view(Dossier d) {
    Procedure procedure = procedures.findById(d.getProcedureId()).orElseThrow();
    String label =
        labels.labelOf(
            OffsetDateTime.now(clock), d.getDueAt(), d.getResultAt(), procedure.getDeadlineUnit());
    return assembler.detail(d, label);
  }

  /** Reads the full log (FR-23). */
  @Transactional(readOnly = true)
  public List<LogView> logs(UUID id, Actor actor) {
    Dossier dossier =
        actor.role() == Role.ADMIN
            ? dossiers.findById(id).orElseThrow(() -> ApiException.notFound("Không có hồ sơ."))
            : find(id, actor);
    return audit.read(dossier.getId()).stream().map(this::logView).toList();
  }

  private LogView logView(ProcessingLog l) {
    return new LogView(
        l.getActorRole(),
        l.getAction(),
        l.getFromStatus(),
        l.getToStatus(),
        l.getNote(),
        l.getOccurredAt());
  }

  private Dossier find(UUID id, Actor actor) {
    Dossier dossier =
        dossiers.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ."));
    if (dossier.getStatus() == DossierStatus.DRAFT
        || (actor.role() == Role.CCTP && !actor.id().equals(dossier.getAssignedOfficerId()))) {
      throw ApiException.forbidden();
    }
    return dossier;
  }
}
