# Phiếu phê duyệt mốc M2 – Thiết kế

Hạn mốc: 05/11/2026 theo CR-02 (trước đó 08/11/2026) · Người lập: Claude (vai điều phối M2 – Mai Thành Khang) · Ngày lập: 04/11/2026 · Người phê duyệt: Đỗ Văn Tiến (PM, thay mặt nhóm)

Phiếu review nằm trong `Drafts/M2/reviews/` (RV-M2-01 → RV-M2-07), do Claude thực hiện theo vai người duyệt. Tổng hợp: `Deliverables/M2_ThietKe/eHoSo_ReviewThietKe_v1.0.pdf`.

## 1. Sản phẩm
| ID | Sản phẩm | Tác giả (vai) | Phiếu review | Kết luận review | PM duyệt |
|---|---|---|---|---|---|
| D11 | SDD v1.1 – `eHoSo_SDD_v1.1.pdf` | Khang | RV-M2-02 | Đạt | Đã duyệt 04/11 |
| D12 | UML – `UML/` | Khang | RV-M2-03 | Đạt | Đã duyệt 04/11 |
| D13 | ERD v1.0 – `eHoSo_ERD_v1.0.pdf`, `V1__init.sql` | Quang Anh | RV-M2-04 | Đạt | Đã duyệt 04/11 |
| D14 | OpenAPI v1.0 – `openapi.yaml` | Quang Anh | RV-M2-05 | Đạt | Đã duyệt 04/11 |
| D15 | Prototype v1.0 – `Prototype/index.html` | Phương | RV-M2-06 | Đạt | Đã duyệt 04/11 |
| D16 | Test Plan v1.0 – `eHoSo_TestPlan_v1.0.pdf` | Đạt | RV-M2-07 | Đạt có điều kiện: T-02, T-03 | Đã duyệt 04/11 |
| D17 | Quyết định kho tệp v1.0 | Khang | RV-M2-01 | Đạt; PR #47 đã merge | Đã duyệt 04/11 |
| T13 | Báo cáo review thiết kế v1.0 | Tiến, Đạt | – | – | Đã duyệt 04/11 |
| D08b | RTM v1.1 | Đạt | Mục 3 của T13 | Đạt | Đã duyệt 04/11 |
| D05c | SRS v1.2 | Tiến | Kiểm tra: 47/47 yêu cầu giữ nguyên nội dung | Đạt | Đã duyệt 04/11 |

## 2. Quyết định
| Mã | Nội dung | Đề xuất | PM |
|---|---|---|---|
| CR-02 | Dự án kết thúc **10/11/2026** thay vì 06/12/2026. Lịch: M2 05/11; Sprint 1 05/11–07/11; Sprint 2 07/11–09/11; M5 09/11–10/11. Phạm vi và 47 yêu cầu không đổi. Hệ quả: Project Plan đã trình có lịch khác, cần giải thích khi bảo vệ; số giờ kế hoạch chỉ còn là mốc so sánh | Áp dụng | **Đã xác nhận 04/11** |
| QD-09 | Quyết định thiết kế TK-01 → TK-10 | Áp dụng | **Đã duyệt 04/11** |
| V-03 → V-07, V-10 | Cách hiểu yêu cầu ở SRS v1.2 Phụ lục B | Áp dụng | **Đã duyệt 04/11** |

## 3. Tồn đọng chuyển sang mốc sau
| # | Việc | Người phụ trách | Hạn |
|---|---|---|---|
| 1 | Đồng bộ tài liệu M2 vào repo qua PR; sửa ADR-001 (V-09) | Đạt, Khang | 05/11 |
| 2 | Tích hợp Garage, Caddy vào Compose; K1–K7; đóng #40 | Khang, Đạt | Sprint 1 |
| 3 | Máy có Docker hoặc GitHub Actions (T-02) | Tiến, Đạt | 05/11 |
| 4 | 3 người dùng thật cho CK-U02 (T-03) | Tiến | 09/11 |
| 5 | OI-02, OI-03 | Tiến, Khang | 09/11 |

## 4. Phê duyệt
PM trả lời: **Đã duyệt: Tiến (PM, thay mặt nhóm), 04/11/2026**, gồm QD-09 và V-03 → V-07, V-10. CR-02: "finish" là toàn bộ dự án và báo cáo nộp giảng viên. Kiểm thử trên GitHub Actions · Ngày: 04/11/2026 · Ghi chú: Claude được toàn quyền push
