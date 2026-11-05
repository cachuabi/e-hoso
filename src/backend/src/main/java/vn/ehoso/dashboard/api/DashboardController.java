package vn.ehoso.dashboard.api;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ehoso.dashboard.service.DashboardService;

/** Leader dashboard endpoints. */
@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController {
  private final DashboardService service;

  DashboardController(DashboardService service) {
    this.service = service;
  }

  @GetMapping
  DashboardService.Dashboard get(@RequestParam LocalDate from, @RequestParam LocalDate to) {
    return service.dashboard(from, to);
  }

  @GetMapping("/export.csv")
  ResponseEntity<byte[]> export(@RequestParam LocalDate from, @RequestParam LocalDate to) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dashboard.csv\"")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(service.exportCsv(from, to).getBytes(StandardCharsets.UTF_8));
  }
}
