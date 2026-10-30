# Phiếu review RV-M1-03

| Mục | Nội dung |
|---|---|
| Sản phẩm | D06 – Mô hình và đặc tả use case v1.0 |
| Tác giả (vai) | Đỗ Văn Tiến (BA) |
| Người review (vai) | Mai Thành Khang (Tech Lead) – review do Claude thực hiện theo vai |
| Ngày | 28/10/2026 |
| Căn cứ | SRS v1.1; RTM v1.0; kiểm tra chéo tự động FR ↔ UC |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Đủ 17 use case, mỗi UC có tác nhân, tiền/hậu điều kiện, luồng chính, thay thế, ngoại lệ | Có | |
| 2 | 28/28 FR thuộc ít nhất một UC | Có | Kiểm tra tự động |
| 3 | Mọi mã FR/NFR/BR dẫn trong tài liệu có trong SRS | Có | |
| 4 | Tiền/hậu điều kiện khớp vòng đời hồ sơ và BR-04 | Có | |
| 5 | Ngưỡng số (phút, lần, MB) trong UC trùng SRS | Có | Không có ngưỡng tự đặt |
| 6 | Danh mục, đặc tả và ma trận mục 7 nhất quán | Không | Phát hiện UC-R1 |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| UC-R1 | minor | Mục 4 và đặc tả UC-10 | Luồng 2a của UC-10 gửi email gia hạn do xác minh (FR-14), ma trận mục 7 ghi UC-10 include UC-17, nhưng dòng "Yêu cầu" của UC-10 không có FR-14 | Thêm FR-14 vào UC-10 | Đã sửa ở v1.1 |
| UC-R2 | nit | Mục 6, sơ đồ vòng đời | Chuyển trạng thái "Xoá nháp" chưa có FR (OI-04) | Theo quyết định OI-04 của PM | Chờ PM |

## Kết luận
**Đạt** với phiên bản 1.1 (`eHoSo_UseCase_v1.1`). UC-R2 xử lý theo quyết định OI-04.
