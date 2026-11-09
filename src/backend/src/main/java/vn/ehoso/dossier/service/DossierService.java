package vn.ehoso.dossier.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.repository.UserRepository;
import vn.ehoso.audit.domain.ProcessingLog;
import vn.ehoso.audit.service.AuditWriter;
import vn.ehoso.catalog.domain.DossierComponent;
import vn.ehoso.catalog.domain.Procedure;
import vn.ehoso.catalog.repository.DossierComponentRepository;
import vn.ehoso.catalog.repository.ProcedureRepository;
import vn.ehoso.dossier.domain.Dossier;
import vn.ehoso.dossier.domain.DossierAction;
import vn.ehoso.dossier.domain.DossierStatus;
import vn.ehoso.dossier.repository.DossierRepository;
import vn.ehoso.file.domain.Attachment;
import vn.ehoso.file.domain.AttachmentKind;
import vn.ehoso.file.repository.AttachmentRepository;
import vn.ehoso.security.Actor;
import vn.ehoso.security.ApiException;
import vn.ehoso.security.ApiException.FieldProblem;

/** The citizen side of a dossier (FR-07, FR-09 to FR-13). */
@Service
public class DossierService {
  private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final DossierRepository dossiers;
  private final ProcedureRepository procedures;
  private final DossierComponentRepository components;
  private final AttachmentRepository attachments;
  private final UserRepository users;
  private final DossierLifecycle lifecycle;
  private final FormValidator formValidator;
  private final DossierAssembler assembler;
  private final AuditWriter audit;
  private final LookupThrottle throttle;
  private final Clock clock;

  /** Creates the service. */
  public DossierService(
      DossierRepository dossiers,
      ProcedureRepository procedures,
      DossierComponentRepository components,
      AttachmentRepository attachments,
      UserRepository users,
      DossierLifecycle lifecycle,
      FormValidator formValidator,
      DossierAssembler assembler,
      AuditWriter audit,
      LookupThrottle throttle,
      Clock clock) {
    this.dossiers = dossiers;
    this.procedures = procedures;
    this.components = components;
    this.attachments = attachments;
    this.users = users;
    this.lifecycle = lifecycle;
    this.formValidator = formValidator;
    this.assembler = assembler;
    this.audit = audit;
    this.throttle = throttle;
    this.clock = clock;
  }

  /** Creates a draft (FR-07). */
  @Transactional
  public DossierViews.Detail createDraft(
      String procedureCode, Map<String, Object> formData, Actor actor) {
    Procedure procedure =
        procedures
            .findByCode(procedureCode)
            .orElseThrow(() -> ApiException.invalidField("procedureCode", "Không có thủ tục."));
    Dossier dossier = new Dossier();
    dossier.setProcedureId(procedure.getId());
    dossier.setApplicantId(actor.id());
    dossier.setFormData(formData == null ? new HashMap<>() : formData);
    dossier.setCreatedAt(now());
    dossiers.saveAndFlush(dossier);
    return assembler.detail(dossier, null);
  }

  /** Updates the form while the dossier is a draft or awaits a supplement (FR-07, FR-11). */
  @Transactional
  public DossierViews.Detail updateForm(
      UUID id, Map<String, Object> formData, long version, Actor actor) {
    Dossier dossier = owned(id, actor);
    requireVersion(dossier, version);
    if (dossier.getStatus() != DossierStatus.DRAFT
        && dossier.getStatus() != DossierStatus.NEED_SUPPLEMENT) {
      lifecycle.check(dossier, DossierAction.SUBMIT);
    }
    dossier.setFormData(formData);
    dossiers.saveAndFlush(dossier);
    return assembler.detail(dossier, null);
  }

  /** Submits a draft and issues the reference code (FR-09). */
  @Transactional
  public DossierViews.Detail submit(UUID id, Actor actor) {
    Dossier dossier = owned(id, actor);
    lifecycle.check(dossier, DossierAction.SUBMIT);
    requireComplete(dossier);
    dossier.setReferenceCode(newReferenceCode());
    dossier.setSubmittedAt(now());
    lifecycle.apply(dossier, DossierAction.SUBMIT, actor, null, null);
    dossiers.saveAndFlush(dossier);
    return assembler.detail(dossier, null);
  }

  /** Submits again after a supplement (FR-11); the reference code stays. */
  @Transactional
  public DossierViews.Detail resubmit(UUID id, Actor actor) {
    Dossier dossier = owned(id, actor);
    lifecycle.check(dossier, DossierAction.RESUBMIT);
    requireComplete(dossier);
    dossier.setSubmittedAt(now());
    lifecycle.apply(dossier, DossierAction.RESUBMIT, actor, null, null);
    dossiers.saveAndFlush(dossier);
    return assembler.detail(dossier, null);
  }

  /** Withdraws a dossier (FR-10, BR-04). */
  @Transactional
  public DossierViews.Detail withdraw(UUID id, Actor actor) {
    Dossier dossier = owned(id, actor);
    lifecycle.apply(dossier, DossierAction.WITHDRAW, actor, null, null);
    dossiers.saveAndFlush(dossier);
    return assembler.detail(dossier, null);
  }

  /** Lists the citizen's dossiers, optionally by status (FR-13). */
  @Transactional(readOnly = true)
  public DossierViews.Page listMine(DossierStatus status, int page, int size, Actor actor) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    org.springframework.data.domain.Page<Dossier> result =
        status == null
            ? dossiers.findByApplicantId(actor.id(), pageable)
            : dossiers.findByApplicantIdAndStatus(actor.id(), status, pageable);
    return new DossierViews.Page(
        result.stream().map(d -> assembler.summary(d, null)).toList(), result.getTotalElements());
  }

  /** Reads one dossier of the citizen (FR-13). */
  @Transactional(readOnly = true)
  public DossierViews.Detail getMine(UUID id, Actor actor) {
    return assembler.detail(owned(id, actor), null);
  }

  /** Returns status milestones without staff notes (SRS section 3.7). */
  @Transactional(readOnly = true)
  public List<DossierViews.StatusEvent> history(UUID id, Actor actor) {
    return milestones(owned(id, actor).getId());
  }

  /** Public lookup: the same answer for a wrong code and wrong digits (FR-12, UC-07). */
  @Transactional(readOnly = true)
  public DossierViews.PublicStatus lookup(String code, String lastFour, String address) {
    if (throttle.isBlocked(address)) {
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS, "LOOKUP_BLOCKED", "Vui lòng thử lại sau 10 phút.");
    }
    Optional<Dossier> found = findByCode(code).filter(d -> matches(d, lastFour));
    if (found.isEmpty()) {
      throttle.recordFailure(address);
      throw ApiException.notFound("Không tìm thấy hồ sơ.");
    }
    Dossier dossier = found.get();
    Procedure procedure = procedures.findById(dossier.getProcedureId()).orElseThrow();
    return new DossierViews.PublicStatus(
        procedure.getName(), dossier.getStatus(), milestones(dossier.getId()), dossier.getDueAt());
  }

  private List<DossierViews.StatusEvent> milestones(UUID dossierId) {
    List<DossierViews.StatusEvent> events = new ArrayList<>();
    for (ProcessingLog log : audit.read(dossierId)) {
      if (!log.getToStatus().equals(log.getFromStatus())) {
        events.add(
            new DossierViews.StatusEvent(
                DossierStatus.valueOf(log.getToStatus()), log.getOccurredAt()));
      }
    }
    return events;
  }

  private Optional<Dossier> findByCode(String code) {
    if (code == null) {
      return Optional.empty();
    }
    String normalized = code.trim().toUpperCase();
    return normalized.startsWith("TC-")
        ? dossiers.findByReferenceCode(normalized)
        : dossiers.findByDossierCode(normalized);
  }

  private boolean matches(Dossier dossier, String lastFour) {
    return lastFour != null
        && users
            .findById(dossier.getApplicantId())
            .map(User::getNationalId)
            .filter(n -> n.endsWith(lastFour) && lastFour.length() == 4)
            .isPresent();
  }

  private void requireComplete(Dossier dossier) {
    Procedure procedure = procedures.findById(dossier.getProcedureId()).orElseThrow();
    List<FieldProblem> problems =
        new ArrayList<>(formValidator.validate(procedure.getFormSchema(), dossier.getFormData()));
    Set<UUID> present =
        attachments.findByDossierIdOrderByCreatedAtAsc(dossier.getId()).stream()
            .filter(a -> a.getKind() == AttachmentKind.DOSSIER)
            .map(Attachment::getComponentId)
            .collect(Collectors.toSet());
    for (DossierComponent c : components.findByProcedureIdOrderBySortOrderAsc(procedure.getId())) {
      if ("REQUIRED".equals(c.getRequirement()) && !present.contains(c.getId())) {
        problems.add(new FieldProblem("component:" + c.getId(), "Thiếu " + c.getName() + "."));
      }
    }
    if (!problems.isEmpty()) {
      throw ApiException.invalidFields(problems);
    }
  }

  private Dossier owned(UUID id, Actor actor) {
    return dossiers
        .findById(id)
        .filter(d -> d.getApplicantId().equals(actor.id()))
        .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ."));
  }

  private void requireVersion(Dossier dossier, long version) {
    if (dossier.getVersion() != version) {
      throw ApiException.conflict("VERSION_CONFLICT", "Hồ sơ vừa được thay đổi, vui lòng tải lại.");
    }
  }

  private String newReferenceCode() {
    String code;
    do {
      StringBuilder builder = new StringBuilder("TC-");
      for (int i = 0; i < 8; i++) {
        builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
      }
      code = builder.toString();
    } while (dossiers.existsByReferenceCode(code));
    return code;
  }

  private OffsetDateTime now() {
    return OffsetDateTime.now(clock);
  }
}
