"""Tests for tools/metrics.py: the 8 weekly project metrics from the Project Plan."""

import sys
import unittest
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import metrics  # noqa: E402

NOW = datetime(2026, 11, 15, 13, 0, tzinfo=timezone.utc)


def issue(number, state="closed", closed_at=None, body="", labels=()):
    return {
        "number": number,
        "state": state,
        "closed_at": closed_at,
        "body": body,
        "labels": [{"name": name} for name in labels],
    }


class ParseFieldTest(unittest.TestCase):
    def test_reads_issue_form_section(self):
        body = "### Ước lượng (giờ)\n\n8\n\n### Story points\n\n3"
        self.assertEqual(metrics.parse_field(body, "Ước lượng (giờ)"), 8.0)
        self.assertEqual(metrics.parse_field(body, "Story points"), 3.0)

    def test_placeholder_or_missing_means_unknown(self):
        self.assertIsNone(metrics.parse_field("### Thực tế (giờ)\n\n_\n", "Thực tế (giờ)"))
        self.assertIsNone(metrics.parse_field(None, "Story points"))

    def test_accepts_inline_form_and_decimal_comma(self):
        self.assertEqual(metrics.parse_field("Thực tế (giờ): 6,5h", "Thực tế (giờ)"), 6.5)


class ScheduleTest(unittest.TestCase):
    def test_flags_a_milestone_that_lags_the_calendar_by_more_than_10_points(self):
        milestones = [
            {"title": "M1", "due_on": "2026-11-01T16:59:59Z", "open_issues": 0, "closed_issues": 4},
            {"title": "M2", "due_on": "2026-11-29T16:59:59Z", "open_issues": 8, "closed_issues": 2},
        ]
        rows = metrics.schedule_progress(milestones, NOW)
        m2 = rows[1]
        self.assertAlmostEqual(m2["done"], 0.2)
        self.assertAlmostEqual(m2["elapsed"], 0.5, places=1)
        self.assertTrue(m2["warn"])
        self.assertFalse(rows[0]["warn"])


class EffortTest(unittest.TestCase):
    def test_deviation_compares_actual_with_estimate_on_closed_issues(self):
        issues = [
            issue(1, body="### Ước lượng (giờ)\n\n10\n\n### Thực tế (giờ)\n\n13"),
            issue(2, body="### Ước lượng (giờ)\n\n10\n\n### Thực tế (giờ)\n\n11"),
            issue(3, state="open", body="### Ước lượng (giờ)\n\n10\n\n### Thực tế (giờ)\n\n99"),
            issue(4, body="### Ước lượng (giờ)\n\n5"),
        ]
        result = metrics.effort_deviation(issues)
        self.assertAlmostEqual(result.value, 0.2)
        self.assertFalse(result.warn)  # exactly 20% is still within the threshold

    def test_no_data_is_reported_not_guessed(self):
        self.assertIsNone(metrics.effort_deviation([issue(1)]).value)


class VelocityTest(unittest.TestCase):
    def test_warns_when_points_drop_more_than_25_percent(self):
        issues = [
            issue(1, closed_at="2026-11-14T10:00:00Z", body="### Story points\n\n3"),
            issue(2, closed_at="2026-11-06T10:00:00Z", body="### Story points\n\n5"),
            issue(3, closed_at="2026-11-07T10:00:00Z", body="### Story points\n\n3"),
        ]
        result = metrics.velocity(issues, NOW)
        self.assertEqual(result.value, 3)
        self.assertEqual(result.previous, 8)
        self.assertTrue(result.warn)


class VolatilityTest(unittest.TestCase):
    def test_counts_added_removed_and_modified_requirements(self):
        baseline = {"FR-01": ("a",), "FR-02": ("b",), "FR-03": ("c",), "FR-04": ("d",)}
        current = {"FR-01": ("a",), "FR-02": ("B changed",), "FR-04": ("d",), "FR-05": ("e",)}
        result = metrics.requirement_volatility(baseline, current)
        self.assertAlmostEqual(result.value, 3 / 4)
        self.assertTrue(result.warn)

    def test_missing_baseline_is_reported(self):
        self.assertIsNone(metrics.requirement_volatility(None, {"FR-01": ("a",)}).value)

    def test_only_statement_and_priority_count_as_a_requirement_change(self):
        header = "id,type,priority,sprint,milestone,source,related,title,statement\n"
        baseline = metrics.requirement_content(header + "FR-01,FR,M,S1,M3,,,Cũ,Phải làm X.\n"
                                                        "FR-02,FR,S,S1,M3,,,T,Phải làm Y.\n")
        metadata_only = metrics.requirement_content(header + "FR-01,FR,M,S2,M4,KS-01,BR-01,Mới,Phải làm X.\n"
                                                             "FR-02,FR,S,S1,M3,,,T,Phải làm Y.\n")
        reworded = metrics.requirement_content(header + "FR-01,FR,M,S1,M3,,,Cũ,Phải làm Z.\n"
                                                        "FR-02,FR,M,S1,M3,,,T,Phải làm Y.\n")
        self.assertEqual(metrics.requirement_volatility(baseline, metadata_only).value, 0)
        self.assertEqual(metrics.requirement_volatility(baseline, reworded).value, 1.0)


class CoverageTest(unittest.TestCase):
    JACOCO = (
        "GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,"
        "LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED\n"
        "b,vn.ehoso.dossier.service,DueDateService,0,0,0,0,10,30,0,0,0,0\n"
        "b,vn.ehoso.dossier.web,DossierController,0,0,0,0,50,0,0,0,0,0\n"
    )

    def test_measures_only_the_service_layer(self):
        result = metrics.service_coverage(self.JACOCO)
        self.assertAlmostEqual(result.value, 0.75)
        self.assertFalse(result.warn)

    def test_without_service_classes_there_is_no_value(self):
        self.assertIsNone(metrics.service_coverage(self.JACOCO.splitlines()[0] + "\n").value)


class DefectsTest(unittest.TestCase):
    def test_counts_open_bugs_by_severity_from_label_or_form(self):
        issues = [
            issue(1, state="open", labels=("bug", "severity:critical")),
            issue(2, state="open", labels=("bug",), body="### Mức độ nghiêm trọng\n\nMajor"),
            issue(3, state="closed", labels=("bug", "severity:critical")),
            issue(4, state="open", labels=("enhancement",)),
        ]
        result = metrics.defects_by_severity(issues)
        self.assertEqual(result.value, {"critical": 1, "major": 1, "minor": 0, "unknown": 0})
        self.assertTrue(result.warn)


class ReviewTimeTest(unittest.TestCase):
    def test_median_hours_to_first_review_for_prs_merged_this_week(self):
        prs = [
            {"merged_at": "2026-11-14T00:00:00Z", "created_at": "2026-11-10T00:00:00Z",
             "first_review_at": "2026-11-10T10:00:00Z"},
            {"merged_at": "2026-11-13T00:00:00Z", "created_at": "2026-11-10T00:00:00Z",
             "first_review_at": "2026-11-13T00:00:00Z"},
            {"merged_at": "2026-11-12T00:00:00Z", "created_at": "2026-11-11T00:00:00Z",
             "first_review_at": "2026-11-11T20:00:00Z"},
            {"merged_at": "2026-11-01T00:00:00Z", "created_at": "2026-10-20T00:00:00Z",
             "first_review_at": "2026-10-30T00:00:00Z"},
        ]
        result = metrics.review_time(prs, NOW)
        self.assertEqual(result.value, 20.0)
        self.assertFalse(result.warn)


class RenderTest(unittest.TestCase):
    def test_report_lists_all_eight_metrics_with_status(self):
        report = metrics.Report(
            schedule=[{"title": "M1", "done": 1.0, "elapsed": 1.0, "warn": False}],
            effort=metrics.Metric(None),
            velocity=metrics.Metric(3, previous=8, warn=True),
            volatility=metrics.Metric(0.05),
            traceability=metrics.Metric(0.5, warn=True),
            coverage=metrics.Metric(None),
            defects=metrics.Metric({"critical": 0, "major": 1, "minor": 0, "unknown": 0}),
            review=metrics.Metric(20.0),
        )
        text = metrics.render(report, NOW)
        for name in ("Hoàn thành so với kế hoạch", "Nỗ lực thực tế", "Vận tốc sprint",
                     "Biến động yêu cầu", "Độ phủ truy vết", "Độ bao phủ unit test",
                     "Lỗi theo mức độ", "Thời gian duyệt PR"):
            self.assertIn(name, text)
        self.assertIn("⚠️", text)
        self.assertIn("chưa có dữ liệu", text)


if __name__ == "__main__":
    unittest.main()
