# Phiếu review RV-M2-02

| Mục | Nội dung |
|---|---|
| Sản phẩm | D11 – Tài liệu thiết kế phần mềm (SDD), phiên bản 1.0 |
| Tác giả (vai) | Mai Thành Khang (Tech Lead) |
| Người review (vai) | Đỗ Văn Tiến (PM) – review do Claude thực hiện theo vai |
| Ngày | 04/11/2026 |
| Căn cứ | Checklist HUONG_DAN mục 6; SRS v1.1; Use case v1.1; D17; RV-M2-01; `requirements.csv` |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Đủ góc nhìn IEEE 1016 theo checklist và lý do thiết kế | Có | Mục 3 → 11, 13 |
| 2 | Mỗi FR thuộc ít nhất 1 thành phần | Có | 28/28, kiểm tra bằng script |
| 3 | Mã FR, NFR, BR khớp `requirements.csv` | Có | 47/47, không có mã lạ |
| 4 | Kiến trúc khớp stack trong Project Plan | Có | Kho tệp theo QD-02 |
| 5 | Xử lý Q-01, Q-02, Q-03, R-09 | Có | Mục 6.3, 11, 8.1 |
| 6 | Không ghi kết quả đo hay kiểm thử khi chưa chạy | Có | Ngân sách RAM ghi "chưa đo"; ví dụ BR-03 ghi "chưa phải kết quả kiểm thử" |
| 7 | Các quyết định còn chờ được nêu rõ | Có | Mục 1.5, 15 |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| S-01 | major | Mục 7, 12.2 | Bản nháp chỉ nêu quyền tải xuống, chưa nêu quyền tải lên và xoá tệp theo trạng thái | Bổ sung vào mục 12.2 | Đã sửa trong v1.0 |
| S-02 | major | Mục 1.5, V-01, V-02 | Thiết kế dựa trên phương án A của QD-06, QD-07; issue #43, #44 đóng nhưng không ghi phương án | PM xác nhận | Đóng: PM xác nhận phương án A ngày 04/11; SDD v1.1 |
| S-03 | minor | Mục 13, V-11 | Thêm package và dịch vụ là điểm dừng của `RULES.md` | PM duyệt QD-10 trước Sprint 1 | Đóng: PM đồng ý ngày 04/11 |
| S-04 | minor | Mục 6.3, V-09 | Ví dụ khoá lưu trữ trong ADR-001 (PR #47) không dùng được vì mã tham chiếu chỉ có khi nộp | Sửa ADR-001 | Chờ cập nhật PR #47 |
| S-05 | minor | Mục 11 | Cấu hình nháp ở D17 mở cổng 3900, khác thiết kế cổng 9443 | Áp dụng khi tích hợp; chạy K4 qua cổng 9443 | Chuyển sang việc tích hợp (vấn đề mở 4) |
| S-06 | minor | Mục 15 | V-03 → V-07, V-10 cần BA chốt để Test Plan có kỳ vọng rõ | Chốt trong phiếu phê duyệt M2 | Chờ PM |
| S-07 | nit | Mục 4 → 9 | Chữ trong sơ đồ lớn hơi nhỏ khi in A4 | Kèm PNG độ phân giải cao và mã `.puml` | Đã xử lý |

Mức: blocker (phải sửa trước khi qua) · major (nên sửa) · minor · nit.

## Kết luận
Đạt có điều kiện ở v1.0 (S-02). Điều kiện đã đóng ngày 04/11/2026, bản hiện hành là v1.1.
