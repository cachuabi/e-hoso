# Phiếu review RV-M2-05

| Mục | Nội dung |
|---|---|
| Sản phẩm | D14 – Đặc tả OpenAPI 3, phiên bản 1.0 |
| Tác giả (vai) | Đoàn Quang Anh (Backend + CSDL) |
| Người review (vai) | Đỗ Văn Phương (Frontend + UI/UX) – review do Claude thực hiện theo vai |
| Ngày | 04/11/2026 |
| Căn cứ | Checklist HUONG_DAN mục 6; SRS v1.2 mục 3.7; Use case v1.1; SDD v1.1 mục 7 |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Đúng cú pháp OpenAPI 3 | Có | `openapi-spec-validator`: hợp lệ |
| 2 | Mỗi endpoint ghi FR và vai trò | Có | 44/44 có `x-requirements`, `x-roles` |
| 3 | Mọi FR có endpoint | Có | 28/28; không có mã lạ |
| 4 | Đủ 12 hành động của vòng đời | Có | Kiểm tra bằng script |
| 5 | Tra cứu không lộ thông tin cá nhân | Có | `PublicStatus` chỉ 4 trường, cấm trường thêm |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| A-01 | minor | `x-roles` | Vai trò `CCTP_ASSIGNED` là quy ước của tài liệu, không phải vai trò trong dữ liệu | Hiểu là CCTP được giao hồ sơ; kiểm tra ở service | Giữ nguyên, đã ghi trong SDD mục 12.2 |
| A-02 | nit | `/auth/refresh` | Xác thực bằng cookie nên không dùng `bearerAuth` | Giữ `security: []`; ghi trong mô tả | Giữ nguyên |

## Kết luận
Đạt. Phiên bản: 1.0.
