# Chương 3. Mô hình hoá hệ thống (System Modeling)

Mọi sơ đồ trong chương được vẽ bằng PlantUML. Mã nguồn `.puml` và ảnh nằm ở `docs/design/uml/`. Sơ đồ 01 → 13 được lập ở mốc M2. Sơ đồ 14 → 26 được bổ sung cho báo cáo này để phủ đủ những mục đề bài yêu cầu vẽ toàn bộ. Các sơ đồ mới được đối chiếu với mã nguồn tại commit `1c49d1d`.

## 3.1 Mô hình quy trình (DFD)

### Sơ đồ ngữ cảnh

Sơ đồ ngữ cảnh coi e-HoSo là một tiến trình duy nhất, trao đổi dữ liệu với 3 thực thể ngoài.

![Sơ đồ ngữ cảnh (DFD mức 0)](docs/design/uml/02_ngu_canh.png){w=14}

### DFD mức 1

DFD mức 1 phân rã hệ thống thành 8 tiến trình và 7 kho dữ liệu. Sơ đồ giữ đúng 3 thực thể ngoài của sơ đồ ngữ cảnh để bảo đảm cân bằng giữa hai mức. Ký hiệu: hình chữ nhật là thực thể ngoài, hình elip là tiến trình, hình trụ là kho dữ liệu.

![DFD mức 1 của e-HoSo](docs/design/uml/14_dfd_muc1.png)

Bảng: Thực thể ngoài

| Mã | Thực thể | Dữ liệu vào hệ thống | Dữ liệu ra khỏi hệ thống |
|---|---|---|---|
| E1 | Khách, Người dân | Thông tin đăng ký, đăng nhập; biểu mẫu, tệp; mã tra cứu | Phiên đăng nhập, danh mục thủ tục, mã tham chiếu, trạng thái, kết quả công khai 4 trường |
| E2 | CBMC, CCTP, Lãnh đạo, Quản trị viên | Quyết định xử lý, ý kiến, dự thảo, phê duyệt; tài khoản cán bộ; cấu hình | Hàng đợi, chi tiết hồ sơ, nhật ký, nhãn hạn, dashboard, CSV |
| E3 | Máy chủ SMTP | Không có | Email thông báo |

Bảng: Tiến trình của DFD mức 1

| Mã | Tiến trình | Mô tả | Yêu cầu |
|---|---|---|---|
| 1.0 | Quản lý tài khoản và xác thực | Đăng ký, xác thực email, đăng nhập, khoá tạm thời, đặt lại mật khẩu, quản lý tài khoản cán bộ, kiểm tra quyền | FR-01 → FR-05 |
| 2.0 | Cung cấp danh mục, cấu hình thủ tục | Trả danh mục cho khách; cho quản trị viên sửa thời hạn, thành phần, ngày nghỉ | FR-06, FR-27, FR-28 |
| 3.0 | Nộp và theo dõi hồ sơ của người dân | Lưu nháp, đính kèm tệp, nộp, bổ sung, rút; danh sách và lịch sử của tôi | FR-07 → FR-11, FR-13 |
| 4.0 | Xử lý hồ sơ | Tiếp nhận, bổ sung, từ chối, cấp mã, tính hạn, phân công, thẩm định, phê duyệt, trả kết quả | FR-15 → FR-22 |
| 5.0 | Tra cứu công khai | Đối chiếu mã và 4 số cuối định danh, trả 4 trường; chặn dò | FR-12 |
| 6.0 | Ghi nhật ký | Ghi một dòng cho mỗi sự kiện chuyển trạng thái; xếp thông báo theo sự kiện | FR-23 |
| 7.0 | Gửi thông báo | Lấy email đến hạn, gửi SMTP, thử lại, đánh dấu kết quả | FR-14 |
| 8.0 | Theo dõi hạn, thống kê | Tính nhãn hạn, tỷ lệ đúng hạn, trễ hạn; xuất CSV | FR-24 → FR-26 |

Bảng: Kho dữ liệu

| Mã | Kho dữ liệu | Bảng hoặc nơi lưu | Tiến trình ghi | Tiến trình đọc |
|---|---|---|---|---|
| D1 | Tài khoản | `app_user` | 1.0; 4.0 (đếm sai mật khẩu phê duyệt) | 1.0, 4.0 |
| D2 | Mã xác thực | `auth_token` | 1.0 | 1.0 |
| D3 | Danh mục, lịch | `procedure`, `dossier_component`, `holiday` | 2.0 | 2.0, 3.0, 4.0 |
| D4 | Hồ sơ, yêu cầu bổ sung | `dossier`, `supplement_request`, `supplement_item` | 3.0, 4.0 | 3.0, 4.0, 5.0, 8.0 |
| D5 | Tệp | `attachment` và đối tượng trong kho S3 | 3.0, 4.0 | 3.0, 4.0 |
| D6 | Nhật ký | `processing_log`, `admin_audit_log` | 6.0 | 4.0 (cán bộ xem nhật ký hồ sơ) |
| D7 | Hàng đợi thông báo | `notification` | 1.0, 6.0, 7.0 | 7.0 |

Bảng: Luồng dữ liệu chính

| Luồng | Từ → Đến | Nội dung | Ghi chú |
|---|---|---|---|
| Biểu mẫu, tệp, nộp | E1 → 3.0 | Dữ liệu biểu mẫu JSON, tệp PDF, JPG, PNG | Kiểm loại theo nội dung, giới hạn 10 MB và 30 MB |
| Mã tham chiếu, trạng thái | 3.0 → E1 | `TC-xxxxxxxx`, trạng thái, mốc thời gian | |
| Quyết định xử lý | E2 → 4.0 | Tiếp nhận, thiếu sót, lý do, CCTP được chọn, ý kiến, mật khẩu phê duyệt | Kèm `version` để chống ghi đè |
| Thời hạn, ngày nghỉ | D3 → 4.0 | Đơn vị thời hạn, mốc 15 giờ, lịch nghỉ, ngày làm bù | Dùng để tính ngày hẹn trả |
| Sự kiện chuyển trạng thái | 3.0, 4.0 → 6.0 | Hồ sơ, hành động, trạng thái trước và sau, người làm, ghi chú | Cùng giao dịch với thay đổi D4 |
| 4 trường công khai | 5.0 → E1 | Tên thủ tục, trạng thái, các mốc thời gian, ngày hẹn trả | Không có dữ liệu cá nhân |
| Email | 7.0 → E3 | Tiêu đề, nội dung HTML và văn bản | Ngoài giao dịch nghiệp vụ |

### DFD mức 2: tiến trình 4.0 Xử lý hồ sơ

Tiến trình 4.0 được phân rã vì đây là nơi tập trung các quy tắc nghiệp vụ dễ sai: mốc 15 giờ, ngày làm việc, gia hạn, phê duyệt. Thực thể E2 được tách thành 3 vai để thấy rõ ai cung cấp dữ liệu nào.

![DFD mức 2: phân rã tiến trình 4.0 Xử lý hồ sơ](docs/design/uml/15_dfd_muc2_xu_ly.png)

Bảng: Tiến trình con của 4.0

| Mã | Tiến trình con | Đầu vào | Đầu ra | Quy tắc |
|---|---|---|---|---|
| 4.1 | Kiểm tra, tiếp nhận, yêu cầu bổ sung, từ chối | Hàng đợi SUBMITTED; quyết định của CBMC | NEED_SUPPLEMENT kèm phiếu bổ sung, REJECTED kèm lý do, hoặc chuyển sang 4.2 | BR-09; thủ tục chứng thực bắt buộc đối chiếu bản chính |
| 4.2 | Cấp mã hồ sơ, tính ngày hẹn trả | Hồ sơ được tiếp nhận; D3; bộ đếm D8 | ACCEPTED, mã HS, hạn trả, phiếu hẹn | BR-01, BR-02, BR-03 |
| 4.3 | Phân công CCTP | CCTP được chọn; danh sách CCTP hoạt động; tải việc | IN_PROGRESS, người được giao | FR-18 |
| 4.4 | Thẩm định, xác minh, trình | Ý kiến, dự thảo PDF, nơi xác minh | Hạn mới (+3 ngày làm việc), PENDING_APPROVAL | BR-05 |
| 4.5 | Phê duyệt hoặc trả lại | Đồng ý kèm mật khẩu, hoặc lý do trả lại | APPROVED kèm thời điểm có kết quả, dự thảo thành kết quả; hoặc IN_PROGRESS | BR-07, BR-08; sai mật khẩu 3 lần khoá 15 phút |
| 4.6 | Trả kết quả | Hình thức trả | RETURNED, thời điểm trả | FR-22 |

Mức 2 cân bằng với mức 1: mọi luồng vào, ra của 4.0 ở mức 1 (với E2, D1, D3, D4, D5, D6 và 6.0) đều xuất hiện ở ít nhất một tiến trình con. Kho D8 (bộ đếm mã hồ sơ, bảng `dossier_code_counter`) chỉ xuất hiện ở mức 2 vì nó là chi tiết bên trong của 4.2.

### Thay đổi so với mốc M2

- M2 chỉ có sơ đồ ngữ cảnh (02) và một sơ đồ activity luồng chính (12). DFD mức 1 và mức 2 là sơ đồ mới, lập từ mã nguồn đã hoàn thành nên phản ánh đúng các kho dữ liệu thật.
- Kho tệp dùng Garage thay MinIO (ADR-001). Bản Garage v2.5.0 tự tạo khoá và bucket nên bỏ được dịch vụ `storage-init` dự kiến ở M2 (điểm lệch L-01).
- Kho D7 (hàng đợi thông báo) được tách thành kho riêng: email được xếp hàng trong giao dịch nghiệp vụ và gửi sau, đúng thiết kế TK-04.

## 3.2 Mô hình hoạt động (Activity)

Ba sơ đồ activity dạng swimlane, gộp lại phủ đủ 17 use case. Làn "Hệ thống" tách riêng để thấy rõ bước nào do máy chủ tự làm. Sơ đồ activity luồng chính của M2 (12) được thay bằng ba sơ đồ này.

Bảng: Phạm vi của 3 sơ đồ activity

| Sơ đồ | Làn | Use case | Điểm chính |
|---|---|---|---|
| Người dân | Khách, Người dân; Hệ thống; Dịch vụ email | UC-01 → UC-07 | Hai lối vào: tra cứu không cần tài khoản, hoặc đăng ký rồi nộp; vòng theo dõi bổ sung, rút |
| Cán bộ xử lý | CBMC; Hệ thống; CCTP; Lãnh đạo | UC-08 → UC-13 | Ba nhánh tại tiếp nhận; vòng trình và trả lại; xác minh gia hạn một lần |
| Lãnh đạo và quản trị | Lãnh đạo; Quản trị viên; Hệ thống; Dịch vụ email | UC-14 → UC-17 | Ba luồng độc lập: dashboard, quản trị, vòng gửi email |

![Activity – Khách và Người dân (UC-01 → UC-07)](docs/design/uml/16_activity_nguoi_dan.png)

Ở sơ đồ người dân, mọi kiểm tra đều nằm ở làn Hệ thống: loại tệp đọc theo nội dung, đủ thành phần bắt buộc, khoá sau 5 lần sai mật khẩu. Tra cứu trả cùng một thông báo cho mọi trường hợp sai để không lộ việc mã có tồn tại hay không.

![Activity – Cán bộ xử lý hồ sơ (UC-08 → UC-13)](docs/design/uml/17_activity_can_bo.png)

Sơ đồ cán bộ cho thấy hồ sơ chỉ đi tiếp khi đúng người, đúng trạng thái. Vòng lặp giữa CCTP và lãnh đạo kết thúc khi hồ sơ được phê duyệt. Thời điểm phê duyệt là mốc xét đúng hạn (BR-07), không phải thời điểm trả kết quả.

![Activity – Lãnh đạo, Quản trị viên và Hệ thống (UC-14 → UC-17)](docs/design/uml/18_activity_lanh_dao_quan_tri.png)

## 3.3 Mô hình tương tác (Sequence)

Bốn sơ đồ tuần tự được vẽ cho 4 use case chính đã đặc tả ở mục 2.5. Các sơ đồ thể hiện thứ tự gọi giữa controller, service, `DossierLifecycle`, CSDL, kho tệp và hàng đợi email. Điểm chung: mọi thay đổi trạng thái, dòng nhật ký và email cần gửi đều nằm trong một giao dịch. Việc gửi email thật diễn ra sau, ở bộ lập lịch.

![Sequence UC-04 Nộp hồ sơ trực tuyến](docs/design/uml/07_seq_uc04.png)

![Sequence UC-08 Tiếp nhận hồ sơ](docs/design/uml/08_seq_uc08.png)

![Sequence UC-10 Thẩm định và trình phê duyệt](docs/design/uml/09_seq_uc10.png)

![Sequence UC-11 Phê duyệt kết quả](docs/design/uml/10_seq_uc11.png)

## 3.4 Mô hình trạng thái (State machine)

Sáu đối tượng có vòng đời được mô hình hoá. Hồ sơ là đối tượng quan trọng nhất nên được trình bày chi tiết. Năm đối tượng còn lại được trình bày gọn.

### Hồ sơ

Hồ sơ có 10 trạng thái và 12 hành động. Chỉ 13 cặp (trạng thái, hành động) hợp lệ được khai báo trong bảng `ALLOWED` của lớp `DossierLifecycle`, là nơi duy nhất đổi trạng thái. Mọi cặp khác trả lỗi 409 kèm trạng thái hiện tại.

![State – Vòng đời hồ sơ](docs/design/uml/11_state_vong_doi.png)

Bảng: 13 chuyển trạng thái hợp lệ của hồ sơ

| # | Trạng thái nguồn | Hành động | Trạng thái đích | Tác nhân | Điều kiện, hiệu ứng | Email |
|---|---|---|---|---|---|---|
| 1 | DRAFT | SUBMIT | SUBMITTED | Người dân | Đủ thành phần, trường bắt buộc; cấp mã tham chiếu | Đã nộp |
| 2 | SUBMITTED | REQUEST_SUPPLEMENT | NEED_SUPPLEMENT | CBMC | Ít nhất 1 thành phần thiếu, có hướng dẫn; tăng số lần bổ sung | Cần bổ sung |
| 3 | NEED_SUPPLEMENT | RESUBMIT | SUBMITTED | Người dân | Đủ thành phần; giữ mã tham chiếu | Đã nộp |
| 4 | SUBMITTED | WITHDRAW | WITHDRAWN | Người dân | BR-04 | Không |
| 5 | NEED_SUPPLEMENT | WITHDRAW | WITHDRAWN | Người dân | BR-04 | Không |
| 6 | SUBMITTED | REJECT | REJECTED | CBMC | Có lý do | Từ chối |
| 7 | SUBMITTED | ACCEPT | ACCEPTED | CBMC | Chứng thực: đã đối chiếu bản chính; cấp mã HS, tính hạn | Đã tiếp nhận |
| 8 | ACCEPTED | ASSIGN | IN_PROGRESS | CBMC | CCTP đang hoạt động | Không |
| 9 | IN_PROGRESS | MARK_VERIFICATION | IN_PROGRESS | CCTP được giao | Thủ tục cho phép, chưa gia hạn lần nào; hạn + 3 ngày làm việc | Gia hạn |
| 10 | IN_PROGRESS | SUBMIT_APPROVAL | PENDING_APPROVAL | CCTP được giao | Có ý kiến và dự thảo PDF | Không |
| 11 | PENDING_APPROVAL | SEND_BACK | IN_PROGRESS | Lãnh đạo | Có lý do | Báo CCTP |
| 12 | PENDING_APPROVAL | APPROVE | APPROVED | Lãnh đạo | Đúng mật khẩu; ghi thời điểm có kết quả; ngày hết giá trị (BR-08) | Có kết quả |
| 13 | APPROVED | RETURN_RESULT | RETURNED | CBMC | Hình thức trả: tại quầy hoặc điện tử | Đã trả |

Ba trạng thái kết thúc là REJECTED, WITHDRAWN và RETURNED. Theo QD-07, sơ đồ không có chuyển "Xoá nháp" vì không có yêu cầu chức năng tương ứng. Hai cán bộ thao tác cùng lúc trên một hồ sơ được xử lý bằng khoá lạc quan: người thứ hai nhận 409 và phải tải lại.

### Năm đối tượng còn lại

![State – Tài khoản người dùng](docs/design/uml/19_state_tai_khoan.png){w=15}

![State – Mã xác thực (VERIFY, RESET, REFRESH)](docs/design/uml/20_state_ma_xac_thuc.png){w=11}

![State – Tệp đính kèm](docs/design/uml/21_state_tep_dinh_kem.png){w=15}

[[ROW]]
![State – Yêu cầu bổ sung](docs/design/uml/22_state_yeu_cau_bo_sung.png)
![State – Thông báo email](docs/design/uml/23_state_thong_bao.png)
[[/ROW]]

Bảng: Tóm tắt trạng thái của 5 đối tượng còn lại

| Đối tượng | Trạng thái | Lưu ở | Điểm đáng chú ý |
|---|---|---|---|
| Tài khoản | PENDING, ACTIVE (bình thường, tạm khoá đăng nhập), LOCKED | Cột `status`, `locked_until` | Khoá tạm 15 phút áp dụng cả khi PENDING; cán bộ được tạo ở ACTIVE; khoá tài khoản thu hồi refresh token; không có trạng thái xoá vì nhật ký tham chiếu tới người dùng |
| Mã xác thực | Còn hiệu lực, Đã dùng hoặc thu hồi, Hết hạn | Suy ra từ `used_at`, `expires_at` | Chỉ lưu SHA-256; mỗi mã dùng một lần; hạn 24 giờ, 30 phút, 7 ngày theo mục đích |
| Tệp đính kèm | Đang kiểm tra, Đã ghi kho, DOSSIER, DRAFT, RESULT | Cột `kind` và kho S3 | Ghi CSDL lỗi thì xoá đối tượng đã ghi (bù trừ); dự thảo thành kết quả khi phê duyệt |
| Yêu cầu bổ sung | Đang chờ, Đã đáp ứng, Đóng do rút | Suy ra từ trạng thái hồ sơ | Phiếu không sửa sau khi lập (BR-09); lần sau tạo phiếu mới |
| Thông báo | PENDING, SENT, FAILED | Cột `status`, `attempts` | Lỗi SMTP lần thứ 4 thì FAILED; hẹn lại sau số phút bằng số lần thử |

## 3.5 Mô hình lớp (Class)

### Sơ đồ lớp miền

Sơ đồ lớp miền (M2) thể hiện các thực thể nghiệp vụ, thuộc tính và quan hệ. Mỗi lớp miền tương ứng với một yêu cầu dữ liệu DR-xx.

![Sơ đồ lớp miền](docs/design/uml/05_class_mien.png)

### Sơ đồ lớp thiết kế

Sơ đồ lớp thiết kế được lập từ mã nguồn và vẽ các phụ thuộc chính. Chiều mũi tên là chiều điều hướng, lấy từ tham số hàm dựng của từng lớp (Spring tiêm phụ thuộc qua hàm dựng). Do sơ đồ đầy đủ quá rộng để đọc trên một trang, nó được tách làm hai phần:

- **(a)** 7 controller, các service và quan hệ giữa các service.
- **(b)** các service và repository mà chúng dùng.

![Lớp thiết kế (a): 7 controller và tầng nghiệp vụ](docs/design/uml/24_class_thiet_ke.png)

Giao diện `AttachmentGuard` được cài đặt bởi `DossierAttachmentGuard` trong gói `dossier`. Nhờ vậy module `file` không phụ thuộc module `dossier` và tránh được phụ thuộc vòng (TK-10). `S3FileStorage` là cài đặt duy nhất của cổng `FileStoragePort`.

![Lớp thiết kế (b): service và repository](docs/design/uml/24b_class_thiet_ke_du_lieu.png){w=12}

Bảng: Trách nhiệm của 7 controller

| Controller | Đường dẫn | Service được gọi | Vai trò được phép |
|---|---|---|---|
| `AuthController` | `/api/v1/auth` | `AuthService` | Khách, mọi người dùng |
| `AdminUserController` | `/api/v1/admin/users` | `StaffUserService` | Quản trị viên |
| `CatalogController` | `/api/v1/procedures`, `/api/v1/admin/procedures`, `/api/v1/admin/holidays` | `CatalogService` | Khách (đọc); Quản trị viên (sửa) |
| `DossierController` | `/api/v1/dossiers`, `/api/v1/public/lookup` | `DossierService` | Người dân; Khách (tra cứu) |
| `AttachmentController` | `/api/v1/dossiers/{id}/attachments`, `/api/v1/attachments` | `AttachmentService` | Người dân, CCTP (tải lên); người có quyền xem (tải xuống) |
| `WorkflowController` | `/api/v1/staff` | `IntakeService`, `ReviewService`, `ApprovalService`, `StaffQueryService` | CBMC, CCTP, Lãnh đạo theo từng hành động |
| `DashboardController` | `/api/v1/dashboard` | `DashboardService` | Lãnh đạo |

## 3.6 Mô hình dữ liệu (ERD)

![Sơ đồ thực thể – liên kết (ERD)](docs/design/uml/13_erd.png)

Bảng: Thực thể và quan hệ chính

| Thực thể | Quan hệ | Bản số | Ý nghĩa |
|---|---|---|---|
| Thủ tục – Thành phần hồ sơ | `procedure` có nhiều `dossier_component` | 1 – n | Mỗi thủ tục có danh sách thành phần bắt buộc hoặc có điều kiện |
| Người dùng – Hồ sơ (người nộp) | `app_user` nộp nhiều `dossier` | 1 – n | Chỉ người dân là người nộp |
| Người dùng – Hồ sơ (CCTP được giao) | `app_user` được giao nhiều `dossier` | 1 – n, tuỳ chọn | Có giá trị từ IN_PROGRESS trở đi |
| Thủ tục – Hồ sơ | `procedure` áp dụng cho nhiều `dossier` | 1 – n | Thời hạn đọc tại thời điểm tiếp nhận |
| Hồ sơ – Tệp đính kèm | `dossier` có nhiều `attachment` | 1 – n | Tệp loại DOSSIER gắn với một thành phần |
| Hồ sơ – Yêu cầu bổ sung – Mục thiếu | `dossier` có nhiều `supplement_request`, mỗi phiếu có nhiều `supplement_item` | 1 – n – n | Mỗi mục trỏ tới một thành phần hồ sơ |
| Hồ sơ – Nhật ký | `dossier` có nhiều `processing_log` | 1 – n | Chỉ thêm |
| Hồ sơ – Thông báo | `dossier` có nhiều `notification` | 1 – n, tuỳ chọn | Email tài khoản không gắn hồ sơ |
| Người dùng – Mã xác thực | `app_user` có nhiều `auth_token` | 1 – n | Mỗi mã có một mục đích |

**Thay đổi so với M2.** Lược đồ trong mã (`src/backend/src/main/resources/db/migration/V1__init.sql`) trùng hoàn toàn với bản thiết kế M2 (`docs/design/db/V1__init.sql`), không có thay đổi về bảng, cột hay ràng buộc. Dữ liệu giả lập nằm ở một migration riêng, `V2__seed.sql` (3 thủ tục, thành phần và ngày nghỉ).

## 3.7 Lược đồ dữ liệu (Schema)

Lược đồ gồm 13 bảng trên PostgreSQL 17. Quy ước chung: khoá chính UUID (trừ hai bảng nhật ký dùng số tự tăng), thời điểm là `timestamptz`, biểu mẫu là `jsonb`. Ba bảng quan trọng nhất được trình bày đầy đủ; 10 bảng còn lại trình bày gọn.

Bảng: Bảng `dossier` (hồ sơ)

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | uuid | PK |
| `reference_code` | char(11) | UNIQUE; `^TC-[A-Z0-9]{8}$` |
| `dossier_code` | char(14) | UNIQUE; `^HS-[0-9]{6}-[0-9]{4}$` |
| `procedure_id` | uuid | NOT NULL, FK → `procedure` |
| `applicant_id` | uuid | NOT NULL, FK → `app_user` |
| `assigned_officer_id` | uuid | FK → `app_user` |
| `form_data` | jsonb | NOT NULL, mặc định `{}` |
| `status` | varchar(20) | NOT NULL, mặc định DRAFT; CHECK thuộc 10 trạng thái |
| `created_at`, `submitted_at`, `accepted_at`, `due_at`, `result_at`, `returned_at` | timestamptz | `created_at` NOT NULL |
| `original_checked`, `verification_flag` | boolean | NOT NULL, mặc định false |
| `verification_place` | varchar(300) | Bắt buộc khi `verification_flag` |
| `supplement_count` | int | NOT NULL, ≥ 0 |
| `review_opinion` | text | |
| `result_kind` | varchar(8) | ISSUED hoặc REFUSED |
| `result_expires_on` | date | BR-08 |
| `return_method` | varchar(10) | COUNTER hoặc ELECTRONIC |
| `version` | bigint | NOT NULL; khoá lạc quan |

Bảng `dossier` có 6 ràng buộc `CHECK` liên cột, bảo đảm dữ liệu khớp vòng đời ngay ở tầng CSDL. Ví dụ: từ ACCEPTED trở đi phải có mã hồ sơ, thời điểm tiếp nhận và hạn trả; từ IN_PROGRESS trở đi phải có CCTP được giao; RETURNED phải có thời điểm và hình thức trả. Năm chỉ mục phục vụ hàng đợi, danh sách theo người nộp, theo CCTP, theo hạn (chỉ hồ sơ chưa có kết quả) và theo thời điểm có kết quả.

Bảng: Bảng `attachment` (tệp đính kèm)

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | uuid | PK |
| `dossier_id` | uuid | NOT NULL, FK → `dossier` |
| `component_id` | uuid | FK → `dossier_component`; bắt buộc khi `kind` = DOSSIER |
| `kind` | varchar(8) | DOSSIER, DRAFT hoặc RESULT |
| `file_name` | varchar(255) | NOT NULL |
| `mime_type` | varchar(30) | PDF, JPEG hoặc PNG; DRAFT và RESULT chỉ PDF |
| `size_bytes` | bigint | 0 < giá trị ≤ 10 485 760 |
| `storage_key` | varchar(120) | NOT NULL, UNIQUE |
| `uploaded_by` | uuid | NOT NULL, FK → `app_user` |
| `created_at` | timestamptz | NOT NULL |

Bảng: Bảng `processing_log` (nhật ký xử lý)

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | bigint | PK, tự tăng |
| `dossier_id` | uuid | NOT NULL, FK → `dossier` |
| `actor_id` | uuid | NOT NULL, FK → `app_user` |
| `actor_role` | varchar(10) | NOT NULL; lưu vai trò tại thời điểm thao tác |
| `action` | varchar(20) | NOT NULL |
| `from_status` | varchar(20) | Rỗng ở dòng đầu |
| `to_status` | varchar(20) | NOT NULL |
| `note` | text | |
| `occurred_at` | timestamptz | NOT NULL |

Bảng `processing_log` có trigger từ chối `UPDATE`, `DELETE` và `TRUNCATE`, nên nhật ký chỉ được thêm kể cả khi mã ứng dụng có lỗi (FR-23). Bảng `admin_audit_log` có cùng cơ chế.

Bảng: 10 bảng còn lại

| Bảng | Khoá | Cột chính | Ràng buộc đáng chú ý |
|---|---|---|---|
| `app_user` | PK `id` | họ tên, email, điện thoại, số định danh, mật khẩu băm, vai trò, trạng thái, đếm sai đăng nhập và phê duyệt | Email, số định danh UNIQUE; số định danh 12 số chỉ bắt buộc với người dân |
| `auth_token` | PK `id`; FK `user_id` | mục đích, `token_hash`, `expires_at`, `used_at` | `token_hash` UNIQUE; mục đích VERIFY, RESET, REFRESH |
| `procedure` | PK `id` | mã, tên, đơn vị thời hạn, số ngày, mốc 15 giờ, xác minh, đối chiếu bản chính, số tháng hiệu lực, `form_schema` | `deadline_days` chỉ có khi đơn vị là WORKING_DAYS |
| `dossier_component` | PK `id`; FK `procedure_id` | tên, REQUIRED hoặc CONDITIONAL, định dạng cho phép, thứ tự | UNIQUE (thủ tục, tên) |
| `supplement_request` | PK `id`; FK `dossier_id`, `requested_by` | hướng dẫn, thời điểm | Chỉ mục theo hồ sơ, mới nhất trước |
| `supplement_item` | PK `id`; FK `request_id`, `component_id` | ghi chú | UNIQUE (phiếu, thành phần) |
| `admin_audit_log` | PK tự tăng; FK `actor_id`, `target_user_id` | hành động, ghi chú, thời điểm | Chỉ thêm, chặn bằng trigger |
| `holiday` | PK `day` | loại HOLIDAY hoặc MAKEUP_WORKDAY, mô tả | |
| `notification` | PK `id`; FK `dossier_id` (tuỳ chọn) | người nhận, loại, payload, trạng thái, số lần thử, lần thử kế tiếp, lỗi cuối | Chỉ mục riêng cho email PENDING |
| `dossier_code_counter` | PK `code_date` | `last_seq` | 1 ≤ `last_seq` ≤ 9999 |
