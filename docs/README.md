# Tài liệu dự án (Documents Repository)

| Thư mục | Nội dung | Mốc |
|---|---|---|
| `project/` | Project Plan (bản nộp `ProjectPlan.pdf` và slide) | M0 – 27/10 |
| `requirements/` | SRS v1.0, danh mục yêu cầu, báo cáo khảo sát | M1 – 01/11 |
| `design/` | SDD (IEEE 1016), UML, ERD, OpenAPI, prototype | M2 – 08/11 |
| `test/` | Test Plan, báo cáo kiểm thử (ISO/IEC/IEEE 29119-3) | M2, M5 |
| `../meetings/` | Biên bản họp (Meetings Repository) | hằng tuần |
| `QUY_TRINH.md`, `templates/` | Quy trình làm việc, duyệt 3 tầng; mẫu phiếu review, phiếu phê duyệt mốc, nhật ký ngày | xuyên suốt |

Mỗi mốc được gắn tag phiên bản trên `main` khi nộp, ví dụ `git tag m1-srs && git push origin m1-srs`. Riêng SRS v1.0 dùng thêm tag `srs-v1.0` làm mốc so sánh cho chỉ số biến động yêu cầu.
