#!/usr/bin/env python3
"""Weekly project metrics for e-HoSo (Project Plan, slide 8), standard library only.

  python3 tools/metrics.py --repo OWNER/NAME [--jacoco FILE] [--out FILE]

Reads milestones, issues and pull requests through the gh CLI, the requirement catalog
and its baseline tag (srs-v1.0) through git, and JaCoCo's CSV report if it exists.
Writes a one-page Markdown report. Metrics without data say so instead of guessing.

Issue fields read from the body (issue forms and generated requirement issues):
  ### Ước lượng (giờ)   ### Thực tế (giờ)   ### Story points   ### Mức độ nghiêm trọng
"""

from __future__ import annotations

import argparse
import csv
import io
import json
import re
import statistics
import subprocess
import sys
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import req  # noqa: E402

PROJECT_START = datetime(2026, 10, 26, tzinfo=timezone.utc)
BASELINE_TAG = "srs-v1.0"
WEEK = timedelta(days=7)
# Warning thresholds from the Project Plan (slide 8).
SCHEDULE_LAG = 0.10
EFFORT_DEVIATION = 0.20
VELOCITY_DROP = 0.25
VOLATILITY = 0.15
TRACEABILITY = 1.0
SERVICE_COVERAGE = 0.70
REVIEW_HOURS = 48.0
SEVERITIES = ("critical", "major", "minor")


@dataclass(frozen=True)
class Metric:
    value: object
    previous: object = None
    warn: bool = False


@dataclass(frozen=True)
class Report:
    schedule: list = field(default_factory=list)
    effort: Metric = Metric(None)
    velocity: Metric = Metric(None)
    volatility: Metric = Metric(None)
    traceability: Metric = Metric(None)
    coverage: Metric = Metric(None)
    defects: Metric = Metric(None)
    review: Metric = Metric(None)


def _time(value: str | None) -> datetime | None:
    return datetime.fromisoformat(value.replace("Z", "+00:00")) if value else None


def parse_field(body: str | None, label: str) -> float | None:
    """Number under '### label' (issue forms) or after 'label:'; None if absent or '_'."""
    if not body:
        return None
    pattern = rf"(?:###\s*{re.escape(label)}\s*\n+|{re.escape(label)}\s*:\s*)([0-9]+(?:[.,][0-9]+)?)"
    match = re.search(pattern, body)
    return float(match.group(1).replace(",", ".")) if match else None


def schedule_progress(milestones: list[dict], now: datetime) -> list[dict]:
    """Done ratio vs calendar ratio per milestone; each starts when the previous one is due."""
    rows = []
    start = PROJECT_START
    for m in sorted((m for m in milestones if m.get("due_on")), key=lambda m: m["due_on"]):
        due = _time(m["due_on"])
        total = m["open_issues"] + m["closed_issues"]
        done = m["closed_issues"] / total if total else 0.0
        span = (due - start).total_seconds()
        elapsed = min(max((now - start).total_seconds() / span, 0.0), 1.0) if span > 0 else 1.0
        rows.append({
            "title": m["title"],
            "done": done,
            "elapsed": elapsed,
            "warn": total > 0 and done < elapsed - SCHEDULE_LAG,
        })
        start = due
    return rows


def effort_deviation(issues: list[dict]) -> Metric:
    pairs = [
        (parse_field(i.get("body"), "Ước lượng (giờ)"), parse_field(i.get("body"), "Thực tế (giờ)"))
        for i in issues
        if i["state"] == "closed"
    ]
    pairs = [(e, a) for e, a in pairs if e and a is not None]
    if not pairs:
        return Metric(None)
    deviation = sum(a for _, a in pairs) / sum(e for e, _ in pairs) - 1
    return Metric(deviation, warn=abs(deviation) > EFFORT_DEVIATION + 1e-9)


def _points_closed_between(issues, start, end) -> float:
    total = 0.0
    for i in issues:
        closed = _time(i.get("closed_at"))
        if closed and start <= closed < end:
            total += parse_field(i.get("body"), "Story points") or 0
    return total


def velocity(issues: list[dict], now: datetime) -> Metric:
    current = _points_closed_between(issues, now - WEEK, now)
    previous = _points_closed_between(issues, now - 2 * WEEK, now - WEEK)
    drop = previous > 0 and current < previous * (1 - VELOCITY_DROP)
    return Metric(current, previous=previous, warn=drop)


def requirement_volatility(baseline: dict | None, current: dict) -> Metric:
    if not baseline:
        return Metric(None)
    changed = sum(1 for rid in baseline.keys() | current.keys() if baseline.get(rid) != current.get(rid))
    ratio = changed / len(baseline)
    return Metric(ratio, warn=ratio > VOLATILITY)


def service_coverage(jacoco_csv: str | None) -> Metric:
    if not jacoco_csv:
        return Metric(None)
    rows = [r for r in csv.DictReader(io.StringIO(jacoco_csv)) if ".service" in r["PACKAGE"]]
    covered = sum(int(r["LINE_COVERED"]) for r in rows)
    total = covered + sum(int(r["LINE_MISSED"]) for r in rows)
    if total == 0:
        return Metric(None)
    ratio = covered / total
    return Metric(ratio, warn=ratio < SERVICE_COVERAGE)


def _severity(issue: dict) -> str:
    for label in (l["name"] for l in issue.get("labels", [])):
        if label.startswith("severity:") and label[9:] in SEVERITIES:
            return label[9:]
    match = re.search(r"###\s*Mức độ nghiêm trọng\s*\n+\s*(\w+)", issue.get("body") or "")
    level = match.group(1).lower() if match else ""
    return level if level in SEVERITIES else "unknown"


def defects_by_severity(issues: list[dict]) -> Metric:
    counts = {s: 0 for s in (*SEVERITIES, "unknown")}
    for i in issues:
        if i["state"] == "open" and any(l["name"] == "bug" for l in i.get("labels", [])):
            counts[_severity(i)] += 1
    return Metric(counts, warn=counts["critical"] > 0)


def review_time(prs: list[dict], now: datetime) -> Metric:
    hours = [
        (_time(p["first_review_at"]) - _time(p["created_at"])).total_seconds() / 3600
        for p in prs
        if p.get("merged_at") and p.get("first_review_at") and _time(p["merged_at"]) >= now - WEEK
    ]
    if not hours:
        return Metric(None)
    median = statistics.median(hours)
    return Metric(median, warn=median > REVIEW_HOURS)


def _pct(value) -> str:
    return f"{value:.0%}"


def _status(metric_warn: bool, has_value: bool) -> str:
    if not has_value:
        return "chưa có dữ liệu"
    return "⚠️ vượt ngưỡng" if metric_warn else "✅"


def render(report: Report, now: datetime) -> str:
    def line(name, metric, text, threshold):
        has = metric.value is not None
        return f"| {name} | {text if has else '—'} | {threshold} | {_status(metric.warn, has)} |"

    behind = [r for r in report.schedule if r["warn"]]
    schedule_text = "; ".join(f"{r['title']} {_pct(r['done'])} / lịch {_pct(r['elapsed'])}" for r in report.schedule)
    schedule = Metric(schedule_text or None, warn=bool(behind))
    d = report.defects.value or {}
    lines = [
        f"# Báo cáo chỉ số tuần {now.astimezone(timezone(timedelta(hours=7))):%d/%m/%Y}",
        "",
        "| Chỉ số | Giá trị | Ngưỡng cảnh báo | Trạng thái |",
        "|---|---|---|---|",
        line("Hoàn thành so với kế hoạch", schedule, schedule_text, "chậm hơn 10%"),
        line("Nỗ lực thực tế so với ước lượng", report.effort,
             f"{report.effort.value:+.0%}" if report.effort.value is not None else "", "lệch trên 20%"),
        line("Vận tốc sprint (story point/tuần)", report.velocity,
             f"{report.velocity.value:g} (tuần trước {report.velocity.previous or 0:g})"
             if report.velocity.value is not None else "", "giảm trên 25%"),
        line("Biến động yêu cầu", report.volatility,
             _pct(report.volatility.value) if report.volatility.value is not None else "", "trên 15%"),
        line("Độ phủ truy vết yêu cầu", report.traceability,
             _pct(report.traceability.value) if report.traceability.value is not None else "", "dưới 100%"),
        line("Độ bao phủ unit test (service)", report.coverage,
             _pct(report.coverage.value) if report.coverage.value is not None else "", "dưới 70%"),
        line("Lỗi theo mức độ (đang mở)", report.defects,
             ", ".join(f"{k}: {v}" for k, v in d.items()), "còn lỗi Critical"),
        line("Thời gian duyệt PR (trung vị)", report.review,
             f"{report.review.value:.0f} giờ" if report.review.value is not None else "", "trên 48 giờ"),
        "",
        "Chỉ số vượt ngưỡng cần có hành động khắc phục ghi trong biên bản họp tuần.",
    ]
    return "\n".join(lines) + "\n"


# ---- data collection (gh CLI and git); kept thin so the logic above stays testable ----

def _run(args: list[str]) -> str:
    return subprocess.run(args, check=True, capture_output=True, text=True).stdout


def _gh_json(path: str) -> list:
    pages = _run(["gh", "api", "--paginate", "--slurp", path])
    return [item for page in json.loads(pages) for item in page]


def _pull_requests(repo: str, now: datetime) -> list[dict]:
    result = []
    for pr in _gh_json(f"repos/{repo}/pulls?state=closed&per_page=100"):
        if not pr.get("merged_at") or _time(pr["merged_at"]) < now - WEEK:
            continue
        reviews = _gh_json(f"repos/{repo}/pulls/{pr['number']}/reviews?per_page=100")
        submitted = sorted(r["submitted_at"] for r in reviews if r.get("submitted_at"))
        result.append({**pr, "first_review_at": submitted[0] if submitted else None})
    return result


def requirement_content(text: str) -> dict:
    """What counts as the requirement itself: its statement and priority.

    Metadata (title, source, related, sprint, milestone) can be corrected without it
    being a requirement change for the volatility metric.
    """
    return {row["id"]: (row["priority"], row["statement"]) for row in csv.DictReader(io.StringIO(text))}


def _baseline_rows(root: Path) -> dict | None:
    try:
        text = _run(["git", "-C", str(root), "show", f"{BASELINE_TAG}:docs/requirements/requirements.csv"])
    except subprocess.CalledProcessError:
        return None
    return requirement_content(text)


def _traceability(root: Path) -> Metric:
    reqs = req.load_requirements(req.DEFAULT_CATALOG)
    files = {str(p): p.read_text(encoding="utf-8", errors="replace") for p in req.collect_test_files(root)}
    rows = req.build_rtm(reqs, req.find_references(files))
    ratio = sum(1 for r in rows if r.tests) / len(rows)
    return Metric(ratio, warn=ratio < TRACEABILITY)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument("--repo", required=True)
    parser.add_argument("--root", default=str(req.ROOT))
    parser.add_argument("--jacoco", default="src/backend/target/site/jacoco/jacoco.csv")
    parser.add_argument("--out", help="write the report here instead of stdout")
    args = parser.parse_args(argv)

    now = datetime.now(timezone.utc)
    root = Path(args.root)
    try:
        issues = [i for i in _gh_json(f"repos/{args.repo}/issues?state=all&per_page=100")
                  if "pull_request" not in i]
        milestones = _gh_json(f"repos/{args.repo}/milestones?state=all&per_page=100")
        prs = _pull_requests(args.repo, now)
    except (subprocess.CalledProcessError, json.JSONDecodeError) as err:
        print(f"metrics: could not read GitHub data: {err}", file=sys.stderr)
        return 1
    jacoco = Path(args.jacoco)
    current = requirement_content(req.DEFAULT_CATALOG.read_text(encoding="utf-8"))
    report = Report(
        schedule=schedule_progress(milestones, now),
        effort=effort_deviation(issues),
        velocity=velocity(issues, now),
        volatility=requirement_volatility(_baseline_rows(root), current),
        traceability=_traceability(root),
        coverage=service_coverage(jacoco.read_text(encoding="utf-8") if jacoco.is_file() else None),
        defects=defects_by_severity(issues),
        review=review_time(prs, now),
    )
    text = render(report, now)
    if args.out:
        Path(args.out).write_text(text, encoding="utf-8")
    else:
        print(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
