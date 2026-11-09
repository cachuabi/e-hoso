# Phiếu review RV-M5-01

| Mục | Nội dung |
|---|---|
| Sản phẩm | D12b – 14 sơ đồ UML, DFD bổ sung cho báo cáo cuối kỳ (`docs/design/uml/14` → `26`, gồm `24b`), phiên bản 1.0 |
| Tác giả (vai) | Mai Thành Khang (Tech Lead) – do Claude thực hiện theo vai |
| Người review (vai) | Đoàn Quang Anh (Backend + CSDL, chủ sở hữu `docs/design/uml/` trong `CODEOWNERS`) – review do Claude thực hiện theo vai |
| Ngày | 09/11/2026 |
| Căn cứ | Outline báo cáo cuối kỳ (chốt 09/11/2026); SRS v1.2; Use case v1.2; mã nguồn tại `1c49d1d`; sơ đồ ngữ cảnh 02 |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Đủ các sơ đồ outline yêu cầu: DFD mức 1, 2; 3 activity; 5 state; lớp thiết kế; kiến trúc | Có | 14 hình: lớp thiết kế tách 24, 24b để đọc được |
| 2 | Vẽ bằng PlantUML, có `.puml` và `.png`, kết xuất không lỗi | Có | PlantUML 1.2024.7, bố cục Smetana |
| 3 | DFD mức 1 cân bằng với sơ đồ ngữ cảnh: cùng 3 thực thể ngoài | Có | |
| 4 | DFD mức 2 cân bằng với tiến trình 4.0 ở mức 1 | Có, sau khi sửa | Xem P5-02 |
| 5 | Mọi tiến trình có luồng vào và ra; không có luồng thực thể → kho trực tiếp | Có | |
| 6 | 3 activity phủ đủ 17 UC | Có | 16: UC-01 → 07; 17: UC-08 → 13; 18: UC-14 → 17 |
| 7 | State khớp mã: `AuthService`, `StaffUserService`, `DossierAttachmentGuard`, `ApprovalService`, `NotificationDispatcher` | Có | Đối chiếu từng chuyển trạng thái và ngưỡng (5 lần, 15 phút, 3 lần thử, TTL) |
| 8 | Lớp thiết kế có đủ 7 controller, service, repository, chiều điều hướng | Có | Chiều lấy từ hàm dựng; ghi rõ chỉ vẽ phụ thuộc chính |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| P5-01 | minor | `16_activity_nguoi_dan.puml:72` | Nhãn `:Nộp lại; giữ mã tham chiếu;` có `;` ở giữa nên PlantUML cắt nhãn | Đổi `;` thành `,` | Đã sửa |
| P5-02 | major | `15_dfd_muc2_xu_ly.puml` | Mức 2 chưa cân bằng với mức 1: thiếu luồng ra cho CBMC (hàng đợi, chi tiết), CCTP (hồ sơ được giao), Lãnh đạo (dự thảo); thiếu luồng đọc D5, D6 | Thêm 4.1 → CBMC, 4.4 → CCTP, 4.5 → Lãnh đạo, D5 → 4.1, D5 → 4.5, D6 → 4.1 | Đã sửa |
| P5-03 | minor | `14_dfd_muc1.puml` | 4.0 ghi D1 (đếm sai mật khẩu phê duyệt) nhưng chỉ vẽ chiều đọc; luồng "nhật ký hồ sơ" ra E2 không có nguồn từ D6 | `D1 <--> P4`, thêm `D6 --> P4` | Đã sửa; bảng kho dữ liệu ở báo cáo mục 3.1 cập nhật theo |
| P5-04 | minor | `19_state_tai_khoan.puml` | Khoá đăng nhập 15 phút áp dụng cả khi PENDING, sơ đồ chỉ đặt trong ACTIVE | Thêm ghi chú | Đã sửa ở tầng tự kiểm (commit `39648eb`) |
| P5-05 | nit | Các sơ đồ dùng Smetana | Máy dựng không có Graphviz nên đường nối nhiều chỗ cắt nhau, khác phong cách sơ đồ 01 → 13 | Render lại bằng Graphviz nếu có | Không sửa: nội dung đúng, đọc được |

Mức: blocker (phải sửa trước khi qua) · major (nên sửa) · minor · nit.

## Kết luận
Đạt. Phiên bản sau khi sửa: 1.1.
