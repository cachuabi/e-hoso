# Phiếu review RV-M2-07

| Mục | Nội dung |
|---|---|
| Sản phẩm | D16 – Test Plan, phiên bản 1.0 |
| Tác giả (vai) | Mai Xuân Đạt (QA + DevOps) |
| Người review (vai) | Đỗ Văn Tiến (PM) – review do Claude thực hiện theo vai |
| Ngày | 04/11/2026 |
| Căn cứ | Checklist HUONG_DAN mục 6; SRS v1.2 mục 11; RTM v1.1; SDD v1.1 mục 9, 10 |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Phạm vi, chiến lược, tiêu chí vào/ra, môi trường, lịch, rủi ro, ca chính | Có | |
| 2 | Có ca BR-03: trước/sau 15:00, thứ Sáu, ngày lễ | Có | CK-H01 → CK-H17, kết quả mong đợi tính lại bằng script |
| 3 | Có ca cho 5 tiêu chí nghiệm thu | Có | Mục 5.6 |
| 4 | 47/47 yêu cầu có ca kiểm thử | Có | Kiểm tra bằng script |
| 5 | Không ghi kết quả khi chưa chạy | Có | |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| T-01 | major | Mục 5 | Bản nháp thiếu ca rõ ràng cho 11 yêu cầu mức M và FR-27, FR-28 | Thêm mục 5.5 | Đã sửa |
| T-02 | major | Mục 4, 7 | Môi trường Claude không có Docker; K1–K7 và các ca trên Compose cần máy khác | Nhóm cung cấp máy có Docker hoặc chạy trên GitHub Actions | Chờ PM |
| T-03 | major | CK-U02 | Cần 3 người dùng thật trước 10/11 | PM sắp xếp, nếu không kịp thì ghi chưa kiểm | Chờ PM |

## Kết luận
Đạt có điều kiện: T-02, T-03 cần xử lý trước khi chạy kiểm thử cuối. Phiên bản: 1.0.
