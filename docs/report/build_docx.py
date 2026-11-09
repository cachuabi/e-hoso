"""Builds the final report (.docx) from the Markdown chapters in docs/report/src.

Usage (from the repository root):
    python docs/report/build_docx.py [output.docx]

Needs python-docx (pip install python-docx). Not part of the product build or check.sh.

Markdown subset understood here:
    # / ## / ###        headings (a level-1 heading starts on a new page)
    paragraphs, - bullets, 1. numbered items, **bold**, *italic*, `code`
    | a | b |           pipe tables; a line "Bảng: <caption>" right above adds a numbered caption
    ![caption](path){w=6}   image (width in cm, default 16) with a numbered caption below
    ::: cover ... :::   cover page, lines "key: value"
    [[TOC]] [[LOF]] [[LOT]] [[PAGEBREAK]]
    [[ROW]] image lines [[/ROW]]   images side by side, each with its own caption
"""

import re
import struct
import sys
from pathlib import Path

from docx import Document
from docx.enum.section import WD_ORIENT
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

ROOT = Path(__file__).resolve().parents[2]
SRC = Path(__file__).resolve().parent / "src"
DEFAULT_OUT = Path(__file__).resolve().parent / "eHoSo_BaoCaoCuoiKy_v2.0.docx"
FONT = "Times New Roman"
BODY_PT = 13
TABLE_PT = 10
MAX_IMG_CM = 16.0
MAX_IMG_HEIGHT_CM = 15.5
TEXT_WIDTH_CM = 16.0
INLINE_RE = re.compile(r"(\*\*[^*]+\*\*|\*[^*\s][^*]*\*|`[^`]+`)")
IMG_RE = re.compile(r"^!\[(?P<cap>[^\]]*)\]\((?P<path>[^)]+)\)(\{w=(?P<w>[\d.]+)\})?\s*$")


def set_font(run, size=None, bold=None, italic=None, mono=False):
    name = "Courier New" if mono else FONT
    run.font.name = name
    run._element.get_or_add_rPr().get_or_add_rFonts().set(qn("w:eastAsia"), name)
    if size:
        run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic


def add_inline(paragraph, text, size=BODY_PT, bold=False):
    for part in INLINE_RE.split(text):
        if not part:
            continue
        if part.startswith("**"):
            set_font(paragraph.add_run(part[2:-2]), size, bold=True)
        elif part.startswith("`"):
            set_font(paragraph.add_run(part[1:-1]), size - 1.5, bold=bold, mono=True)
        elif part.startswith("*") and part.endswith("*") and len(part) > 2:
            set_font(paragraph.add_run(part[1:-1]), size, bold=bold, italic=True)
        else:
            set_font(paragraph.add_run(part), size, bold=bold)


def add_field(paragraph, instruction, placeholder=""):
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = f" {instruction} "
    sep = OxmlElement("w:fldChar")
    sep.set(qn("w:fldCharType"), "separate")
    run._r.extend([begin, instr, sep])
    shown = paragraph.add_run(placeholder)
    set_font(shown, BODY_PT)
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    paragraph.add_run()._r.append(end)


def caption(container, label, number, text):
    p = container.add_paragraph(style="Caption")
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(p.add_run(f"{label} "), 12, bold=True, italic=False)
    run = p.add_run()
    for kind, value in (("begin", None), ("instr", f" SEQ {label} \\* ARABIC "), ("separate", None)):
        if kind == "instr":
            node = OxmlElement("w:instrText")
            node.set(qn("xml:space"), "preserve")
            node.text = value
        else:
            node = OxmlElement("w:fldChar")
            node.set(qn("w:fldCharType"), kind)
        run._r.append(node)
    set_font(p.add_run(str(number)), 12, bold=True, italic=False)
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    p.add_run()._r.append(end)
    set_font(p.add_run(f". {text}"), 12, bold=False, italic=True)


def shade(cell, fill):
    props = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), fill)
    props.append(shd)


def repeat_header(row):
    props = row._tr.get_or_add_trPr()
    flag = OxmlElement("w:tblHeader")
    flag.set(qn("w:val"), "true")
    props.append(flag)


def split_row(line):
    cells = line.strip().strip("|").split("|")
    return [c.strip() for c in cells]


CHAR_CM = 0.21  # average width of a 10 pt Times New Roman character
CELL_PAD_CM = 0.5
MAX_MIN_COL_CM = 2.8  # longer codes wrap instead of starving the text columns


def visible(text):
    return re.sub(r"\*\*|`", "", text)


def column_widths(rows, count):
    """Give every column room for its longest word, then share the rest by average length."""
    minimum, average = [], []
    for c in range(count):
        cells = [visible(r[c]) if c < len(r) else "" for r in rows]
        longest = max((len(w) for v in cells for w in v.split()), default=1)
        minimum.append(min(longest * CHAR_CM + CELL_PAD_CM, MAX_MIN_COL_CM))
        average.append(sum(len(v) for v in cells) / max(len(cells), 1))
    spare = max(TEXT_WIDTH_CM - sum(minimum), 0)
    need = [max(a * CHAR_CM + CELL_PAD_CM - m, 0) for a, m in zip(average, minimum)]
    total_need = sum(need) or 1
    widths = [m + spare * n / total_need for m, n in zip(minimum, need)]
    scale = TEXT_WIDTH_CM / sum(widths)
    return [Cm(w * scale) for w in widths]


def add_table(doc, rows):
    header, body = rows[0], [r for r in rows[2:]]
    table = doc.add_table(rows=1 + len(body), cols=len(header))
    table.style = "Table Grid"
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    widths = column_widths([header] + body, len(header))
    for row in table.rows:
        for idx, width in enumerate(widths):
            row.cells[idx].width = width
    for r_idx, values in enumerate([header] + body):
        row = table.rows[r_idx]
        for c_idx in range(len(header)):
            value = values[c_idx] if c_idx < len(values) else ""
            cell = row.cells[c_idx]
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.1
            add_inline(p, value, TABLE_PT, bold=r_idx == 0)
            if r_idx == 0:
                shade(cell, "D9E2F3")
        if r_idx == 0:
            repeat_header(row)
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


def image_width(path, width_cm):
    with open(path, "rb") as handle:
        header = handle.read(24)
    if not header.startswith(b"\x89PNG"):
        raise ValueError(f"Chỉ hỗ trợ ảnh PNG: {path}")
    w_px, h_px = struct.unpack(">II", header[16:24])
    width = min(width_cm, MAX_IMG_CM)
    if width * h_px / w_px > MAX_IMG_HEIGHT_CM:
        width = MAX_IMG_HEIGHT_CM * w_px / h_px
    return Cm(width)


def add_image_row(doc, images, counters):
    table = doc.add_table(rows=1, cols=len(images))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell_cm = TEXT_WIDTH_CM / len(images)
    for cell, match in zip(table.rows[0].cells, images):
        path = ROOT / match["path"]
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        width = min(float(match["w"] or cell_cm - 0.4), cell_cm - 0.4)
        p.add_run().add_picture(str(path), width=image_width(path, width))
        counters["Hình"] += 1
        caption(cell, "Hình", counters["Hình"], match["cap"])
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


def add_cover(doc, fields):
    for _ in range(3):
        doc.add_paragraph()
    lines = [
        (fields.get("top", ""), 13, True),
        ("", 13, False),
        (fields.get("title", ""), 30, True),
        (fields.get("subtitle", ""), 20, True),
        ("", 13, False),
        (fields.get("desc", ""), 15, False),
        ("", 13, False),
        (fields.get("version", ""), 13, False),
    ]
    for text, size, bold in lines:
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        set_font(p.add_run(text), size, bold=bold)
    for _ in range(4):
        doc.add_paragraph()
    for key in ("team", "members", "teacher", "place"):
        if key in fields:
            p = doc.add_paragraph()
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            add_inline(p, fields[key], 13)
    doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)


def setup(doc):
    section = doc.sections[0]
    section.orientation = WD_ORIENT.PORTRAIT
    section.page_width, section.page_height = Cm(21), Cm(29.7)
    section.top_margin = section.bottom_margin = Cm(2)
    section.left_margin, section.right_margin = Cm(3), Cm(2)
    normal = doc.styles["Normal"]
    normal.font.name = FONT
    normal.font.size = Pt(BODY_PT)
    normal.element.rPr.rFonts.set(qn("w:eastAsia"), FONT)
    normal.paragraph_format.line_spacing = 1.15
    normal.paragraph_format.space_after = Pt(4)
    for level, size in ((1, 16), (2, 14), (3, 13)):
        style = doc.styles[f"Heading {level}"]
        style.font.name = FONT
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor(0, 0, 0)
        style.element.rPr.rFonts.set(qn("w:eastAsia"), FONT)
        style.paragraph_format.space_before = Pt(12 if level > 1 else 0)
        style.paragraph_format.space_after = Pt(6)
        style.paragraph_format.keep_with_next = True
        style.paragraph_format.page_break_before = level == 1
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    add_field(footer, "PAGE", "1")
    settings = doc.settings.element
    update = OxmlElement("w:updateFields")
    update.set(qn("w:val"), "true")
    settings.append(update)


FIELD_BLOCKS = {
    "[[TOC]]": 'TOC \\o "1-3" \\h \\z \\u',
    "[[LOF]]": 'TOC \\h \\z \\c "Hình"',
    "[[LOT]]": 'TOC \\h \\z \\c "Bảng"',
}
LIST_RE = re.compile(r"^(- |\d+\. )")


def read_block(lines, start, closing):
    """Returns the lines after lines[start] up to the closing marker, and the marker's index."""
    for end in range(start + 1, len(lines)):
        if lines[end].strip() == closing:
            return lines[start + 1:end], end
    raise ValueError(f"Thiếu dòng đóng '{closing}' cho khối mở ở dòng {start + 1}")


class ReportBuilder:
    """Turns the Markdown subset described in the module docstring into a Word document."""

    def __init__(self):
        self.doc = Document()
        setup(self.doc)
        self.counters = {"Hình": 0, "Bảng": 0}
        self.paragraph = []
        self.pending_caption = None

    def flush(self):
        if self.paragraph:
            p = self.doc.add_paragraph()
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            add_inline(p, " ".join(self.paragraph))
            self.paragraph.clear()

    def heading(self, text):
        level = len(text) - len(text.lstrip("#"))
        heading = self.doc.add_heading(level=min(level, 3))
        add_inline(heading, text[level:].strip(), {1: 16, 2: 14}.get(level, 13), bold=True)

    def table(self, rows):
        if self.pending_caption:
            self.counters["Bảng"] += 1
            caption(self.doc, "Bảng", self.counters["Bảng"], self.pending_caption)
            self.pending_caption = None
        add_table(self.doc, rows)

    def image(self, match):
        path = ROOT / match["path"]
        p = self.doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.keep_with_next = True
        width = image_width(path, float(match["w"] or MAX_IMG_CM))
        p.add_run().add_picture(str(path), width=width)
        self.counters["Hình"] += 1
        caption(self.doc, "Hình", self.counters["Hình"], match["cap"])

    def list_item(self, text):
        if text.startswith("- "):
            p = self.doc.add_paragraph(style="List Bullet")
            text = text[2:]
        else:
            # Literal numbers: Word's list numbering would continue across separate lists.
            p = self.doc.add_paragraph()
            p.paragraph_format.left_indent = Cm(0.9)
            p.paragraph_format.first_line_indent = Cm(-0.6)
        p.paragraph_format.space_after = Pt(2)
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        add_inline(p, text)

    def block(self, lines, i):
        """Handles the block starting at lines[i]; returns the index of the next line to read."""
        stripped = lines[i].strip()
        if not stripped.startswith(("::: cover", "[[", "#", "Bảng:", "|", "![", "- ")) \
                and not LIST_RE.match(stripped):
            if stripped:
                self.paragraph.append(stripped)
            else:
                self.flush()
            return i + 1
        self.flush()
        if stripped == "::: cover":
            body, end = read_block(lines, i, ":::")
            add_cover(self.doc, {k.strip(): v.strip() for k, _, v in (b.partition(":") for b in body)})
            return end + 1
        if stripped == "[[ROW]]":
            body, end = read_block(lines, i, "[[/ROW]]")
            add_image_row(self.doc, [IMG_RE.match(b.strip()) for b in body if IMG_RE.match(b.strip())],
                          self.counters)
            return end + 1
        if stripped.startswith("|"):
            end = i
            while end < len(lines) and lines[end].strip().startswith("|"):
                end += 1
            self.table([split_row(row) for row in lines[i:end]])
            return end
        self.single_line(stripped)
        return i + 1

    def single_line(self, stripped):
        if stripped in FIELD_BLOCKS:
            add_field(self.doc.add_paragraph(), FIELD_BLOCKS[stripped], "Nhấn F9 để cập nhật.")
        elif stripped == "[[PAGEBREAK]]":
            self.doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)
        elif stripped.startswith("#"):
            self.heading(stripped)
        elif stripped.startswith("Bảng:"):
            self.pending_caption = stripped[len("Bảng:"):].strip()
        elif IMG_RE.match(stripped):
            self.image(IMG_RE.match(stripped))
        elif LIST_RE.match(stripped):
            self.list_item(stripped)
        else:
            self.paragraph.append(stripped)


def build(out_path):
    lines = []
    for chapter in sorted(SRC.glob("*.md")):
        lines.extend(chapter.read_text(encoding="utf-8").splitlines())
        lines.append("")
    builder = ReportBuilder()
    i = 0
    while i < len(lines):
        i = builder.block(lines, i)
    builder.flush()
    builder.doc.save(out_path)
    print(f"{out_path}: {builder.counters['Hình']} hình, {builder.counters['Bảng']} bảng")


if __name__ == "__main__":
    build(Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_OUT)
