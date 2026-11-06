# Phiếu review RV-M2-06

| Mục | Nội dung |
|---|---|
| Sản phẩm | D15 – Prototype giao diện 4 vai trò, phiên bản 1.0 |
| Tác giả (vai) | Đỗ Văn Phương (Frontend + UI/UX) |
| Người review (vai) | Đỗ Văn Tiến (PM) – review do Claude thực hiện theo vai |
| Ngày | 04/11/2026 |
| Căn cứ | Checklist HUONG_DAN mục 6; Use case v1.1; NFR-02, NFR-06 |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Màn hình chính của 4 vai trò, tiếng Việt | Có | 16 màn hình |
| 2 | Dùng được ở 360 px | Có | Playwright: 16/16 không tràn ngang ở 360 px và 1280 px, không lỗi JavaScript |
| 3 | Trường nhập có nhãn | Có | Kiểm tra tự động trên 16 màn hình |
| 4 | Trạng thái không chỉ dựa vào màu | Có | Nhãn có chữ |
| 5 | Dữ liệu giả lập, số dashboard ghi rõ minh hoạ | Có | |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| P-01 | minor | Phạm vi | Không có màn hình Quản trị viên, vì D15 chỉ yêu cầu 4 vai trò | Thiết kế trực tiếp ở Sprint 2 theo UC-15, UC-16 | Chấp nhận |
| P-02 | minor | NFR-02, NFR-06 | Chưa thử với người dùng thật, chưa đo tương phản bằng công cụ | CK-U02, CK-A01 ở Test Plan | Chuyển kiểm thử |

## Kết luận
Đạt. Phiên bản: 1.0.
