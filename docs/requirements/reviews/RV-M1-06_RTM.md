# Phiếu review RV-M1-06

| Mục | Nội dung |
|---|---|
| Sản phẩm | D08 – RTM v1.0 (PDF và CSV) |
| Tác giả (vai) | Mai Xuân Đạt (QA) |
| Người review (vai) | Đỗ Văn Tiến (PM + BA) – review do Claude thực hiện theo vai |
| Ngày | 28/10/2026 |
| Căn cứ | SRS v1.1; Use case v1.1; kiểm tra chéo tự động |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Đủ 47 yêu cầu, không thừa | Có | |
| 2 | Số yêu cầu mức M = 37; có nguồn = 35/47 | Có | Đếm lại từ CSV |
| 3 | Cột Use case khớp tài liệu use case | Có | FR-05 "Mọi UC", FR-14 qua UC-17, FR-23 theo khoảng UC |
| 4 | Cột Nguồn khớp SRS hiện hành | Không | Phát hiện RTM-R1 |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| RTM-R1 | minor | FR-03, BR-04 | Nguồn ghi "—" theo SRS v1.0; SRS v1.1 ghi NFR-03 và KS-01, nên chỉ tiêu "có nguồn" sẽ là 37/47 | `docs/requirements/requirements.csv` trong repo đã ghi đúng nguồn theo SRS v1.1 (kiểm tra 28/10); chỉ cần sinh lại tài liệu RTM ở v1.1 (M2 bước 7) | Chuyển sang RTM v1.1 |
| RTM-R2 | nit | FR-23 | Ghi "UC-04→06, UC-08→12" dễ đọc nhầm | Liệt kê đầy đủ UC | Chuyển sang RTM v1.1 |

## Kết luận
**Đạt có điều kiện** cho mốc M1: RTM-R1, RTM-R2 xử lý ở RTM v1.1 (M2).
