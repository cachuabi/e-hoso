# Phiếu review RV-M5-02

| Mục | Nội dung |
|---|---|
| Sản phẩm | D29 – Báo cáo cuối kỳ, bản nháp tiếng Việt 2.0 (`docs/report/eHoSo_BaoCaoCuoiKy_v2.0.docx`, nguồn `docs/report/src/`) |
| Tác giả (vai) | Mai Xuân Đạt (QA + DevOps, điều phối M5) – do Claude thực hiện theo vai |
| Người review (vai) | Mai Thành Khang (Tech Lead, người duyệt mặc định trong `CODEOWNERS`) – review do Claude thực hiện theo vai |
| Ngày | 09/11/2026 |
| Căn cứ | Outline báo cáo cuối kỳ (chốt 09/11/2026); `requirements.csv`; SRS v1.2; SDD v1.2; Báo cáo kiểm thử v1.0; RTM v1.2; mã nguồn `1c49d1d`; nhánh `evidence` |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Cấu trúc theo outline: phần đầu, 5 chương, tài liệu tham khảo, phụ lục A, B, C | Có | |
| 2 | Áp dụng 4 quyết định đã chốt trong outline | Có | Mục 1.4; ngày trình bày để trống; mục 4.2; PR #51, #52 |
| 3 | Định dạng nhóm: bìa, lịch sử phiên bản, Times New Roman, bảng có viền | Có | Mục lục, danh mục hình, bảng là trường Word, cập nhật khi mở |
| 4 | Mọi mã FR, NFR, BR có trong `requirements.csv`; UC-01 → 17, KS-01 → 14, DR-01 → 08 hợp lệ | Có | Kiểm tra tự động: 0 mã sai |
| 5 | Mọi tham chiếu "mục x.y" trỏ tới mục có thật | Có | Kiểm tra tự động |
| 6 | Số đếm khớp nguồn: 28 FR (22 M, 4 S, 2 C), 10 NFR, 9 BR, 8 DR, 17 UC, 13 cặp chuyển trạng thái, 13 bảng | Có | Đối chiếu `requirements.csv`, `DossierLifecycle`, `V1__init.sql` |
| 7 | Số liệu kiểm thử có nguồn kiểm chứng được | Có, sau khi sửa | Xem B-01 |
| 8 | Không bịa số liệu, tên người; chỗ chưa biết ghi `?` | Có | |
| 9 | Độ dài 45–50 trang | Không | Xem B-03 |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| B-01 | major | Mục 4.7, phần đầu | Báo cáo dẫn lần chạy E2E `366b2e5` trên nhánh `evidence`, nhưng nhánh này đã được ghi đè bằng lần chạy cho `39648eb` (CI của PR #55); thư mục `366b2e5` không còn | Dùng lần chạy `39648eb` (cùng mã với `1c49d1d`) cho E2E, k6, ZAP, axe, K2, K4, K5, K6 | Đã sửa: p95 7,89 ms, 7495 yêu cầu, K5 158 ms, RAM 390/440 MiB; E2E 9/9 trên x86, arm64. Dẫn theo mã lần chạy GitHub Actions `42555716523` vì nhánh `evidence` chỉ giữ lần chạy mới nhất |
| B-02 | minor | Mục 1.5, 4.4 | Ảnh màn hình chép từ lần chạy cũ, không khớp nguồn đang dẫn | Chép lại từ thư mục `39648eb…/screenshots` | Đã sửa |
| B-03 | major | Toàn văn | Bản xuất thử qua Word khoảng 55 trang, cộng khoảng 3 trang mục lục và danh mục; outline đặt 45–50 trang | PM chọn phần cắt. Đề xuất: (a) chuyển đặc tả 4 UC (mục 2.5) và bảng 10 bảng gọn (mục 3.7) sang phụ lục; (b) bỏ bảng luồng dữ liệu (mục 3.1) vì đã có trên sơ đồ; (c) giữ nguyên nếu giảng viên không giới hạn cứng | PM quyết giữ nguyên (09/11/2026) |
| B-04 | minor | Bìa | Tên trường "Trường Quốc tế – ĐHQGHN" và "Hà Nội" do suy ra từ tên thư mục | PM xác nhận; bìa thay theo mẫu của Trường khi dịch | Chờ PM |
| B-05 | minor | Phần đầu, mục 1.4 | Còn `?`: ngày trình bày, đóng góp và giờ thực tế từng thành viên, trạng thái OI-02 | Thành viên tự khai; PM chốt OI-02 | Chờ PM, cả nhóm |
| B-06 | minor | Mục 1.4, bảng NFR, mục 3.6 | (Tầng tự kiểm) câu về xác nhận KS-01 → 07; NFR-03, NFR-08 ghi "Đạt" quá mức; câu suy luận về `CHECK` | Viết lại cho đúng bằng chứng | Đã sửa ở commit `39648eb` |
| B-07 | nit | Tệp `.docx` | Word hỏi cập nhật trường khi mở | Chọn "Có" để sinh mục lục, danh mục | Không sửa: cố ý |

Mức: blocker (phải sửa trước khi qua) · major (nên sửa) · minor · nit.

## Kết luận
Đạt có điều kiện. Điều kiện: PM xác nhận B-04, B-05 (B-03 đã quyết giữ nguyên ngày 09/11/2026) trước khi chốt bản cuối và dịch sang tiếng Anh. Phiên bản sau khi sửa: 2.0 (bản nháp, chưa đổi số vì chưa chốt).
