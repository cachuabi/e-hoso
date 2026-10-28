#!/usr/bin/env python3
"""Requirement catalog tooling for e-HoSo (standard library only).

  python3 tools/req.py rtm [--enforce S1,S2] [--markdown FILE]
      Traceability: which requirement IDs (FR-xx, NFR-xx, BR-xx) are referenced by tests.
      Fails on references to unknown IDs, and on Must requirements without tests in the
      enforced sprints. Nothing is enforced until a sprint is listed (see Makefile).

  python3 tools/req.py issues --repo OWNER/NAME [--dry-run]
      Creates one GitHub issue per FR/NFR that does not have one yet (needs the gh CLI).

Tests reference requirements by writing the ID anywhere in the test source, for example
@Tag("BR-03") in JUnit or it('[FR-12] ...') in Vitest.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CATALOG = ROOT / "docs/requirements/requirements.csv"
REQ_ID = re.compile(r"(?<![A-Za-z0-9-])(?:FR|NFR|BR)-\d{2}(?![0-9])")
PRIORITIES = {"M", "S", "C"}
SPRINTS = {"S1", "S2", "ALL"}
ISSUE_TYPES = {"FR", "NFR"}
TEST_GLOBS = (
    "src/backend/src/test/**/*.java",
    "src/frontend/src/**/*.test.ts",
    "src/frontend/src/**/*.test.tsx",
    "tests/**/*.spec.ts",
    "tests/**/*.test.ts",
)


@dataclass(frozen=True)
class Requirement:
    id: str
    type: str
    priority: str
    sprint: str
    milestone: str
    source: str
    related: tuple[str, ...]
    title: str
    statement: str


@dataclass(frozen=True)
class RtmRow:
    requirement: Requirement
    tests: tuple[str, ...]

    @property
    def id(self) -> str:
        return self.requirement.id


def _split(value: str) -> tuple[str, ...]:
    return tuple(part.strip() for part in value.split(";") if part.strip())


def _validate(row: dict, line: int) -> None:
    rid = row["id"]
    if not REQ_ID.fullmatch(rid):
        raise ValueError(f"line {line}: bad id {rid!r}")
    if rid.split("-")[0] != row["type"]:
        raise ValueError(f"line {line}: type {row['type']!r} does not match id {rid}")
    if row["priority"] not in PRIORITIES:
        raise ValueError(f"line {line}: priority must be one of {sorted(PRIORITIES)}")
    if row["sprint"] not in SPRINTS:
        raise ValueError(f"line {line}: sprint must be one of {sorted(SPRINTS)}")


def load_requirements(path: Path) -> tuple[Requirement, ...]:
    """Reads and validates the catalog; raises ValueError on the first problem."""
    with open(path, encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    seen: set[str] = set()
    result = []
    for line, row in enumerate(rows, start=2):
        _validate(row, line)
        if row["id"] in seen:
            raise ValueError(f"line {line}: duplicate id {row['id']}")
        seen.add(row["id"])
        result.append(
            Requirement(
                id=row["id"],
                type=row["type"],
                priority=row["priority"],
                sprint=row["sprint"],
                milestone=row["milestone"],
                source=row["source"],
                related=_split(row["related"]),
                title=row["title"],
                statement=row["statement"],
            )
        )
    return tuple(result)


def collect_test_files(root: Path) -> list[Path]:
    files = {p for pattern in TEST_GLOBS for p in root.glob(pattern) if p.is_file()}
    return sorted(p for p in files if "node_modules" not in p.parts)


def find_references(files: dict[str, str]) -> dict[str, tuple[str, ...]]:
    """Maps each requirement ID to the test files (by name) that mention it."""
    refs: dict[str, list[str]] = {}
    for name, text in sorted(files.items()):
        for rid in sorted(set(REQ_ID.findall(text))):
            refs.setdefault(rid, []).append(name)
    return {rid: tuple(names) for rid, names in refs.items()}


def unknown_references(reqs, refs) -> list[str]:
    known = {r.id for r in reqs}
    return sorted(rid for rid in refs if rid not in known)


def build_rtm(reqs, refs) -> list[RtmRow]:
    return [RtmRow(r, refs.get(r.id, ())) for r in reqs]


def missing_tests(rows, enforced_sprints: set[str]) -> list[Requirement]:
    """Must requirements in the enforced sprints that no test references yet."""
    return [
        row.requirement
        for row in rows
        if row.requirement.priority == "M"
        and row.requirement.sprint in enforced_sprints
        and not row.tests
    ]


def render_markdown(rows, enforced_sprints: set[str]) -> str:
    covered = sum(1 for row in rows if row.tests)
    enforced = ", ".join(sorted(enforced_sprints)) or "none yet"
    lines = [
        "## Requirements traceability (RTM)",
        "",
        f"Requirements with at least one test: **{covered}/{len(rows)}**. "
        f"Enforced sprints: {enforced}.",
        "",
        "| ID | Priority | Sprint | Tests | Title | Test files |",
        "|---|---|---|---|---|---|",
    ]
    for row in rows:
        r = row.requirement
        files = "<br>".join(f"`{name}`" for name in row.tests) or "—"
        lines.append(f"| {r.id} | {r.priority} | {r.sprint} | {len(row.tests)} | {r.title} | {files} |")
    missing = missing_tests(rows, enforced_sprints)
    if missing:
        lines += ["", "**Missing tests (blocking):** " + ", ".join(r.id for r in missing)]
    return "\n".join(lines) + "\n"


def issue_title(r: Requirement) -> str:
    return f"[{r.id}] {r.title}"


def issue_labels(r: Requirement) -> list[str]:
    return [f"type:{r.type}", f"priority:{r.priority}", f"sprint:{r.sprint}"]


def issue_body(r: Requirement) -> str:
    related = ", ".join(r.related) or "—"
    source = r.source.replace(";", ", ") or "—"
    return f"""**Yêu cầu ({r.id}):** {r.statement}

**Ưu tiên:** {r.priority} · **Sprint:** {r.sprint} · **Mốc:** {r.milestone} · **Nguồn:** {source} · **Liên quan:** {related}

### Tiêu chí nghiệm thu
- [ ] Hành vi đáp ứng đúng phát biểu yêu cầu ở trên (kể cả các quy tắc liên quan)
- [ ] Có ít nhất một test tham chiếu `{r.id}` (kiểm tra bằng `python3 tools/req.py rtm`)
- [ ] `./scripts/check.sh` xanh, PR đã được duyệt

### Ước lượng (giờ)

_

### Story points

_

### Thực tế (giờ)

_

<sub>Tạo từ `docs/requirements/requirements.csv` (SRS v1.0). Sửa yêu cầu ở file CSV, không sửa ở đây.</sub>
"""


def plan_issues(reqs, existing_titles) -> list[Requirement]:
    """FR/NFR requirements that have no issue yet (matched by the "[ID]" title prefix)."""
    have = {title.split("]")[0] + "]" for title in existing_titles if title.startswith("[")}
    return [r for r in reqs if r.type in ISSUE_TYPES and f"[{r.id}]" not in have]


def _gh(args: list[str]) -> str:
    return subprocess.run(["gh", *args], check=True, capture_output=True, text=True).stdout


def _sync_issues(reqs, repo: str, dry_run: bool) -> int:
    existing = json.loads(
        _gh(["issue", "list", "--repo", repo, "--state", "all", "--limit", "1000", "--json", "title"])
    )
    planned = plan_issues(reqs, [item["title"] for item in existing])
    for r in planned:
        print(("would create: " if dry_run else "create: ") + issue_title(r))
        if dry_run:
            continue
        args = ["issue", "create", "--repo", repo, "--title", issue_title(r), "--body", issue_body(r)]
        for label in issue_labels(r):
            args += ["--label", label]
        if r.milestone:
            args += ["--milestone", r.milestone]
        _gh(args)
    print(f"{len(planned)} issue(s) {'to create' if dry_run else 'created'}.")
    return 0


def _rtm(reqs, root: Path, enforce: str, markdown: str | None) -> int:
    files = {
        str(p.relative_to(root)): p.read_text(encoding="utf-8", errors="replace")
        for p in collect_test_files(root)
    }
    refs = find_references(files)
    rows = build_rtm(reqs, refs)
    enforced = {s.strip() for s in enforce.split(",") if s.strip()}
    report = render_markdown(rows, enforced)
    if markdown:
        with open(markdown, "a", encoding="utf-8") as out:
            out.write(report)
    covered = sum(1 for row in rows if row.tests)
    print(f"RTM: {covered}/{len(rows)} requirements referenced by tests.")
    failed = False
    unknown = unknown_references(reqs, refs)
    if unknown:
        print("Unknown requirement IDs referenced by tests: " + ", ".join(unknown), file=sys.stderr)
        failed = True
    missing = missing_tests(rows, enforced)
    if missing:
        print("Must requirements without tests: " + ", ".join(r.id for r in missing), file=sys.stderr)
        failed = True
    return 1 if failed else 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    rtm = sub.add_parser("rtm", help="traceability report and gate")
    rtm.add_argument("--root", default=str(ROOT))
    rtm.add_argument("--enforce", default="", help="comma-separated sprints, e.g. S1,S2")
    rtm.add_argument("--markdown", help="append the RTM table to this file")
    issues = sub.add_parser("issues", help="create GitHub issues for FR/NFR")
    issues.add_argument("--repo", required=True)
    issues.add_argument("--dry-run", action="store_true")
    for command in (rtm, issues):
        command.add_argument("--catalog", default=str(DEFAULT_CATALOG))
    args = parser.parse_args(argv)

    try:
        reqs = load_requirements(Path(args.catalog))
    except (OSError, ValueError, KeyError) as err:
        print(f"req: invalid catalog {args.catalog}: {err}", file=sys.stderr)
        return 2
    if args.command == "rtm":
        return _rtm(reqs, Path(args.root), args.enforce, args.markdown)
    return _sync_issues(reqs, args.repo, args.dry_run)


if __name__ == "__main__":
    sys.exit(main())
