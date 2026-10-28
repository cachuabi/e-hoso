# ADR-001: Kho tệp tương thích S3 thay MinIO

| Mục | Nội dung |
|---|---|
| Trạng thái | Đã quyết định, chờ kiểm chứng khi tích hợp (K1–K7) |
| Ngày | 28/10/2026 |
| Người quyết định | Mai Thành Khang (Tech Lead); Claude chốt thay theo uỷ quyền của PM Đỗ Văn Tiến (QD-02) |
| Người review | Đoàn Quang Anh (Backend + CSDL): RV-M2-01 Đạt (review do Claude thực hiện theo vai) |
| Liên quan | Issue #40, OI-05; DR-05; FR-08; NFR-01, NFR-04, NFR-05, NFR-08 |
| Tài liệu đầy đủ | [`../eHoSo_KhoTepS3_v1.0.pdf`](../eHoSo_KhoTepS3_v1.0.pdf) (D17) |

## Bối cảnh

SRS v1.0 và Project Plan chọn MinIO. Từ 09/2026, Docker Hub không còn `minio/minio`, quay.io từ chối tải ẩn danh, và kho GitHub của MinIO đã chuyển sang lưu trữ. Vì vậy hệ thống chưa có dịch vụ lưu tệp trong `docker-compose.yml`. SRS v1.1 đã đổi ràng buộc thành "kho tệp tương thích S3".

Nhu cầu bắt buộc: put/get/delete theo khoá; presigned GET có thời hạn 5 phút (NFR-04); path-style; SigV4 với AWS SDK for Java v2; tệp tối đa 10 MB, một lệnh PutObject (FR-08); không mất tệp khi khởi động lại (NFR-05); toàn hệ thống vừa máy 8 GB RAM, chạy bằng một lệnh (NFR-08).

## Phương án đã xét

| Phương án | Kết luận |
|---|---|
| Tự build MinIO từ mã nguồn | Loại: không còn bảo trì, không có image chính thức |
| SeaweedFS | 4,25/5: trưởng thành; nhiều thành phần, cấu hình khoá phức tạp hơn |
| **Garage** | **4,30/5: nhẹ nhất, chạy sản xuất từ 2020; không có ACL, policy, versioning (không cần)** |
| RustFS | 4,30/5: độ phủ S3 rộng, có console; mới GA 16/09/2026 |

## Quyết định

Chọn **Garage** (`dxflrs/garage`, ghim v2.4.1). Dự phòng: **RustFS** (`rustfs/rustfs`, ghim 1.0.0). Lý do phân định khi bằng điểm: tốt hơn ở NFR-08 (Must) và độ trưởng thành, giảm rủi ro demo (R4).

## Hệ quả

- Backend truy cập kho tệp qua giao diện riêng (`FileStoragePort`) dùng AWS SDK for Java v2, `forcePathStyle(true)`; đổi sản phẩm chỉ sửa `docker-compose.yml` và biến môi trường.
- `S3Presigner` ký theo endpoint công khai; `S3Client` dùng endpoint nội bộ trong mạng Compose.
- Ghi tệp và ghi CSDL không cùng giao dịch: ghi tệp trước, ghi CSDL sau, lỗi thì xoá tệp vừa ghi (RV-M2-01, Q-01). Chi tiết ở SDD.
- Khoá lưu trữ không chứa tên tệp hay dữ liệu cá nhân, ví dụ `ho-so/<mã tham chiếu>/<uuid>` (Q-02).
- Chuyển sang RustFS nếu một trong K1, K3, K4, K6, K7 không đạt và không khắc phục được trong 2 giờ kiểm chứng.

## Việc còn lại (issue #40)

- [x] Ghi quyết định và lý do vào `docs/design/` (ADR này)
- [ ] Thêm dịch vụ `storage` vào `docker-compose.yml` kèm healthcheck (Q-03); chạy kiểm chứng K1–K7, ghi kết quả thật vào ADR
- [x] SRS: không cần yêu cầu thay đổi vì SRS v1.1 đã trung lập về sản phẩm; OI-05 đã đóng ở SRS v1.1
