package vn.ehoso.workflow.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.account.domain.Role;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.domain.UserStatus;
import vn.ehoso.account.repository.UserRepository;
import vn.ehoso.catalog.domain.DossierComponent;
import vn.ehoso.catalog.domain.Procedure;
import vn.ehoso.catalog.repository.DossierComponentRepository;
import vn.ehoso.catalog.repository.ProcedureRepository;
import vn.ehoso.dossier.domain.Dossier;
import vn.ehoso.dossier.domain.DossierAction;
import vn.ehoso.dossier.domain.DossierStatus;
import vn.ehoso.dossier.domain.SupplementItem;
import vn.ehoso.dossier.domain.SupplementRequest;
import vn.ehoso.dossier.repository.DossierRepository;
import vn.ehoso.dossier.repository.SupplementItemRepository;
import vn.ehoso.dossier.repository.SupplementRequestRepository;
import vn.ehoso.dossier.service.DossierLifecycle;
import vn.ehoso.security.Actor;
import vn.ehoso.security.ApiException;

/** One-stop officer actions (FR-16, FR-17, FR-18, FR-22, BR-09). */
@Service
public class IntakeService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final DossierRepository dossiers;
  private final ProcedureRepository procedures;
  private final DossierComponentRepository components;
  private final SupplementRequestRepository supplements;
  private final SupplementItemRepository supplementItems;
  private final UserRepository users;
  private final DossierLifecycle lifecycle;
  private final DeadlineCalculator deadlines;
  private final DossierCodeGenerator codes;
  private final Clock clock;

  /** Creates the service. */
  public IntakeService(
      DossierRepository dossiers,
      ProcedureRepository procedures,
      DossierComponentRepository components,
      SupplementRequestRepository supplements,
      SupplementItemRepository supplementItems,
      UserRepository users,
      DossierLifecycle lifecycle,
      DeadlineCalculator deadlines,
      DossierCodeGenerator codes,
      Clock clock) {
    this.dossiers = dossiers;
    this.procedures = procedures;
    this.components = components;
    this.supplements = supplements;
    this.supplementItems = supplementItems;
    this.users = users;
    this.lifecycle = lifecycle;
    this.deadlines = deadlines;
    this.codes = codes;
    this.clock = clock;
  }

  /** A missing component named by the officer. */
  public record MissingItem(UUID componentId, String note) {}

  /** A candidate officer and their current load (FR-18). */
  public record OfficerLoad(UUID id, String fullName, long activeDossiers) {}

  /** Accepts a dossier: code, due date, status, log, email in one transaction (FR-17). */
  @Transactional
  public Dossier accept(UUID id, boolean originalChecked, long version, Actor actor) {
    Dossier dossier = StaffAccess.find(dossiers, id, version);
    lifecycle.check(dossier, DossierAction.ACCEPT);
    Procedure procedure = procedures.findById(dossier.getProcedureId()).orElseThrow();
    if (procedure.isRequiresOriginalCheck() && !originalChecked) {
      throw ApiException.invalid("ORIGINAL_NOT_CHECKED", "Cần xác nhận đã đối chiếu bản chính.");
    }
    OffsetDateTime now = OffsetDateTime.now(clock);
    dossier.setOriginalChecked(originalChecked);
    dossier.setAcceptedAt(now);
    dossier.setDueAt(deadlines.dueAt(procedure, now));
    dossier.setDossierCode(codes.next(now.atZoneSameInstant(ZONE).toLocalDate()));
    lifecycle.apply(
        dossier, DossierAction.ACCEPT, actor, null, dossier.getDueAt().toString());
    return dossiers.saveAndFlush(dossier);
  }

  /** Asks for a supplement listing every missing component at once (FR-16, BR-09). */
  @Transactional
  public Dossier requestSupplement(
      UUID id, String instruction, List<MissingItem> items, long version, Actor actor) {
    Dossier dossier = StaffAccess.find(dossiers, id, version);
    lifecycle.check(dossier, DossierAction.REQUEST_SUPPLEMENT);
    if (items == null || items.isEmpty()) {
      throw ApiException.invalidField("items", "Cần chọn ít nhất một thành phần thiếu.");
    }
    if (instruction == null || instruction.isBlank()) {
      throw ApiException.invalidField("instruction", "Cần ghi hướng dẫn bổ sung.");
    }
    SupplementRequest request = new SupplementRequest();
    request.setDossierId(id);
    request.setInstruction(instruction);
    request.setRequestedBy(actor.id());
    request.setCreatedAt(OffsetDateTime.now(clock));
    supplements.save(request);
    for (MissingItem item : items) {
      DossierComponent component =
          components
              .findById(item.componentId())
              .filter(c -> c.getProcedureId().equals(dossier.getProcedureId()))
              .orElseThrow(() -> ApiException.invalidField("items", "Thành phần không hợp lệ."));
      SupplementItem missing = new SupplementItem();
      missing.setRequestId(request.getId());
      missing.setComponentId(component.getId());
      missing.setNote(item.note());
      supplementItems.save(missing);
    }
    dossier.setSupplementCount(dossier.getSupplementCount() + 1);
    lifecycle.apply(dossier, DossierAction.REQUEST_SUPPLEMENT, actor, instruction, instruction);
    return dossiers.saveAndFlush(dossier);
  }

  /** Refuses a dossier; a reason is mandatory (FR-16). */
  @Transactional
  public Dossier reject(UUID id, String reason, long version, Actor actor) {
    Dossier dossier = StaffAccess.find(dossiers, id, version);
    StaffAccess.requireReason(reason);
    lifecycle.apply(dossier, DossierAction.REJECT, actor, reason, reason);
    return dossiers.saveAndFlush(dossier);
  }

  /** Assigns an active officer (FR-18). */
  @Transactional
  public Dossier assign(UUID id, UUID officerId, long version, Actor actor) {
    Dossier dossier = StaffAccess.find(dossiers, id, version);
    lifecycle.check(dossier, DossierAction.ASSIGN);
    User officer =
        users
            .findById(officerId)
            .filter(u -> u.getRole() == Role.CCTP && u.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(
                () -> ApiException.invalid("OFFICER_UNAVAILABLE", "Công chức không hoạt động."));
    dossier.setAssignedOfficerId(officer.getId());
    lifecycle.apply(dossier, DossierAction.ASSIGN, actor, officer.getFullName(), null);
    return dossiers.saveAndFlush(dossier);
  }

  /** Hands over the result and records how (FR-22). */
  @Transactional
  public Dossier returnResult(UUID id, String method, long version, Actor actor) {
    Dossier dossier = StaffAccess.find(dossiers, id, version);
    if (!"COUNTER".equals(method) && !"ELECTRONIC".equals(method)) {
      throw ApiException.invalidField("method", "Hình thức trả không hợp lệ.");
    }
    lifecycle.check(dossier, DossierAction.RETURN_RESULT);
    dossier.setReturnMethod(method);
    dossier.setReturnedAt(OffsetDateTime.now(clock));
    lifecycle.apply(dossier, DossierAction.RETURN_RESULT, actor, method, null);
    return dossiers.saveAndFlush(dossier);
  }

  /** Lists active officers with the number of dossiers in progress (FR-18). */
  @Transactional(readOnly = true)
  public List<OfficerLoad> officers() {
    List<DossierStatus> active =
        List.of(DossierStatus.IN_PROGRESS, DossierStatus.PENDING_APPROVAL);
    return users.findByRoleAndStatus(Role.CCTP, UserStatus.ACTIVE).stream()
        .map(
            u ->
                new OfficerLoad(
                    u.getId(),
                    u.getFullName(),
                    dossiers.countByAssignedOfficerIdAndStatusIn(u.getId(), active)))
        .toList();
  }
}
