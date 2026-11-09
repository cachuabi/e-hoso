package vn.ehoso.catalog.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.audit.service.AdminAudit;
import vn.ehoso.catalog.domain.DeadlineUnit;
import vn.ehoso.catalog.domain.DossierComponent;
import vn.ehoso.catalog.domain.Holiday;
import vn.ehoso.catalog.domain.Procedure;
import vn.ehoso.catalog.repository.DossierComponentRepository;
import vn.ehoso.catalog.repository.HolidayRepository;
import vn.ehoso.catalog.repository.ProcedureRepository;
import vn.ehoso.security.Actor;
import vn.ehoso.security.ApiException;

/** Procedure catalog and calendar configuration (FR-06, FR-27, FR-28). */
@Service
public class CatalogService {
  private final ProcedureRepository procedures;
  private final DossierComponentRepository components;
  private final HolidayRepository holidays;
  private final AdminAudit audit;

  /** Creates the service. */
  public CatalogService(
      ProcedureRepository procedures,
      DossierComponentRepository components,
      HolidayRepository holidays,
      AdminAudit audit) {
    this.procedures = procedures;
    this.components = components;
    this.holidays = holidays;
    this.audit = audit;
  }

  /** A component as shown to clients. */
  public record ComponentView(UUID id, String name, String requirement, List<String> allowedTypes) {
    static ComponentView of(DossierComponent c) {
      return new ComponentView(
          c.getId(), c.getName(), c.getRequirement(), List.of(c.getAllowedTypes().split(",")));
    }
  }

  /** A procedure as shown to clients. */
  public record ProcedureView(
      String code,
      String name,
      DeadlineUnit deadlineUnit,
      Integer deadlineDays,
      boolean applies15hCutoff,
      boolean allowsVerification,
      boolean requiresOriginalCheck,
      String documentsToPresent,
      Map<String, Object> formSchema,
      List<ComponentView> components) {}

  /** Editable configuration of a procedure (FR-27). */
  public record ProcedureConfig(
      DeadlineUnit deadlineUnit, Integer deadlineDays, boolean applies15hCutoff) {}

  /** Lists all procedures. */
  @Transactional(readOnly = true)
  public List<ProcedureView> list() {
    return procedures.findAll().stream().map(this::view).toList();
  }

  /** Reads one procedure. */
  @Transactional(readOnly = true)
  public ProcedureView get(String code) {
    return view(find(code));
  }

  /** Changes the time limit of a procedure; applies to dossiers accepted afterwards (UC-16). */
  @Transactional
  public ProcedureView configure(String code, ProcedureConfig config, Actor actor) {
    Procedure procedure = find(code);
    if (config.deadlineUnit() == DeadlineUnit.WORKING_DAYS
        && (config.deadlineDays() == null || config.deadlineDays() <= 0)) {
      throw ApiException.invalidField("deadlineDays", "Số ngày phải lớn hơn 0.");
    }
    procedure.setDeadlineUnit(config.deadlineUnit());
    procedure.setDeadlineDays(
        config.deadlineUnit() == DeadlineUnit.SAME_DAY ? null : config.deadlineDays());
    procedure.setApplies15hCutoff(config.applies15hCutoff());
    audit.record(actor.id(), "CONFIGURE_PROCEDURE", null, code);
    return view(procedure);
  }

  /** Lists the calendar. */
  @Transactional(readOnly = true)
  public List<Holiday> holidays() {
    return holidays.findAllByOrderByDayAsc();
  }

  /** Adds or replaces a calendar day (FR-28). */
  @Transactional
  public Holiday addHoliday(LocalDate day, String type, String description, Actor actor) {
    if (!Holiday.HOLIDAY.equals(type) && !Holiday.MAKEUP.equals(type)) {
      throw ApiException.invalidField("type", "Loại ngày không hợp lệ.");
    }
    Holiday holiday = new Holiday();
    holiday.setDay(day);
    holiday.setType(type);
    holiday.setDescription(description);
    audit.record(actor.id(), "ADD_HOLIDAY", null, day.toString());
    return holidays.save(holiday);
  }

  /** Removes a calendar day (FR-28). */
  @Transactional
  public void deleteHoliday(LocalDate day, Actor actor) {
    holidays.deleteById(day);
    audit.record(actor.id(), "DELETE_HOLIDAY", null, day.toString());
  }

  private Procedure find(String code) {
    return procedures
        .findByCode(code)
        .orElseThrow(() -> ApiException.notFound("Không có thủ tục."));
  }

  private ProcedureView view(Procedure p) {
    return new ProcedureView(
        p.getCode(),
        p.getName(),
        p.getDeadlineUnit(),
        p.getDeadlineDays(),
        p.isApplies15hCutoff(),
        p.isAllowsVerification(),
        p.isRequiresOriginalCheck(),
        p.getDocumentsToPresent(),
        p.getFormSchema(),
        components.findByProcedureIdOrderBySortOrderAsc(p.getId()).stream()
            .map(ComponentView::of)
            .toList());
  }
}
