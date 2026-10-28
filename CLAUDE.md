# e-HoSo

## Dự án là gì
Hệ thống tiếp nhận, xử lý và theo dõi hồ sơ thủ tục hành chính trực tuyến cấp xã/phường. Đây là Final Project môn INS6021 (Nhóm 1). MVP số hoá 3 thủ tục: chứng thực bản sao, đăng ký khai sinh, xác nhận tình trạng hôn nhân. **Chỉ dùng dữ liệu giả lập** (NFR-04).

## Stack
- Backend: Java 21, Spring Boot 3.5, Maven (`src/backend`), REST `/api/v1/...`, OpenAPI 3 qua springdoc.
- Frontend: React 19 + TypeScript + Vite (`src/frontend`), ESLint + Prettier, Vitest.
- Hạ tầng: PostgreSQL 17, Mailpit (SMTP thử nghiệm), Docker Compose (`docker-compose.yml`). Kho tệp S3 đang chờ chọn bản thay thế MinIO.
- Múi giờ: mọi thời điểm lưu ISO 8601 có múi giờ, tính hạn theo Asia/Ho_Chi_Minh (NFR-10).

## Thư mục quan trọng
- `docs/requirements/requirements.csv`: **nguồn sự thật** cho FR/NFR/BR, chép từ SRS v1.0. Đổi yêu cầu thì sửa ở đây.
- `docs/requirements/eHoSo_SRS_v1.1.pdf`: SRS hiện hành (ISO/IEC/IEEE 29148). `eHoSo_UseCase_v1.0.pdf` đặc tả đủ 17 use case; nên đọc trước khi làm một FR.
- `src/backend/src/main/java/vn/ehoso/`: mã backend. Logic nghiệp vụ đặt trong package `*.service` (JaCoCo đo ≥70% ở đây).
- `src/frontend/src/`: mã frontend.
- `tools/req.py`, `tools/metrics.py`: kiểm tra truy vết (RTM) và báo cáo chỉ số tuần.
- `meetings/`: biên bản họp.

## Lệnh
- Cổng chất lượng: `./scripts/check.sh` (chạy `make check`: tool tests, RTM, `mvnw verify`, lint/format/test/build frontend).
- Chạy riêng: `make backend`, `make frontend`, `make rtm`.
- Dịch vụ phụ trợ: `docker compose up -d`.
- Dev: `cd src/backend && ./mvnw spring-boot:run` (cổng 8080); `cd src/frontend && npm run dev` (proxy `/api` sang 8080).

## Quy ước riêng
- Google Java Style (Checkstyle chặn build nếu vi phạm); ESLint + Prettier cho frontend.
- **Mỗi test phải ghi mã yêu cầu nó kiểm chứng**: JUnit `@Tag("BR-03")` hoặc ghi trong tên test Vitest `it('[FR-12] ...')`. `tools/req.py rtm` dựa vào đó để lập RTM.
- Tiêu đề PR theo Conventional Commits và nêu mã yêu cầu, ví dụ `feat(dossier): nộp hồ sơ (FR-09)`. PR được squash merge.
- Mọi chuyển trạng thái hồ sơ phải kiểm tra phía máy chủ theo sơ đồ vòng đời (SRS mục 5) và ghi nhật ký trong cùng giao dịch (FR-23, NFR-05).
- Quyền kiểm tra phía máy chủ theo ma trận SRS mục 3.7 (FR-05).

## Vùng dễ vỡ
- Tính ngày hẹn trả (BR-02, BR-03, BR-05): mốc 15:00, 17:00, ngày làm việc và ngày nghỉ. Đây là tiêu chí nghiệm thu số 2, nên cần test cho từng trường hợp: trước/sau 15:00, thứ Sáu, ngày nghỉ lễ.
- Tra cứu công khai (FR-12) tuyệt đối không được trả về thông tin cá nhân.
- Nhật ký (FR-23) chỉ được thêm, không có API sửa hay xoá.

## Quy trình làm việc
@.claude/sdlc/RULES.md
