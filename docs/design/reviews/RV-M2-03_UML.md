# Phiếu review RV-M2-03

| Mục | Nội dung |
|---|---|
| Sản phẩm | D12 – Bộ sơ đồ UML, phiên bản 1.0 (12 sơ đồ; thêm ERD do RV-M2-04 review) |
| Tác giả (vai) | Mai Thành Khang (Tech Lead) |
| Người review (vai) | Đoàn Quang Anh (Backend + CSDL) – review do Claude thực hiện theo vai |
| Ngày | 04/11/2026 |
| Căn cứ | Checklist HUONG_DAN mục 6; SRS v1.1 mục 2.2, 2.3, 5; Use case v1.1 |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Có use case tổng quát; class; sequence UC-04, 08, 10, 11; state; component, deployment | Có | Thêm sơ đồ ngữ cảnh và activity |
| 2 | Vẽ bằng PlantUML, có mã `.puml` và ảnh | Có | 12 cặp tệp, PlantUML kết xuất không lỗi |
| 3 | Sơ đồ use case đủ 17 use case | Có | |
| 4 | Sơ đồ trạng thái khớp SRS mục 5 | Có | Trừ "Xoá nháp", có ghi chú QD-07 |
| 5 | Sequence khớp luồng chính, thay thế, ngoại lệ | Có | Đối chiếu từng luồng của 4 use case |
| 6 | Activity sửa R-09 | Có | Ghi nhận đúng hạn sau phê duyệt |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| U-01 | minor | Sơ đồ use case | UC-01 bước 4 ghi include UC-17, nhưng đặc tả UC-17 chỉ liệt kê 6 use case khác. Sơ đồ đang theo UC-01 | Bổ sung UC-01 vào đặc tả UC-17 ở Use case bản sau | Chờ BA |
| U-02 | minor | Sơ đồ trạng thái | SRS v1.1 mục 5 vẫn vẽ "Xoá nháp" | Thay ảnh ở SRS v1.2 | Đã sửa ở SRS v1.2 |
| U-03 | nit | Class dịch vụ | Chỉ vẽ luồng xử lý hồ sơ, không vẽ `account`, `dashboard` | Giữ nguyên vì sơ đồ tập trung vào luồng cốt lõi; ghi trong tiêu đề | Không sửa |

Mức: blocker (phải sửa trước khi qua) · major (nên sửa) · minor · nit.

## Kết luận
Đạt. Phiên bản: 1.0.
