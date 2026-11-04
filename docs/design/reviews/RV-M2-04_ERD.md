# Phiếu review RV-M2-04

| Mục | Nội dung |
|---|---|
| Sản phẩm | D13 – ERD và thiết kế CSDL, phiên bản 1.0 |
| Tác giả (vai) | Đoàn Quang Anh (Backend + CSDL) |
| Người review (vai) | Mai Thành Khang (Tech Lead) – review do Claude thực hiện theo vai |
| Ngày | 04/11/2026 |
| Căn cứ | Checklist HUONG_DAN mục 6; SRS v1.2 mục 8; SDD v1.1 mục 6 |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Đủ DR-01 → DR-08, khoá, ràng buộc | Có | 13 bảng |
| 2 | Khớp SRS mục 8 và SDD mục 6.2 | Có | Trạng thái, vai trò khớp OpenAPI (kiểm tra bằng script) |
| 3 | Nhật ký chỉ thêm (FR-23) | Có | Trigger đã thử |
| 4 | Lược đồ chạy được | Có | PostgreSQL 16; chưa chạy trên bản 17 của dự án |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| E-01 | major | `ck_user_national_id`, `ck_procedure_deadline` | `CHECK` nhận giá trị `NULL` nên không chặn được dữ liệu thiếu | Thêm `IS NOT NULL` | Đã sửa, thử lại đạt |
| E-02 | minor | Mục 7 | Chưa chạy trên PostgreSQL 17 | Chạy khi dựng Sprint 1 | Chuyển Sprint 1 |
| E-03 | nit | Dữ liệu khởi tạo | Chưa có thành phần hồ sơ, biểu mẫu của 3 thủ tục | Viết ở migration seed Sprint 1, căn cứ Báo cáo khảo sát | Chuyển Sprint 1 |

## Kết luận
Đạt. Phiên bản: 1.0.
