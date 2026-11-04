package vn.ehoso.dashboard.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.repository.UserRepository;
import vn.ehoso.catalog.domain.Procedure;
import vn.ehoso.catalog.repository.ProcedureRepository;
import vn.ehoso.dossier.domain.Dossier;
import vn.ehoso.dossier.repository.DossierRepository;
import vn.ehoso.security.ApiException;

/** Figures for the leader (FR-25, FR-26, BR-07; time basis per V-03). */
@Service
public class DashboardService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private final DossierRepository dossiers;
  private final ProcedureRepository procedures;
  private final UserRepository users;
  private final Clock clock;

  /** Creates the service. */
  public DashboardService(
      DossierRepository dossiers,
      ProcedureRepository procedures,
      UserRepository users,
      Clock clock) {
    this.dossiers = dossiers;
    this.procedures = procedures;
    this.users = users;
    this.clock = clock;
  }

  /** On-time figures of one procedure or officer. */
  public record OnTimeStat(String name, int onTime, int late, Double onTimeRate) {}

  /** The whole dashboard. */
  public record Dashboard(
      Map<String, Long> byStatus,
      List<OnTimeStat> byProcedure,
      List<OnTimeStat> byOfficer,
      long overdueNow) {}

  /** Computes the dashboard for an inclusive date range. */
  @Transactional(readOnly = true)
  public Dashboard dashboard(LocalDate from, LocalDate to) {
    if (from.isAfter(to)) {
      throw ApiException.invalidField("from", "Khoảng thời gian không hợp lệ.");
    }
    OffsetDateTime start = from.atStartOfDay(ZONE).toOffsetDateTime();
    OffsetDateTime end = to.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
    Map<String, Long> byStatus = new TreeMap<>();
    for (Object[] row : dossiers.countByStatus(start, end)) {
      byStatus.put(row[0].toString(), ((Number) row[1]).longValue());
    }
    List<Dossier> finished =
        dossiers.findByResultAtGreaterThanEqualAndResultAtLessThan(start, end);
    Map<UUID, String> procedureNames =
        procedures.findAll().stream().collect(Collectors.toMap(Procedure::getId, Procedure::getName));
    Map<UUID, String> officerNames =
        users.findAll().stream().collect(Collectors.toMap(User::getId, User::getFullName));
    return new Dashboard(
        byStatus,
        stats(finished, d -> procedureNames.get(d.getProcedureId())),
        stats(finished, d -> officerNames.getOrDefault(d.getAssignedOfficerId(), "?")),
        dossiers.countByDueAtBeforeAndResultAtIsNull(OffsetDateTime.now(clock)));
  }

  /** Exports the dashboard as UTF-8 CSV with a byte order mark for spreadsheets (FR-26). */
  @Transactional(readOnly = true)
  public String exportCsv(LocalDate from, LocalDate to) {
    Dashboard data = dashboard(from, to);
    List<String> lines = new ArrayList<>();
    lines.add("Nhóm,Tên,Đúng hạn,Trễ hạn,Tỷ lệ đúng hạn");
    data.byProcedure().forEach(s -> lines.add(row("Thủ tục", s)));
    data.byOfficer().forEach(s -> lines.add(row("Công chức", s)));
    data.byStatus().forEach((k, v) -> lines.add("Trạng thái," + k + "," + v + ",,"));
    lines.add("Đang quá hạn,," + data.overdueNow() + ",,");
    return "﻿" + String.join("\r\n", lines) + "\r\n";
  }

  private String row(String group, OnTimeStat s) {
    String name = "\"" + s.name().replace("\"", "\"\"") + "\"";
    String rate = s.onTimeRate() == null ? "" : String.format("%.1f%%", s.onTimeRate() * 100);
    return group + "," + name + "," + s.onTime() + "," + s.late() + "," + rate;
  }

  private List<OnTimeStat> stats(List<Dossier> finished, Function<Dossier, String> key) {
    Map<String, List<Dossier>> groups =
        finished.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList()));
    return groups.entrySet().stream()
        .map(
            e -> {
              int onTime =
                  (int) e.getValue().stream().filter(DashboardService::isOnTime).count();
              int late = e.getValue().size() - onTime;
              return new OnTimeStat(e.getKey(), onTime, late, (double) onTime / (onTime + late));
            })
        .toList();
  }

  private static boolean isOnTime(Dossier d) {
    return d.getDueAt() != null && !d.getResultAt().isAfter(d.getDueAt());
  }
}
