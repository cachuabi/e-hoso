# Phiếu review RV-M2-01

| Mục | Nội dung |
|---|---|
| Sản phẩm | D17 – Quyết định kho tệp tương thích S3 thay MinIO, v1.0 |
| Tác giả (vai) | Mai Thành Khang (Tech Lead) |
| Người review (vai) | Đoàn Quang Anh (Backend + CSDL) – review do Claude thực hiện theo vai |
| Ngày | 28/10/2026 |
| Căn cứ | SRS v1.1: FR-08, NFR-01, NFR-04, NFR-05, NFR-08, DR-05; GitHub issue #40 |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Nhu cầu lưu tệp dẫn đúng mã yêu cầu trong SRS v1.1 | Có | N1–N8 |
| 2 | Phương án chọn đáp ứng mọi nhu cầu mức bắt buộc | Có | Presigned URL, path-style, SigV4, PutObject 10 MB |
| 3 | Có phương án dự phòng và điều kiện chuyển rõ ràng | Có | RustFS; K1, K3, K4, K6, K7 |
| 4 | Không ghi kết quả kiểm chứng khi chưa chạy | Có | Ô kết quả để "?" |
| 5 | Backend không phụ thuộc API riêng của sản phẩm | Có | Nguyên tắc 1, 6 ở mục 8 |
| 6 | Nhất quán giữa siêu dữ liệu (PostgreSQL) và nội dung tệp (kho S3) | Chưa đề cập | Phát hiện Q-01 |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| Q-01 | major | Mục 8 | Ghi tệp vào kho S3 và ghi bản ghi DR-05 vào PostgreSQL không nằm trong một giao dịch, nên có thể để lại tệp "mồ côi" hoặc bản ghi trỏ tới tệp không tồn tại | Thiết kế trong SDD: ghi tệp trước, ghi CSDL sau; nếu giao dịch CSDL lỗi thì xoá tệp vừa ghi (bù trừ). Không ảnh hưởng lựa chọn sản phẩm | Chuyển sang SDD (D11) |
| Q-02 | minor | Mục 8 | Chưa quy ước khoá lưu trữ (object key) cho DR-05 | Quy ước trong SDD và ERD, ví dụ `ho-so/<mã tham chiếu>/<uuid>` (không chứa tên tệp hay dữ liệu cá nhân, NFR-04) | Chuyển sang SDD (D11), ERD (D13) |
| Q-03 | minor | Mục 9, cấu hình nháp | Chưa có healthcheck cho dịch vụ `storage`; backend có thể khởi động trước khi kho tệp sẵn sàng | Thêm `healthcheck` và `depends_on: condition: service_healthy` khi tích hợp | Chuyển sang việc tích hợp (vấn đề mở 4) |

## Kết luận
**Đạt.** Lựa chọn Garage phù hợp nhu cầu backend. Q-01 → Q-03 không đổi quyết định, được chuyển vào SDD và việc tích hợp.
