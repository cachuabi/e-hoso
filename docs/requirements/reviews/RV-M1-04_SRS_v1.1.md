# Phiếu review RV-M1-04

| Mục | Nội dung |
|---|---|
| Sản phẩm | D05b – SRS v1.1 |
| Tác giả (vai) | Đỗ Văn Tiến (BA) |
| Người review (vai) | Mai Xuân Đạt (QA) – review do Claude thực hiện theo vai |
| Ngày | 28/10/2026 |
| Căn cứ | Báo cáo review SRS v1.0 (R-01 → R-10, CR-01); QD-02, QD-05; ISO/IEC/IEEE 29148 |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | CR-01 áp dụng đúng ở mục 2.5 và 10 (6 tuần, 325 giờ, Sprint 1 09/11–20/11, Sprint 2 21/11–29/11) | Có | |
| 2 | R-03, R-04, R-06, R-07, R-08 đã áp dụng | Có | R-03 bằng đính chính, giữ nguyên dòng 1.0 |
| 3 | Số yêu cầu không đổi: 28 FR, 10 NFR, 9 BR, 8 DR | Có | Kiểm tra tự động |
| 4 | Tài liệu tham chiếu mục 1.4 tồn tại | Có | Sau khi sửa S-R3 |
| 5 | Lịch sử phiên bản đúng sự thật | Không | Phát hiện S-R1, S-R4 |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| S-R1 | major | Lịch sử phiên bản, dòng 1.0 | Dẫn tới biên bản review 24/10 không tồn tại; PM xác nhận không có buổi họp này (QD-05) | Thêm đính chính, giữ nguyên dòng gốc | Đã sửa |
| S-R2 | minor | Mục 2.5, 12 | OI-05 và OI-06 đã có quyết định nhưng chưa đóng | Đóng OI-05 (Garage, QD-02), OI-06 (QD-05) | Đã sửa |
| S-R3 | minor | Mục 1.4 | Tham chiếu khảo sát và use case v1.0; thiếu tài liệu quyết định kho tệp | Trỏ tới bản 1.1 và thêm eHoSo_KhoTepS3_v1.0 | Đã sửa |
| S-R4 | minor | Lịch sử phiên bản, dòng 0.9 và 1.0 | PM cho biết ngày 24/10 chưa làm SRS, nhưng tệp `eHoSo_SRS_v1.0.pdf` có ngày tạo 24/10/2026 10:50 (giờ VN) | PM xác nhận ngày đúng của bản 0.9 và 1.0; nếu khác, thêm đính chính | Chờ PM |
| S-R5 | – | OI-01, OI-04; R-09 | Chưa quyết | PM quyết OI-01, OI-04 tại họp 01/11; R-09 sửa khi vẽ lại sơ đồ ở M2 | Chờ PM / M2 |

## Kết luận
**Đạt có điều kiện.** Điều kiện: PM xác nhận S-R4 và quyết OI-01, OI-04.
