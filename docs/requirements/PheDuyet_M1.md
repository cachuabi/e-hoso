# Phiếu phê duyệt mốc M1 – Yêu cầu

Hạn mốc: 01/11/2026 · Người lập: Claude (vai điều phối M1 – Đỗ Văn Tiến) · Ngày lập: 28/10/2026 · Người phê duyệt: Đỗ Văn Tiến (PM, thay mặt nhóm)

Phiếu review nằm trong `docs/requirements/reviews/` (RV-M1-01 → RV-M1-06), do Claude thực hiện theo vai người duyệt.

## 1. Sản phẩm

| ID | Sản phẩm (bản trình duyệt) | Tác giả (vai) | Phiếu review | Kết luận review | PM duyệt |
|---|---|---|---|---|---|
| D04 | Báo cáo khảo sát v1.1 – `docs/requirements/eHoSo_KhaoSat_v1.1.pdf` | Quang Anh | RV-M1-02 (vai Tiến) | Đạt | ? |
| D05b | SRS v1.1 – `eHoSo_SRS_v1.1.pdf` (17 trang, dựng lại 28/10 sau review) | Tiến | RV-M1-04 (vai Đạt) | Đạt có điều kiện: mục 3 (S-R4) và mục 2 (QD-06, QD-07) | ? |
| D06 | Use case v1.1 – `eHoSo_UseCase_v1.1.pdf` | Tiến | RV-M1-03 (vai Khang) | Đạt | ? |
| D07b | Báo cáo review SRS v1.2 – `eHoSo_SRS_Review_v1.2.pdf` | Đạt | RV-M1-05 (vai Tiến) | Đạt | ? |
| D08 | RTM v1.0 – `eHoSo_RTM_v1.0.pdf`, `.csv` | Đạt | RV-M1-06 (vai Tiến) | Đạt có điều kiện: RTM-R1, RTM-R2 sửa ở RTM v1.1 (M2) | ? |
| D09 | Repository, CI khung | Đạt | – (đã merge PR #1, #42 trên GitHub) | Xong | Đã xong |
| D01 *(M0, tồn đọng)* | ProjectPlan.pdf | Tiến | RV-M1-01 (vai Đạt, đại diện nhóm) | Đạt có điều kiện: mục 3 (PP-01 → PP-03) | ? |

## 2. Quyết định cần PM

| Mã | Nội dung | Đề xuất | Ảnh hưởng | PM |
|---|---|---|---|---|
| QD-06 (OI-01, R-10) | Hồ sơ Chờ bổ sung quá lâu có tự huỷ không | **Không tự huỷ trong MVP**; ghi "ngoài phạm vi MVP" ở SRS v1.2, đưa vào hướng phát triển. Lý do: chưa có căn cứ pháp lý đã đối chiếu; lịch 6 tuần không dư | Không đổi FR, backlog, RTM | ? |
| QD-07 (OI-04, R-05) | Chuyển trạng thái "Xoá nháp" chưa có FR | **Bỏ chuyển trạng thái này** khỏi sơ đồ vòng đời (sửa ở SRS v1.2 và sơ đồ trạng thái UML của M2) | Không đổi 28 FR, backlog, RTM. Phương án khác: thêm FR-29 mức C, cần thêm issue và test | ? |

## 3. Thông tin cần PM xác nhận

| # | Nội dung | Nguồn |
|---|---|---|
| X1 | Ngày đúng của SRS v0.9 và v1.0. Tệp `eHoSo_SRS_v1.0.pdf` có ngày tạo 24/10/2026 10:50 (giờ VN), trong khi PM cho biết ngày 24/10 chưa làm SRS | RV-M1-04, S-R4 |
| X2 | 4 điểm suy ra trong ProjectPlan: giờ từng công việc; % hoàn thành tới 28/10; K/T và biện pháp R5–R8; cột Kiểm thử (UAT) của Tiến | RV-M1-01, PP-01 |
| X3 | Bản Project Plan đã nộp trước hạn 15:59 ngày 27/10 là bản nào; bản ProjectPlan.pdf (tạo 28/10) có nộp bổ sung không | RV-M1-01, PP-02 |
| X4 | Đề bài P1 yêu cầu tài liệu họp trong Meetings Repository. Nếu nhóm đã họp khởi động hoặc họp chuẩn bị trình bày: ngày, hình thức, người dự, nội dung, quyết định. Không có họp thì không lập biên bản | RV-M1-01, PP-03 |

## 4. Tồn đọng chuyển sang mốc sau

| # | Việc | Phụ trách (vai) | Hạn |
|---|---|---|---|
| 1 | SRS v1.2: QD-06, QD-07, đính chính X1 (nếu cần), ghi Garage | Tiến | 08/11 |
| 2 | Sinh RTM v1.1 từ `requirements.csv` (đã đúng nguồn FR-03, BR-04), thêm cột Thiết kế | Đạt | 08/11 |
| 3 | R-09: sửa sơ đồ quy trình khi vẽ lại ở M2 | Khang | 08/11 |
| 4 | OI-02 (đối chiếu thời hạn KS-10), OI-03 (phỏng vấn cán bộ, nếu có điều kiện) | Tiến, Khang | 09/11 |
| 5 | Merge PR tài liệu M1 sau khi PM duyệt, gắn tag mốc M1 | Đạt | Sau khi PM duyệt |

## 5. Phê duyệt

PM trả lời: ? · Ngày: ? · Ghi chú: ?
