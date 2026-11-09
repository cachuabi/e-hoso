package vn.ehoso.workflow.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ehoso.dossier.domain.DossierStatus;
import vn.ehoso.dossier.service.DossierViews;
import vn.ehoso.security.Actor;
import vn.ehoso.workflow.service.ApprovalService;
import vn.ehoso.workflow.service.IntakeService;
import vn.ehoso.workflow.service.ReviewService;
import vn.ehoso.workflow.service.StaffQueryService;

/** Staff endpoints. */
@RestController
@RequestMapping("/api/v1/staff")
class WorkflowController {
  private final IntakeService intake;
  private final ReviewService review;
  private final ApprovalService approval;
  private final StaffQueryService query;

  WorkflowController(
      IntakeService intake,
      ReviewService review,
      ApprovalService approval,
      StaffQueryService query) {
    this.intake = intake;
    this.review = review;
    this.approval = approval;
    this.query = query;
  }

  record AcceptRequest(boolean originalChecked, long version) {}

  record SupplementBody(String instruction, List<IntakeService.MissingItem> items, long version) {}

  record ReasonBody(String reason, long version) {}

  record AssignBody(@NotNull UUID officerId, long version) {}

  record ReturnBody(String method, long version) {}

  record VerificationBody(String place, long version) {}

  record SubmitBody(String opinion, String resultKind, long version) {}

  record ApproveBody(String password, long version) {}

  @GetMapping("/dossiers")
  DossierViews.Page list(
      @RequestParam(required = false) DossierStatus status,
      @RequestParam(required = false) String deadlineLabel,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal Actor actor) {
    return query.list(status, deadlineLabel, page, Math.min(size, 100), actor);
  }

  @GetMapping("/dossiers/{id}")
  DossierViews.Detail get(@PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
    return query.detail(id, actor);
  }

  @GetMapping("/dossiers/{id}/logs")
  List<StaffQueryService.LogView> logs(
      @PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
    return query.logs(id, actor);
  }

  @GetMapping("/officers")
  List<IntakeService.OfficerLoad> officers() {
    return intake.officers();
  }

  @PostMapping("/dossiers/{id}/accept")
  DossierViews.Detail accept(
      @PathVariable UUID id,
      @RequestBody AcceptRequest body,
      @AuthenticationPrincipal Actor actor) {
    return query.view(intake.accept(id, body.originalChecked(), body.version(), actor));
  }

  @PostMapping("/dossiers/{id}/request-supplement")
  DossierViews.Detail requestSupplement(
      @PathVariable UUID id,
      @RequestBody SupplementBody body,
      @AuthenticationPrincipal Actor actor) {
    return query.view(
        intake.requestSupplement(id, body.instruction(), body.items(), body.version(), actor));
  }

  @PostMapping("/dossiers/{id}/reject")
  DossierViews.Detail reject(
      @PathVariable UUID id, @RequestBody ReasonBody body, @AuthenticationPrincipal Actor actor) {
    return query.view(intake.reject(id, body.reason(), body.version(), actor));
  }

  @PostMapping("/dossiers/{id}/assign")
  DossierViews.Detail assign(
      @PathVariable UUID id,
      @Valid @RequestBody AssignBody body,
      @AuthenticationPrincipal Actor actor) {
    return query.view(intake.assign(id, body.officerId(), body.version(), actor));
  }

  @PostMapping("/dossiers/{id}/return-result")
  DossierViews.Detail returnResult(
      @PathVariable UUID id, @RequestBody ReturnBody body, @AuthenticationPrincipal Actor actor) {
    return query.view(intake.returnResult(id, body.method(), body.version(), actor));
  }

  @PostMapping("/dossiers/{id}/verification")
  DossierViews.Detail verification(
      @PathVariable UUID id,
      @RequestBody VerificationBody body,
      @AuthenticationPrincipal Actor actor) {
    return query.view(review.markVerification(id, body.place(), body.version(), actor));
  }

  @PostMapping("/dossiers/{id}/submit-approval")
  DossierViews.Detail submitApproval(
      @PathVariable UUID id, @RequestBody SubmitBody body, @AuthenticationPrincipal Actor actor) {
    return query.view(
        review.submitForApproval(id, body.opinion(), body.resultKind(), body.version(), actor));
  }

  @PostMapping("/dossiers/{id}/approve")
  DossierViews.Detail approve(
      @PathVariable UUID id, @RequestBody ApproveBody body, @AuthenticationPrincipal Actor actor) {
    return query.view(approval.approve(id, body.password(), body.version(), actor));
  }

  @PostMapping("/dossiers/{id}/send-back")
  DossierViews.Detail sendBack(
      @PathVariable UUID id, @RequestBody ReasonBody body, @AuthenticationPrincipal Actor actor) {
    return query.view(approval.sendBack(id, body.reason(), body.version(), actor));
  }
}
