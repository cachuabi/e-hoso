# Chương 4. Xây dựng hệ thống (System Development)

## 4.1 Kiến trúc

**Tổng quan.** e-HoSo là một monolith chia theo module (TK-01), gồm một backend Spring Boot và một SPA React. Backend có 9 module theo nghiệp vụ: `security`, `account`, `catalog`, `dossier`, `file`, `workflow`, `notification`, `audit`, `dashboard`. Mỗi module chia 3 tầng: controller → service → repository. Logic nghiệp vụ chỉ nằm trong các gói `*.service`, và JaCoCo đo độ phủ tại đây.

![Thành phần và phụ thuộc giữa các module](docs/design/uml/03_thanh_phan.png)

**Mô hình client–server.** Hệ thống theo mô hình client–server nhiều tầng. Trình duyệt chỉ giữ giao diện và access token trong bộ nhớ. Mọi quy tắc nghiệp vụ và kiểm tra quyền chạy ở máy chủ. Proxy Caddy là cửa vào duy nhất: kết thúc TLS, phục vụ tệp tĩnh của SPA, chuyển `/api/v1` sang backend và mở cổng 9443 cho URL ký tới kho tệp. Backend, CSDL và kho tệp không mở cổng ra ngoài.

![Kiến trúc client–server nhiều tầng](docs/design/uml/25_kien_truc_client_server.png){w=12}

**Ánh xạ logic sang vật lý.** Hai sơ đồ dưới đây cho thấy mỗi thành phần logic chạy ở đâu. Sơ đồ triển khai cho thấy các container trong Docker Compose. Sơ đồ ánh xạ cho thấy thành phần nào là phần mềm, phần cứng hay thao tác thủ công. Phần thủ công là những bước pháp luật đòi hỏi làm trên giấy và hệ thống không thay thế: đối chiếu bản chính khi chứng thực, ký giấy và đóng dấu, trao kết quả tại quầy.

![Triển khai bản demo bằng Docker Compose](docs/design/uml/04_trien_khai.png)

![Ánh xạ thành phần logic sang phần cứng, phần mềm và thao tác thủ công](docs/design/uml/26_anh_xa_logic_vat_ly.png){w=14}

Bảng: Phân bổ tài nguyên các container (giới hạn bộ nhớ trong `docker-compose.yml`)

| Container | Ảnh | Vai trò | Giới hạn RAM |
|---|---|---|---|
| `proxy` | Caddy | TLS, tệp tĩnh SPA, chuyển tiếp API, cổng URL ký | 64 MB |
| `backend` | eclipse-temurin 21 | Ứng dụng Spring Boot | 1 GB |
| `db` | postgres:17-alpine | CSDL, volume `db-data` | 512 MB |
| `storage` | dxflrs/garage:v2.5.0 | Kho tệp tương thích S3 | 256 MB |
| `mail` | axllent/mailpit:v1.32 | SMTP thử nghiệm, giao diện web xem thư | 64 MB |

## 4.2 Nền tảng, ngôn ngữ và công cụ

Bảng: Nền tảng và công cụ

| Lớp | Lựa chọn | Lý do |
|---|---|---|
| Ngôn ngữ backend | Java 21 | Theo Project Plan; kiểu tĩnh, hệ sinh thái Spring |
| Framework | Spring Boot 3.5: Web, Security (OAuth2 resource server cho JWT), Data JPA, Mail; springdoc OpenAPI | Giao dịch khai báo bằng `@Transactional` phục vụ NFR-05; sinh OpenAPI 3 tự động (NFR-07) |
| CSDL | PostgreSQL 17, Flyway | `timestamptz`, `jsonb`, trigger, ràng buộc `CHECK` liên cột |
| Kho tệp | Garage (S3), AWS SDK for Java v2 | Thay MinIO (ADR-001) |
| Frontend | React 19, TypeScript, Vite, react-router | Theo Project Plan |
| Kiểm thử | JUnit 5, MockMvc, Testcontainers (PostgreSQL thật), Vitest, Playwright, axe, k6, OWASP ZAP | Test tích hợp trên CSDL thật; E2E trên Docker Compose |
| Chất lượng | Checkstyle (Google Java Style), JaCoCo ≥ 70% tầng service, ESLint, Prettier | NFR-07 |
| Hạ tầng | Docker Compose, Caddy, Mailpit; GitHub Actions | NFR-08; CI trên x86 và arm64 |

**Các phương án kiến trúc đã cân nhắc.** Bảng dưới tóm tắt các quyết định thiết kế chính (SDD mục 13) và phương án bị loại.

Bảng: Các phương án kiến trúc đã cân nhắc

| Mã | Quyết định | Phương án bị loại | Lý do chọn | Đánh đổi |
|---|---|---|---|---|
| TK-01 | Monolith theo module | Microservices | 5 người, 6 tuần, một xã; NFR-05 cần giao dịch chung cho trạng thái và nhật ký | Khó tách dịch vụ về sau |
| TK-02 | `DossierLifecycle` với bảng 13 cặp chuyển | Mẫu State; thư viện máy trạng thái | Dễ đối chiếu với SRS mục 5, không thêm thư viện | Hành vi riêng theo trạng thái nằm ở service |
| TK-03 | Khoá lạc quan bằng cột `version` | Khoá bi quan toàn luồng | Xung đột hiếm (2–4 CBMC) | Người thao tác sau phải tải lại |
| TK-04 | Hàng đợi email trong CSDL | Gửi trong giao dịch; RabbitMQ | Lỗi SMTP không làm hỏng nghiệp vụ; không thêm dịch vụ | Email có độ trễ vài giây |
| TK-05 | Tải lên qua backend, tải xuống bằng URL ký | Tải lên trực tiếp bằng URL ký | Phải kiểm nội dung tệp trước khi nhận (FR-08) | Backend chịu tải băng thông khi tải lên |
| TK-07 | JWT chỉ mang id, đọc vai trò từ CSDL mỗi yêu cầu | JWT mang vai trò | Đổi vai trò, khoá tài khoản có hiệu lực ngay (UC-15) | Thêm một truy vấn theo khoá chính mỗi yêu cầu |
| TK-08 | Biểu mẫu sinh từ `form_schema` JSON | Viết cứng 3 biểu mẫu | Một nguồn quy tắc cho cả hai phía | Chỉ hỗ trợ kiểu trường cơ bản |
| TK-09 | Nhãn hạn, đúng hạn tính khi đọc | Cột tính sẵn, tác vụ định kỳ | Luôn đúng theo thời điểm hiện tại | Lọc trong bộ nhớ, hợp với quy mô MVP (L-04) |
| ADR-001 | Garage | MinIO, SeaweedFS, RustFS | MinIO thượng nguồn ngừng phát hành ảnh Docker; Garage nhẹ, tương thích S3, đạt 7 bước kiểm chứng K1–K7 | Ít tính năng quản trị hơn |

**Tham số cấu hình vận hành.** Các ngưỡng nghiệp vụ và kỹ thuật được đặt thành hằng số có tên hoặc khoá cấu hình, không viết rải rác trong mã.

Bảng: Tham số cấu hình vận hành

| Tham số | Giá trị | Nơi đặt | Căn cứ |
|---|---|---|---|
| Mốc nhận hồ sơ, giờ kết thúc ngày làm việc | 15:00, 17:00 | `ehoso.deadline.cutoff-hour`, `end-hour` | BR-03 |
| Múi giờ | Asia/Ho_Chi_Minh | `spring.jackson.time-zone`, đồng hồ tiêm vào | NFR-10 |
| Hạn access token, refresh token | 30 phút, 7 ngày | `ehoso.jwt.ttl-minutes`, `ehoso.refresh.ttl-days` | NFR-03 |
| Liên kết xác thực email, đặt lại mật khẩu | 24 giờ, 30 phút | `AuthService` | FR-01, FR-03 |
| Khoá đăng nhập | 5 lần sai → 15 phút | `AuthService` | FR-02 |
| Khoá phê duyệt | 3 lần sai → 15 phút | `ApprovalAttempts` | UC-11 luồng 4a |
| Giới hạn tệp | 10 MB mỗi tệp, 30 MB mỗi hồ sơ; PDF, JPG, PNG | `AttachmentService`, `spring.servlet.multipart` | FR-08 |
| Hạn URL ký | 5 phút | `AttachmentService` | NFR-04 |
| Gửi email | Mỗi 5 giây, tối đa 20 email một lượt; thử lại 3 lần, giãn cách 1, 2, 3 phút | `ehoso.notification.delay-ms`, `NotificationDispatcher` | FR-14, UC-17 |
| Chặn dò tra cứu | Quá 10 lần sai trong 10 phút → chặn địa chỉ 10 phút | `LookupThrottle` | FR-12 |
| Gia hạn xác minh | 3 ngày làm việc, một lần | `ReviewService` | BR-05 |
| Nhãn sắp đến hạn | Dưới 2 giờ (trong ngày), dưới 24 giờ (ngày làm việc) | `DeadlineLabels` | BR-06 |

## 4.3 API và phân quyền

API REST có tiền tố `/api/v1`. Mô tả OpenAPI 3 được sinh tại `/v3/api-docs`; bản thiết kế v1.1 nằm ở `docs/design/api/openapi.yaml`. Các thao tác thay đổi hồ sơ của cán bộ gửi kèm `version` để chống ghi đè.

Bảng: Danh mục endpoint

| Nhóm | Phương thức và đường dẫn | Chức năng | Vai trò |
|---|---|---|---|
| Xác thực | `POST /auth/register`, `/verify-email`, `/resend-verification` | Đăng ký, xác thực email | Khách |
| | `POST /auth/login`, `/refresh`, `/logout` | Phiên đăng nhập, đổi refresh token qua cookie | Khách; đăng xuất cần đăng nhập |
| | `POST /auth/password-reset`, `/password-reset/confirm` | Đặt lại mật khẩu | Khách |
| Danh mục | `GET /procedures`, `/procedures/{code}` | Danh mục thủ tục | Khách |
| | `PUT /admin/procedures/{code}`; `GET`, `POST /admin/holidays`; `DELETE /admin/holidays/{day}` | Cấu hình thủ tục, lịch nghỉ | Quản trị viên |
| Hồ sơ của tôi | `GET`, `POST /dossiers`; `GET`, `PUT /dossiers/{id}` | Danh sách, tạo nháp, xem, sửa biểu mẫu | Người dân |
| | `POST /dossiers/{id}/submit`, `/resubmit`, `/withdraw`; `GET /dossiers/{id}/history` | Nộp, nộp lại, rút, lịch sử | Người dân |
| Tra cứu | `POST /public/lookup` | Tra cứu công khai 4 trường | Khách |
| Tệp | `POST /dossiers/{id}/attachments`; `DELETE /attachments/{id}` | Tải lên, xoá tệp | Người dân, CCTP |
| | `GET /attachments/{id}/download-url` | Cấp URL ký 5 phút | Người dân, CBMC, CCTP, Lãnh đạo |
| Xử lý | `GET /staff/dossiers`, `/staff/dossiers/{id}`, `/staff/dossiers/{id}/logs` | Hàng đợi, chi tiết, nhật ký | CBMC, CCTP, Lãnh đạo (nhật ký: cả Quản trị viên) |
| | `GET /staff/officers`; `POST .../accept`, `/request-supplement`, `/reject`, `/assign`, `/return-result` | Tiếp nhận, bổ sung, từ chối, phân công, trả kết quả | CBMC |
| | `POST .../verification`, `/submit-approval` | Xác minh, trình | CCTP |
| | `POST .../approve`, `/send-back` | Phê duyệt, trả lại | Lãnh đạo |
| Dashboard | `GET /dashboard`, `/dashboard/export.csv` | Số liệu, xuất CSV | Lãnh đạo |
| Quản trị | `GET`, `POST /admin/users`; `PATCH /admin/users/{id}` | Tài khoản cán bộ | Quản trị viên |

**Phân quyền hai lớp.**

- **Lớp 1, theo vai trò.** Quy tắc theo URL đặt tập trung trong `SecurityConfig`, nên dễ đối chiếu với ma trận SRS mục 3.7 (điểm lệch L-02). `ActorAuthenticationConverter` đọc vai trò và trạng thái từ CSDL ở mỗi yêu cầu.
- **Lớp 2, theo quyền sở hữu.** Service kiểm tra người dân chỉ thao tác hồ sơ của mình, và CCTP chỉ thao tác hồ sơ được giao. Hồ sơ của người khác trả 404 để không lộ sự tồn tại.

Bảng: Ma trận phân quyền (SRS mục 3.7)

| Thao tác | Khách | Người dân | CBMC | CCTP | Lãnh đạo | Quản trị |
|---|---|---|---|---|---|---|
| Xem danh mục, tra cứu (FR-06, FR-12) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Nộp, bổ sung, rút hồ sơ của mình (FR-07 → FR-11) | – | ✓ | – | – | – | – |
| Tiếp nhận, phân công, trả kết quả (FR-15 → FR-18, FR-22) | – | – | ✓ | – | – | – |
| Thẩm định, trình, xác minh (FR-19, FR-20) | – | – | – | Hồ sơ được giao | – | – |
| Phê duyệt, trả lại (FR-21) | – | – | – | – | ✓ | – |
| Xem nhật ký hồ sơ (FR-23) | – | Chỉ mốc trạng thái | ✓ | Hồ sơ được giao | ✓ | ✓ |
| Dashboard, xuất CSV (FR-25, FR-26) | – | – | – | – | ✓ | – |
| Tài khoản cán bộ, cấu hình (FR-04, FR-27, FR-28) | – | – | – | – | – | ✓ |

## 4.4 Giao diện

Các ảnh dưới đây được chụp tự động bởi Playwright trong lần chạy E2E lưu ở nhánh `evidence`. Màn hình người dân chụp ở độ rộng 360 px; màn hình cán bộ chụp ở 1280 px. Dữ liệu trong ảnh là dữ liệu giả lập.

[[ROW]]
![Tra cứu công khai bằng mã và 4 số cuối định danh](docs/report/img/02-tra-cuu.png)
![Nộp hồ sơ chứng thực: biểu mẫu riêng của thủ tục](docs/report/img/03-tao-ho-so.png)
![Hồ sơ của tôi: danh sách, trạng thái, mã tham chiếu](docs/report/img/06-ho-so-cua-toi.png)
[[/ROW]]

![Hàng đợi hồ sơ của cán bộ một cửa, cũ nhất trước, có nhãn hạn](docs/report/img/07-hang-doi-cbmc.png){w=15}

![Tiếp nhận hồ sơ: đối chiếu bản chính, yêu cầu bổ sung, từ chối, nhật ký](docs/report/img/08-tiep-nhan.png){w=11}

![Thẩm định: ý kiến, dự thảo kết quả, trình phê duyệt](docs/report/img/11-tham-dinh.png){w=11}

![Phê duyệt bằng ký mô phỏng (nhập lại mật khẩu) hoặc trả lại](docs/report/img/12-phe-duyet.png){w=11}

![Dashboard lãnh đạo: theo trạng thái, đúng hạn, trễ hạn](docs/report/img/13-dashboard.png){w=15}

## 4.5 Quản lý dữ liệu

- **Migration.** Lược đồ quản lý bằng Flyway: `V1__init.sql` tạo 13 bảng, ràng buộc, chỉ mục và trigger; `V2__seed.sql` nạp 3 thủ tục, thành phần hồ sơ và lịch nghỉ. Hibernate không được tự sửa lược đồ (`ddl-auto=none`).
- **Dữ liệu giả lập.** Mọi dữ liệu là giả lập (NFR-04). Năm tài khoản demo, mỗi vai một tài khoản với email miền `ehoso.local`, chỉ được tạo khi bật `DEMO_ENABLED`. Bật mặc định trong `docker-compose.yml` và tắt mặc định khi chạy ngoài Compose.
- **Kho tệp.** Nội dung tệp nằm ở Garage, siêu dữ liệu ở bảng `attachment`. Khoá lưu trữ `ho-so/<id hồ sơ>/<uuid>` không chứa tên tệp hay dữ liệu cá nhân. Hai nơi lưu không thể chung một giao dịch, nên hệ thống dùng bù trừ:
  1. Kiểm tra quyền, loại tệp, dung lượng.
  2. Ghi đối tượng vào kho.
  3. Mở giao dịch, khoá dòng hồ sơ, kiểm tra lại, ghi bản ghi.
  4. Nếu bước 3 lỗi, xoá đối tượng vừa ghi.
- **Độ bền.** Dữ liệu CSDL và kho tệp nằm trên volume Docker nên giữ nguyên khi khởi động lại. Phép thử K6 đếm được 5 hồ sơ trước và 5 hồ sơ sau khi khởi động lại. `docker compose down -v` xoá sạch dữ liệu để demo lại từ đầu.
- **Sao lưu.** Bản MVP chưa có cơ chế sao lưu tự động. Khi cần, có thể sao lưu thủ công bằng `pg_dump` cho CSDL và sao chép volume dữ liệu của Garage. Sao lưu định kỳ là hạng mục của hướng phát triển.

## 4.6 Bảo mật và độ tin cậy

Bảng: Biện pháp bảo mật theo OWASP Top 10 (2021)

| Rủi ro | Biện pháp | Bằng chứng |
|---|---|---|
| A01 Kiểm soát truy cập | Quy tắc vai trò theo URL; kiểm tra chủ hồ sơ và CCTP được giao ở service | `matrixOfRolesIsEnforced`, `otherOwnersAndUnassignedOfficersAreRefused` |
| A02 Mật mã | BCrypt; token chỉ lưu SHA-256; HTTPS; cookie `HttpOnly`, `Secure`, `SameSite=Strict` | `registerVerifyThenLogin`, `refreshTokenRotates` |
| A03 Injection | JPA tham số hoá; React tự thoát HTML; email thoát HTML | Đọc mã |
| A04 Thiết kế không an toàn | Máy trạng thái phía máy chủ; khoá lạc quan; tra cứu chỉ 4 trường | `DossierLifecycleTest`, `publicLookupRevealsNoPersonalData` |
| A05 Cấu hình sai | CSP không `unsafe-inline`, HSTS, COOP, CORP, COEP; backend và kho tệp không mở cổng ra ngoài | ZAP baseline |
| A07 Xác thực | Khoá sau 5 lần sai; thông báo lỗi chung; vẫn chạy BCrypt khi email không tồn tại; token dùng một lần | `fiveWrongPasswordsLockForFifteenMinutes`, `passwordResetLinkWorksOnce` |
| A08 Toàn vẹn | Nhật ký chỉ thêm, chặn ở tầng CSDL; CI bắt buộc trước khi merge | `databaseRefusesLogChanges` |
| A09 Ghi nhật ký | Nhật ký nghiệp vụ và nhật ký quản trị | `dossierGoesFromDraftToReturned` |

**Độ tin cậy.**

- Chuyển trạng thái, dòng nhật ký và email cần gửi nằm trong một giao dịch. Nếu ghi nhật ký lỗi, trạng thái cũng quay lại (`statusRollsBackWhenLogFails`).
- Khi tiếp nhận và rút hồ sơ xảy ra đồng thời, đúng một thao tác thành công và thao tác còn lại nhận 409 (`acceptanceAndWithdrawalRacingLetExactlyOneWin`).
- Lỗi SMTP không ảnh hưởng hồ sơ; email được thử lại 3 lần rồi đánh dấu thất bại.
- Đồng hồ được tiêm vào mọi nơi dùng thời gian, nên các ca theo giờ như 15:00, thứ Sáu, ngày nghỉ được kiểm thử chính xác.

## 4.7 Kết quả kiểm thử và phân tích

Kiểm thử theo Test Plan v1.0 và báo cáo theo ISO/IEC/IEEE 29119-3. Số liệu lấy từ 3 nguồn, ghi rõ ở cột "Nguồn":

- **(1)** Chạy lại `./scripts/check.sh` trên `main`, commit `1c49d1d`, ngày 09/11/2026, exit code 0.
- **(2)** Báo cáo kiểm thử v1.0, CI commit `591470a`.
- **(3)** Nhánh `evidence`, lần chạy E2E cho commit `366b2e5`. Commit này không có trong lịch sử `main` hiện tại.

Bảng: Kết quả kiểm thử tổng hợp

| Hạng mục | Kết quả | Nguồn |
|---|---|---|
| Test backend (JUnit 5, PostgreSQL 17 qua Testcontainers) | 79 test, 0 lỗi | (1) |
| Độ phủ dòng tầng service (ngưỡng 70%) | 93,4% (1058/1133 dòng) | (1) |
| Test frontend (Vitest) | 5 test, 0 lỗi | (1) |
| Truy vết | 47/47 yêu cầu có test tham chiếu | (1) |
| Checkstyle, ESLint, Prettier | Không lỗi | (1) |
| Đầu cuối (Playwright trên Docker Compose) | 9/9 đạt trên x86 và arm64 | (2) |
| Hiệu năng k6: 50 người dùng ảo, 90 giây, chỉ đọc | p95 = 8,53 ms (API); 7493 yêu cầu; lỗi 0% | (3) |
| Tải lên tệp 10 MB | 161 ms, không giới hạn băng thông | (3) |
| RAM toàn hệ thống | Rỗi khoảng 391 MiB; sau luồng kiểm thử khoảng 443 MiB | (3) |
| URL ký sau khi hết hạn | Trả 400 | (3) |
| Khởi động lại | 5 hồ sơ trước, 5 hồ sơ sau | (3) |
| OWASP ZAP baseline | High 0, Medium 0, Low 0; 4 mục Informational | (3) |
| `npm audit` | 0 lỗ hổng | (3) |
| axe (WCAG 2 A, AA) | 0 vi phạm trên 4 màn hình | (3) |
| Trình duyệt (Playwright) | chromium, firefox, webkit đạt | (3) |

Bảng: Năm tiêu chí nghiệm thu MVP

| # | Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | Mỗi thủ tục đi trọn từ Nháp đến Đã trả kết quả | Đạt | `flow.spec.ts` (3 thủ tục qua API và kho tệp thật); `DossierFlowApiTest` |
| 2 | Ngày hẹn trả đúng mọi ca BR-03 | Đạt | `DeadlineCalculatorTest`: 17 ca CK-H01 → CK-H17 (trước, sau 15:00; thứ Sáu; ngày nghỉ; ngày làm bù) |
| 3 | Dashboard đúng số đúng hạn, trễ hạn | Đạt | `dashboardCountsOnTimeAndLateResults`; ca biên `resultAtTheDueTimeIsOnTimeAndOneSecondLaterIsLate` |
| 4 | Mọi yêu cầu mức M có ít nhất một ca đạt | Đạt | RTM: 47/47 |
| 5 | Không còn lỗi Critical | Đạt | Không có lỗi Critical mở; ZAP High 0 |

**Lỗi phát hiện và đã sửa.** Kiểm thử phát hiện 7 lỗi, gồm 4 lỗi Major và 3 lỗi Minor; không có lỗi Critical. Các lỗi Major:

- Hai ràng buộc `CHECK` không chặn được `NULL`.
- Caddy trả 405 ở cổng 9443 nên URL ký không tải được.
- Tiếp nhận và rút đồng thời trả 500 thay vì 409.
- Hạn JWT so với giờ hệ thống thay vì đồng hồ của ứng dụng, khiến bộ test đỏ tuỳ theo ngày chạy.

Cả 7 lỗi đã được sửa.

**Phân tích.**

- Chức năng và quy tắc nghiệp vụ được kiểm chứng tốt: mọi yêu cầu có test, độ phủ vượt ngưỡng hơn 20 điểm phần trăm, và các vùng dễ vỡ (ngày hẹn trả, tra cứu, nhật ký) đều có test riêng.
- Kết quả hiệu năng cho thấy còn dư rất nhiều so với ngưỡng 2 giây, nhưng **chưa khẳng định được NFR-01**. Phép đo chạy trên runner GitHub, không phải máy 2 vCPU, 4 GB; dữ liệu nhỏ; chỉ có thao tác đọc; k6 chạy cùng máy. Với tải tệp, tính theo lý thuyết thì 10 MB ở 20 Mbps mất khoảng 4 giây, dưới ngưỡng 10 giây.
- Bảo mật mới dừng ở quét thụ động và đối chiếu thiết kế. Đây chưa phải kiểm thử xâm nhập, và thư viện Java chưa được quét bằng công cụ.

Bảng: Các ca chưa kiểm hoặc mới kiểm một phần

| Ca | Nội dung | Tình trạng |
|---|---|---|
| CK-U02 | 3 người dùng thật nộp hồ sơ trong 5 phút (NFR-02) | Chưa kiểm, cần người thật |
| CK-P01, CK-P02 | Hiệu năng trên đúng cấu hình, giới hạn 20 Mbps | Một phần |
| CK-X01 | Safari, Edge thật | Một phần: mới kiểm bộ máy trình duyệt của Playwright |
| CK-A01 | Điều hướng bằng bàn phím | Một phần: axe đạt, chưa kiểm tay |
| CK-Q01 | Ma trận quyền đủ mọi endpoint | Một phần: 16 endpoint × 5 vai, đại diện từng nhóm quy tắc |
| CK-C11 | Nhánh "quản trị viên cuối cùng" | Một phần: chưa có test riêng |
| CK-C01 | `docker compose up` trên máy thành viên | Đạt trên runner GitHub; chưa chạy trên máy thành viên |

## 4.8 Kịch bản demo

Thời lượng dự kiến 10 phút. Mở sẵn 3 cửa sổ trình duyệt: người dân (giả lập điện thoại 360 px), cán bộ, và Mailpit (`http://localhost:8025`). Mật khẩu chung của các tài khoản demo là `Demo12345`.

Bảng: Kịch bản demo

| Bước | Tài khoản | Thao tác | Điều cần chỉ ra |
|---|---|---|---|
| 1 | Khách | Mở danh mục thủ tục, xem thành phần và thời hạn chứng thực bản sao | FR-06 |
| 2 | `dan@ehoso.local` | Tạo hồ sơ chứng thực, điền biểu mẫu, đính kèm PDF, nộp | Mã tham chiếu; email "Đã nộp" trong Mailpit |
| 3 | `motcua@ehoso.local` | Mở hàng đợi, yêu cầu bổ sung một thành phần kèm hướng dẫn | Trạng thái Chờ bổ sung; email liệt kê thiếu sót |
| 4 | `dan@ehoso.local` | Thay tệp, nộp lại | Mã tham chiếu giữ nguyên |
| 5 | `motcua@ehoso.local` | Đánh dấu đã đối chiếu bản chính, tiếp nhận, in phiếu hẹn | Mã HS-…; ngày hẹn trả theo mốc 15:00 |
| 6 | `motcua@ehoso.local` | Phân công cho `tuphap@ehoso.local` | Danh sách CCTP kèm số hồ sơ đang xử lý |
| 7 | `tuphap@ehoso.local` | Ghi ý kiến, tải dự thảo PDF, trình | Không trình được khi thiếu dự thảo |
| 8 | `lanhdao@ehoso.local` | Nhập sai mật khẩu một lần, rồi phê duyệt | Thông báo lỗi; trạng thái Đã có kết quả |
| 9 | `motcua@ehoso.local` | Trả kết quả bản điện tử | Người dân tải được kết quả qua URL ký |
| 10 | Khách | Tra cứu bằng mã và 4 số cuối `1234` | Chỉ 4 trường, không có tên hay số định danh |
| 11 | `lanhdao@ehoso.local` | Mở dashboard, xuất CSV | Số đúng hạn, trễ hạn |
| 12 | Trình chiếu | Mở nhật ký hồ sơ; chạy `UPDATE processing_log …` trong CSDL | CSDL từ chối: nhật ký chỉ được thêm |

**Phương án dự phòng.** Nếu máy demo gặp sự cố, trình chiếu video `eHoSo_demo.webm` do E2E ghi lại trên nhánh `evidence`.
