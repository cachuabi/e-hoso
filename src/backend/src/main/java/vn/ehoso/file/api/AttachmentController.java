package vn.ehoso.file.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.ehoso.file.domain.Attachment;
import vn.ehoso.file.domain.AttachmentKind;
import vn.ehoso.file.service.AttachmentService;
import vn.ehoso.security.Actor;

/** File endpoints. */
@RestController
@RequestMapping("/api/v1")
class AttachmentController {
  private final AttachmentService service;

  AttachmentController(AttachmentService service) {
    this.service = service;
  }

  record AttachmentView(
      UUID id, UUID componentId, String fileName, String mimeType, long sizeBytes, String kind) {}

  @PostMapping("/dossiers/{id}/attachments")
  @ResponseStatus(HttpStatus.CREATED)
  AttachmentView upload(
      @PathVariable UUID id,
      @RequestParam MultipartFile file,
      @RequestParam AttachmentKind kind,
      @RequestParam(required = false) UUID componentId,
      @AuthenticationPrincipal Actor actor) {
    Attachment a = service.upload(id, componentId, kind, file, actor);
    return new AttachmentView(
        a.getId(),
        a.getComponentId(),
        a.getFileName(),
        a.getMimeType(),
        a.getSizeBytes(),
        a.getKind().name());
  }

  @DeleteMapping("/attachments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
    service.delete(id, actor);
  }

  @GetMapping("/attachments/{id}/download-url")
  AttachmentService.SignedUrl downloadUrl(
      @PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
    return service.downloadUrl(id, actor);
  }
}
