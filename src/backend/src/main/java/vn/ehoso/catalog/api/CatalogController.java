package vn.ehoso.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ehoso.catalog.domain.Holiday;
import vn.ehoso.catalog.service.CatalogService;
import vn.ehoso.security.Actor;

/** Public catalog and its administration. */
@RestController
@RequestMapping("/api/v1")
class CatalogController {
  private final CatalogService service;

  CatalogController(CatalogService service) {
    this.service = service;
  }

  record HolidayRequest(@NotNull LocalDate day, @NotBlank String type, String description) {}

  @GetMapping("/procedures")
  List<CatalogService.ProcedureView> list() {
    return service.list();
  }

  @GetMapping("/procedures/{code}")
  CatalogService.ProcedureView get(@PathVariable String code) {
    return service.get(code);
  }

  @PutMapping("/admin/procedures/{code}")
  CatalogService.ProcedureView configure(
      @PathVariable String code,
      @RequestBody CatalogService.ProcedureConfig config,
      @AuthenticationPrincipal Actor actor) {
    return service.configure(code, config, actor);
  }

  @GetMapping("/admin/holidays")
  List<Holiday> holidays() {
    return service.holidays();
  }

  @PostMapping("/admin/holidays")
  @ResponseStatus(HttpStatus.CREATED)
  Holiday addHoliday(
      @Valid @RequestBody HolidayRequest body, @AuthenticationPrincipal Actor actor) {
    return service.addHoliday(body.day(), body.type(), body.description(), actor);
  }

  @DeleteMapping("/admin/holidays/{day}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteHoliday(@PathVariable LocalDate day, @AuthenticationPrincipal Actor actor) {
    service.deleteHoliday(day, actor);
  }
}
