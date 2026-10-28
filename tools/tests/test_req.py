"""Tests for tools/req.py: requirement catalog, traceability (RTM) and issue planning."""

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import req  # noqa: E402

HEADER = "id,type,priority,sprint,milestone,source,related,title,statement\n"
CATALOG = HEADER + (
    'FR-01,FR,M,S1,M3,KS-01,,Đăng ký,"Khách phải đăng ký được, kèm dấu phẩy."\n'
    "FR-02,FR,S,S1,M3,,,Đăng nhập,Người dùng phải đăng nhập.\n"
    "FR-15,FR,M,S2,M4,,BR-03,Hàng đợi,CBMC phải xem hàng đợi.\n"
    "NFR-07,NFR,M,ALL,M5,,,Bảo trì,Coverage tối thiểu 70%.\n"
    "BR-03,BR,M,S2,M4,,FR-15,Hạn trả,Tiếp nhận trước 15:00 thì hạn 17:00.\n"
)


def write_catalog(text):
    tmp = tempfile.NamedTemporaryFile("w", suffix=".csv", delete=False, encoding="utf-8")
    tmp.write(text)
    tmp.close()
    return Path(tmp.name)


class LoadRequirementsTest(unittest.TestCase):
    def test_reads_every_row_with_quoted_commas(self):
        reqs = req.load_requirements(write_catalog(CATALOG))
        self.assertEqual([r.id for r in reqs], ["FR-01", "FR-02", "FR-15", "NFR-07", "BR-03"])
        self.assertEqual(reqs[0].statement, "Khách phải đăng ký được, kèm dấu phẩy.")
        self.assertEqual(reqs[2].related, ("BR-03",))

    def test_rejects_duplicate_ids(self):
        bad = CATALOG + "FR-01,FR,M,S1,M3,,,Trùng,Trùng mã.\n"
        with self.assertRaisesRegex(ValueError, "duplicate.*FR-01"):
            req.load_requirements(write_catalog(bad))

    def test_rejects_unknown_priority(self):
        bad = HEADER + "FR-01,FR,X,S1,M3,,,T,S\n"
        with self.assertRaisesRegex(ValueError, "priority"):
            req.load_requirements(write_catalog(bad))

    def test_rejects_type_that_does_not_match_the_id(self):
        bad = HEADER + "FR-01,NFR,M,S1,M3,,,T,S\n"
        with self.assertRaisesRegex(ValueError, "type"):
            req.load_requirements(write_catalog(bad))

    def test_real_catalog_is_valid(self):
        reqs = req.load_requirements(req.DEFAULT_CATALOG)
        ids = [r.id for r in reqs]
        self.assertEqual(sum(i.startswith("FR-") for i in ids), 28)
        self.assertEqual(sum(i.startswith("NFR-") for i in ids), 10)
        self.assertEqual(sum(i.startswith("BR-") for i in ids), 9)


class ReferencesTest(unittest.TestCase):
    def test_finds_ids_in_test_sources(self):
        files = {
            "a/DueDateServiceTest.java": '@Tag("BR-03") void before15h() {} // FR-15',
            "b/App.test.tsx": "it('[FR-01] registers', () => {})",
        }
        refs = req.find_references(files)
        self.assertEqual(refs["BR-03"], ("a/DueDateServiceTest.java",))
        self.assertEqual(refs["FR-01"], ("b/App.test.tsx",))

    def test_does_not_match_ids_inside_longer_tokens(self):
        refs = req.find_references({"x.java": "XFR-01 FR-011 FR-1"})
        self.assertEqual(refs, {})

    def test_reports_ids_that_are_not_in_the_catalog(self):
        reqs = req.load_requirements(write_catalog(CATALOG))
        refs = {"FR-01": ("x",), "FR-99": ("y",)}
        self.assertEqual(req.unknown_references(reqs, refs), ["FR-99"])

    def test_collects_backend_and_frontend_test_files_only(self):
        root = Path(tempfile.mkdtemp())
        wanted = [
            root / "src/backend/src/test/java/vn/ehoso/AppTest.java",
            root / "src/frontend/src/App.test.tsx",
            root / "tests/e2e/flow.spec.ts",
        ]
        ignored = [
            root / "src/backend/src/main/java/vn/ehoso/App.java",
            root / "src/frontend/node_modules/lib/x.test.ts",
        ]
        for path in wanted + ignored:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("FR-01", encoding="utf-8")
        found = sorted(str(p.relative_to(root)) for p in req.collect_test_files(root))
        self.assertEqual(found, sorted(str(p.relative_to(root)) for p in wanted))


class EnforcementTest(unittest.TestCase):
    def setUp(self):
        self.reqs = req.load_requirements(write_catalog(CATALOG))

    def test_lists_must_requirements_without_tests_in_enforced_sprints(self):
        rows = req.build_rtm(self.reqs, {"FR-15": ("t",)})
        missing = req.missing_tests(rows, {"S1", "S2"})
        self.assertEqual([r.id for r in missing], ["FR-01", "BR-03"])

    def test_should_and_could_requirements_are_never_enforced(self):
        rows = req.build_rtm(self.reqs, {})
        self.assertNotIn("FR-02", [r.id for r in req.missing_tests(rows, {"S1"})])

    def test_nothing_is_enforced_by_default(self):
        rows = req.build_rtm(self.reqs, {})
        self.assertEqual(req.missing_tests(rows, set()), [])

    def test_markdown_shows_coverage_and_missing_items(self):
        rows = req.build_rtm(self.reqs, {"FR-01": ("a.java", "b.tsx")})
        text = req.render_markdown(rows, {"S1"})
        self.assertIn("| FR-01 | M | S1 | 2 |", text)
        self.assertIn("1/5", text)
        self.assertIn("Enforced sprints: S1", text)


class MainTest(unittest.TestCase):
    def test_rtm_exits_non_zero_when_an_enforced_requirement_lacks_tests(self):
        root = Path(tempfile.mkdtemp())
        catalog = write_catalog(CATALOG)
        code = req.main(["rtm", "--root", str(root), "--catalog", str(catalog), "--enforce", "S1"])
        self.assertEqual(code, 1)

    def test_rtm_passes_when_nothing_is_enforced(self):
        root = Path(tempfile.mkdtemp())
        catalog = write_catalog(CATALOG)
        code = req.main(["rtm", "--root", str(root), "--catalog", str(catalog), "--enforce", ""])
        self.assertEqual(code, 0)

    def test_rtm_fails_on_a_typo_in_a_test_reference(self):
        root = Path(tempfile.mkdtemp())
        test_file = root / "src/frontend/src/x.test.ts"
        test_file.parent.mkdir(parents=True)
        test_file.write_text("it('[FR-99] typo')", encoding="utf-8")
        catalog = write_catalog(CATALOG)
        code = req.main(["rtm", "--root", str(root), "--catalog", str(catalog), "--enforce", ""])
        self.assertEqual(code, 1)


class IssuePlanningTest(unittest.TestCase):
    def setUp(self):
        self.reqs = req.load_requirements(write_catalog(CATALOG))

    def test_plans_issues_for_functional_and_non_functional_requirements_only(self):
        planned = req.plan_issues(self.reqs, existing_titles=[])
        self.assertEqual([r.id for r in planned], ["FR-01", "FR-02", "FR-15", "NFR-07"])

    def test_skips_requirements_that_already_have_an_issue(self):
        planned = req.plan_issues(self.reqs, existing_titles=["[FR-01] renamed by the team"])
        self.assertNotIn("FR-01", [r.id for r in planned])

    def test_issue_carries_labels_title_and_trackable_fields(self):
        fr15 = self.reqs[2]
        self.assertEqual(req.issue_title(fr15), "[FR-15] Hàng đợi")
        self.assertEqual(req.issue_labels(fr15), ["type:FR", "priority:M", "sprint:S2"])
        body = req.issue_body(fr15)
        self.assertIn("CBMC phải xem hàng đợi.", body)
        self.assertIn("BR-03", body)
        for field in ("### Ước lượng (giờ)", "### Story points", "### Thực tế (giờ)"):
            self.assertIn(field, body)


if __name__ == "__main__":
    unittest.main()
