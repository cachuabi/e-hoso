# Phiếu review RV-M1-02

| Mục | Nội dung |
|---|---|
| Sản phẩm | D04 – Báo cáo khảo sát yêu cầu v1.0 |
| Tác giả (vai) | Đoàn Quang Anh (Backend, khảo sát) |
| Người review (vai) | Đỗ Văn Tiến (PM + BA) – review do Claude thực hiện theo vai |
| Ngày | 28/10/2026 |
| Căn cứ | SRS v1.1 (cột Nguồn); HUONG_DAN mục 6; kiểm tra chéo tự động mã KS |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Có đủ KS-01 → KS-14 | Có | 14/14 |
| 2 | Mọi mã KS mà SRS dẫn đều có trong báo cáo, và ngược lại | Có | Kiểm tra tự động: không thiếu, không thừa |
| 3 | Bảng truy vết KS → yêu cầu khớp cột Nguồn của SRS | Không | Phát hiện KS-R1 |
| 4 | Nêu rõ phương pháp và giới hạn (chưa phỏng vấn) | Có | Mục 2, giả định A2 |
| 5 | Căn cứ pháp lý có nguồn | Có | Mục 8; các điểm "cần đối chiếu" đã ghi rõ |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| KS-R1 | minor | Mục 4, 6 (KS-01) | SRS v1.1 ghi nguồn BR-04 là KS-01 (R-07) nhưng bảng KS-01 không có BR-04 | Thêm BR-04 vào KS-01 | Đã sửa ở v1.1 |
| KS-R2 | minor | Mục 6, đoạn cuối | Ghi FR-03 lấy từ "lựa chọn của nhóm"; SRS v1.1 ghi nguồn NFR-03 | Sửa theo SRS v1.1 | Đã sửa ở v1.1 |
| KS-R3 | minor | KS-08, KS-10, KS-13, KS-14 | Một số căn cứ chưa đối chiếu điều khoản cụ thể hoặc bản hợp nhất | Đã có việc theo dõi: OI-02 (09/11), mục 7 (08/11). Không chặn M1 | Theo dõi |

## Kết luận
**Đạt** với phiên bản 1.1 (`eHoSo_KhaoSat_v1.1`).
