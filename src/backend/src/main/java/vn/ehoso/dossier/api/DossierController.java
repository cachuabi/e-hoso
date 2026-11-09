package vn.ehoso.dossier.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.ehoso.dossier.domain.DossierStatus;
import vn.ehoso.dossier.service.DossierService;
import vn.ehoso.dossier.service.DossierViews;
import vn.ehoso.security.Actor;

/** Citizen dossier endpoints and public lookup. */
@RestController
@RequestMapping("/api/v1")
class DossierController {
  private final DossierService service;

  DossierController(DossierService service) {
    this.service = service;
  }

  record DraftRequest(@NotBlank String procedureCode, Map<String, Object> formData) {}

  record FormUpdate(@NotNull Map<String, Object> formData, long version) {}

  record LookupRequest(@NotBlank String code, @NotBlank String nationalIdLast4) {}

  @GetMapping("/dossiers")
  DossierViews.Page list(
      @RequestParam(required = false) DossierStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal Actor actor) {
    return service.listMine(status, page, Math.min(size, 100), actor);
  }

  @PostMapping("/dossiers")
  @ResponseStatus(HttpStatus.CREATED)
  DossierViews.Detail create(
      @Valid @RequestBody DraftRequest body, @AuthenticationPrincipal Actor actor) {
    return service.createDraft(body.procedureCode(), body.formData(), actor);
  }

  @GetMapping("/dossiers/{id}")
  DossierViews.Detail get(@PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
    return service.getMine(id, actor);
  }

  @PutMapping("/dossiers/{id}")
  DossierViews.Detail update(
      @PathVariable UUID id,
      @Valid @RequestBody FormUpdate body,
      @AuthenticationPrincipal Actor actor) {
    return service.updateForm(id, body.formData(), body.version(), actor);
  }

  @PostMapping("/dossiers/{id}/submit")
  DossierViews.Detail submit(@PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
    return service.submit(id, actor);
  }

  @PostMapping("/dossiers/{id}/resubmit")
  DossierViews.Detail resubmit(@PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
    return service.resubmit(id, actor);
  }

  @PostMapping("/dossiers/{id}/withdraw")
  DossierViews.Detail withdraw(@PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
    return service.withdraw(id, actor);
  }

  @GetMapping("/dossiers/{id}/history")
  List<DossierViews.StatusEvent> history(
      @PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
    return service.history(id, actor);
  }

  @PostMapping("/public/lookup")
  DossierViews.PublicStatus lookup(
      @Valid @RequestBody LookupRequest body, HttpServletRequest request) {
    return service.lookup(body.code(), body.nationalIdLast4(), request.getRemoteAddr());
  }
}
