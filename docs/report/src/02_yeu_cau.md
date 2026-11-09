# Chương 2. Yêu cầu hệ thống (System Requirements)

Chương này tóm tắt SRS v1.2 (ISO/IEC/IEEE 29148). Mỗi yêu cầu có mã duy nhất và được truy vết tới use case, mã nguồn và test. Mức ưu tiên dùng MoSCoW: **M** (bắt buộc cho MVP), **S** (nên có), **C** (có thể có).

## 2.1 Yêu cầu giao diện

**Giao diện người dùng.**

- SPA tiếng Việt chạy trên trình duyệt, gồm cổng người dân, phân hệ cán bộ và phân hệ quản trị.
- Cổng người dân hiển thị đúng từ độ rộng 360 px (NFR-02).
- Trạng thái và nhãn hạn thể hiện bằng cả chữ và màu (NFR-06).
- Mọi trường nhập liệu có nhãn; lỗi được báo cạnh từng trường.

**Giao diện phần mềm.**

Bảng: Các giao diện ngoài của hệ thống

| Giao diện | Giao thức, định dạng | Bên sử dụng | Ràng buộc chính |
|---|---|---|---|
| REST API | HTTPS, JSON, tiền tố `/api/v1`, mô tả bằng OpenAPI 3 (`/v3/api-docs`) | SPA | JWT ở tiêu đề `Authorization: Bearer`; lỗi trả theo một khuôn chung gồm mã lỗi, thông điệp, danh sách trường lỗi |
| SMTP | SMTP, thư HTML kèm bản văn bản thuần | Module `notification` → máy chủ thư (Mailpit trên bản demo) | Gửi ngoài giao dịch nghiệp vụ, qua hàng đợi, thử lại tối đa 3 lần |
| Kho tệp S3 | S3 API qua AWS SDK v2; URL ký cho tải xuống | Module `file` → Garage | Khoá lưu trữ `ho-so/<id hồ sơ>/<uuid>`, không chứa dữ liệu cá nhân; URL ký hết hạn sau 5 phút |
| Cơ sở dữ liệu | JDBC tới PostgreSQL 17 | Backend | Lược đồ quản lý bằng Flyway; mọi thời điểm là `timestamptz` |

**Giao diện truyền thông.** Bản demo chạy HTTPS ở cổng 443 cho SPA và API, cổng 9443 cho URL ký tới kho tệp. Chứng chỉ do Caddy tự cấp nên trình duyệt sẽ cảnh báo.

## 2.2 Yêu cầu chức năng

Hệ thống có 28 yêu cầu chức năng: 22 mức M, 4 mức S, 2 mức C. Toàn bộ 28 yêu cầu đã được xây dựng.

Bảng: 28 yêu cầu chức năng theo MoSCoW

| Mã | Yêu cầu | Mức | Sprint | Use case |
|---|---|---|---|---|
| FR-01 | Đăng ký tài khoản người dân; kích hoạt qua liên kết email hạn 24 giờ | M | S1 | UC-01 |
| FR-02 | Đăng nhập; sai 5 lần liên tiếp thì khoá 15 phút | M | S1 | UC-02 |
| FR-03 | Đặt lại mật khẩu qua liên kết hạn 30 phút, dùng một lần | S | S1 | UC-02 |
| FR-04 | Quản lý tài khoản cán bộ và vai trò | M | S1 | UC-15 |
| FR-05 | Kiểm tra quyền phía máy chủ theo ma trận SRS mục 3.7 | M | S1 | UC-15, mọi UC |
| FR-06 | Danh mục thủ tục: thành phần, thời hạn, giấy tờ xuất trình | M | S1 | UC-03 |
| FR-07 | Tạo hồ sơ bằng biểu mẫu điện tử riêng từng thủ tục, lưu nháp | M | S1 | UC-04 |
| FR-08 | Đính kèm PDF, JPG, PNG theo thành phần; 10 MB mỗi tệp, 30 MB mỗi hồ sơ; kiểm loại theo nội dung | M | S1 | UC-04, UC-05 |
| FR-09 | Nộp hồ sơ, kiểm tra đủ thành phần bắt buộc, cấp mã tham chiếu, gửi email | M | S1 | UC-04 |
| FR-10 | Rút hồ sơ ở trạng thái Đã nộp hoặc Chờ bổ sung | S | S1 | UC-06 |
| FR-11 | Bổ sung hồ sơ: xem yêu cầu, thay tệp, sửa biểu mẫu, nộp lại | M | S1 | UC-05 |
| FR-12 | Tra cứu công khai bằng mã và 4 số cuối định danh; không lộ thông tin cá nhân | M | S1 | UC-07 |
| FR-13 | Danh sách hồ sơ của người dân, lọc theo trạng thái, lịch sử trạng thái | M | S1 | UC-07 |
| FR-14 | Email theo trạng thái: nộp, cần bổ sung, từ chối, tiếp nhận, có kết quả, gia hạn | M | S1 | UC-17 |
| FR-15 | Hàng đợi hồ sơ Đã nộp của CBMC, cũ nhất trước | M | S2 | UC-08 |
| FR-16 | Tiếp nhận, yêu cầu bổ sung (liệt kê từng thiếu sót) hoặc từ chối (có lý do) | M | S2 | UC-08 |
| FR-17 | Cấp mã hồ sơ, tính ngày hẹn trả, in phiếu hẹn | M | S2 | UC-08 |
| FR-18 | Phân công CCTP, hiển thị số hồ sơ mỗi người đang xử lý | M | S2 | UC-09 |
| FR-19 | Thẩm định, ghi ý kiến, đính kèm dự thảo PDF, trình lãnh đạo | M | S2 | UC-10 |
| FR-20 | Đánh dấu cần xác minh, cộng 3 ngày làm việc một lần | S | S2 | UC-10 |
| FR-21 | Phê duyệt (ký mô phỏng bằng mật khẩu) hoặc trả lại có lý do | M | S2 | UC-11 |
| FR-22 | Trả kết quả tại quầy hoặc bản điện tử | M | S2 | UC-12 |
| FR-23 | Nhật ký xử lý chỉ được thêm | M | S1 | UC-04 → UC-12 |
| FR-24 | Nhãn hạn xử lý bằng chữ và màu | M | S2 | UC-13 |
| FR-25 | Dashboard lãnh đạo: theo trạng thái, đúng hạn, trễ hạn theo thủ tục và CCTP | M | S2 | UC-14 |
| FR-26 | Xuất dashboard ra CSV UTF-8 | S | S2 | UC-14 |
| FR-27 | Cấu hình thời hạn, mốc 15 giờ, thành phần của thủ tục | C | S2 | UC-16 |
| FR-28 | Quản lý lịch ngày nghỉ và ngày làm bù | C | S2 | UC-16 |

## 2.3 Yêu cầu phi chức năng

Bảng: 10 yêu cầu phi chức năng và cách đo

| Mã | Thuộc tính | Yêu cầu đo được | Mức | Kết quả (mục 4.7) |
|---|---|---|---|---|
| NFR-01 | Hiệu năng | 95% yêu cầu API dưới 2 giây với 50 người dùng đồng thời trên máy 2 vCPU, 4 GB; tải tệp 10 MB dưới 10 giây ở 20 Mbps | M | Một phần |
| NFR-02 | Khả dụng | Tiếng Việt, đúng từ 360 px; người dân mới nộp được hồ sơ chứng thực trong 5 phút (3 người dùng) | M | Một phần |
| NFR-03 | Bảo mật | BCrypt; mật khẩu tối thiểu 8 ký tự gồm chữ và số; JWT 30 phút có refresh token; HTTPS; không lỗ hổng Critical, High | M | Một phần: quét thụ động, thư viện Java chưa quét |
| NFR-04 | Riêng tư | Dữ liệu giả lập; tra cứu không lộ thông tin cá nhân; tải tệp qua URL 5 phút sau khi kiểm quyền | M | Đạt |
| NFR-05 | Tin cậy | Chuyển trạng thái và nhật ký trong cùng giao dịch; không mất hồ sơ khi khởi động lại | M | Đạt |
| NFR-06 | Tiếp cận | WCAG 2.1 AA: tương phản, nhãn trường, bàn phím; không chỉ dùng màu | S | Một phần |
| NFR-07 | Bảo trì | Phủ test tầng service ≥ 70%; OpenAPI 3; Checkstyle, ESLint không lỗi | M | Đạt |
| NFR-08 | Khả chuyển | Chạy bằng một lệnh `docker compose up` trên máy 8 GB RAM | M | Đạt trên runner GitHub; chưa chạy trên máy thành viên |
| NFR-09 | Tương thích | Chrome, Edge, Firefox, Safari, 2 phiên bản mới nhất | S | Một phần |
| NFR-10 | Thời gian | ISO 8601 có múi giờ; tính hạn theo Asia/Ho_Chi_Minh | M | Đạt |

## 2.4 Quy tắc nghiệp vụ và yêu cầu dữ liệu

Bảng: 9 quy tắc nghiệp vụ

| Mã | Quy tắc | Mức |
|---|---|---|
| BR-01 | Mã hồ sơ `HS-YYMMDD-NNNN`, số thứ tự bắt đầu lại từ 0001 mỗi ngày, duy nhất; mã tham chiếu `TC-` + 8 ký tự chữ, số ngẫu nhiên | M |
| BR-02 | Thời hạn mặc định: chứng thực và khai sinh trong ngày; xác nhận tình trạng hôn nhân 3 ngày làm việc; tính từ lúc tiếp nhận | M |
| BR-03 | Thủ tục trong ngày: tiếp nhận trước 15:00 của ngày làm việc thì hạn 17:00 cùng ngày; từ 15:00 hoặc vào ngày nghỉ thì hạn 17:00 ngày làm việc kế tiếp | M |
| BR-04 | Chỉ rút được hồ sơ Đã nộp hoặc Chờ bổ sung | S |
| BR-05 | Xác minh cộng 3 ngày làm việc vào ngày hẹn trả hiện tại, mỗi hồ sơ một lần | S |
| BR-06 | Quá hạn: qua ngày hẹn mà chưa có kết quả. Sắp đến hạn: còn dưới 2 giờ (thủ tục trong ngày) hoặc dưới 24 giờ (thủ tục theo ngày làm việc) | M |
| BR-07 | Đúng hạn khi thời điểm vào Đã có kết quả không muộn hơn ngày hẹn trả; ngược lại là trễ hạn | M |
| BR-08 | Giấy xác nhận tình trạng hôn nhân hết giá trị sau 6 tháng kể từ ngày phê duyệt | M |
| BR-09 | Yêu cầu bổ sung phải liệt kê đủ thiếu sót trong một lần; đếm số lần bổ sung | M |

Bảng: 8 yêu cầu dữ liệu

| Mã | Thực thể | Thuộc tính chính | Bảng |
|---|---|---|---|
| DR-01 | Người dùng | Họ tên, email (duy nhất), điện thoại, số định danh (người dân, duy nhất), mật khẩu băm, vai trò, trạng thái | `app_user` |
| DR-02 | Thủ tục | Mã, tên, đơn vị thời hạn, số ngày, áp dụng mốc 15 giờ, cho phép xác minh | `procedure` |
| DR-03 | Thành phần hồ sơ | Thủ tục, tên, bắt buộc hoặc có điều kiện, định dạng cho phép | `dossier_component` |
| DR-04 | Hồ sơ | Mã tham chiếu, mã hồ sơ, thủ tục, người nộp, biểu mẫu JSON, trạng thái, các mốc thời gian, CCTP được giao, cờ xác minh, số lần bổ sung | `dossier` |
| DR-05 | Tệp đính kèm | Hồ sơ, thành phần, tên tệp, MIME, dung lượng, khoá lưu trữ, loại (hồ sơ, dự thảo, kết quả) | `attachment` |
| DR-06 | Nhật ký xử lý | Hồ sơ, người thực hiện, vai trò, hành động, trạng thái trước và sau, ghi chú, thời điểm; chỉ thêm | `processing_log` |
| DR-07 | Ngày nghỉ | Ngày, loại (nghỉ lễ, làm bù), mô tả | `holiday` |
| DR-08 | Thông báo | Hồ sơ, người nhận, loại, trạng thái gửi, số lần thử, thời điểm | `notification` |

## 2.5 Use case

Hệ thống có 17 use case. Sơ đồ use case tổng quát (Hình dưới) thể hiện đủ 17 use case và 7 tác nhân.

![Sơ đồ use case tổng quát (17 use case)](docs/design/uml/01_usecase_tong_quat.png)

Bảng: Tóm tắt 17 use case

| Mã | Use case | Tác nhân chính | Yêu cầu | Sprint |
|---|---|---|---|---|
| UC-01 | Đăng ký tài khoản | Khách | FR-01, NFR-03 | S1 |
| UC-02 | Đăng nhập, đặt lại mật khẩu | Người dùng | FR-02, FR-03, NFR-03 | S1 |
| UC-03 | Xem danh mục thủ tục | Khách | FR-06 | S1 |
| UC-04 | Nộp hồ sơ trực tuyến | Người dân | FR-07 → FR-09, FR-14, FR-23, BR-01 | S1 |
| UC-05 | Bổ sung hồ sơ | Người dân | FR-11, FR-14, FR-23 | S1 |
| UC-06 | Rút hồ sơ | Người dân | FR-10, FR-23, BR-04 | S1 |
| UC-07 | Tra cứu trạng thái hồ sơ | Khách | FR-12, FR-13, NFR-04 | S1 |
| UC-08 | Tiếp nhận hồ sơ | CBMC | FR-14 → FR-17, FR-23, BR-01 → BR-03, BR-09 | S2 |
| UC-09 | Phân công xử lý | CBMC | FR-18, FR-23 | S2 |
| UC-10 | Thẩm định, trình phê duyệt | CCTP | FR-14, FR-19, FR-20, FR-23, BR-05 | S2 |
| UC-11 | Phê duyệt kết quả | Lãnh đạo | FR-14, FR-21, FR-23, BR-07, BR-08 | S2 |
| UC-12 | Trả kết quả | CBMC | FR-14, FR-22, FR-23 | S2 |
| UC-13 | Theo dõi hạn xử lý | CBMC, CCTP, Lãnh đạo | FR-24, BR-06 | S2 |
| UC-14 | Xem dashboard, xuất báo cáo | Lãnh đạo | FR-25, FR-26, BR-07 | S2 |
| UC-15 | Quản lý tài khoản cán bộ | Quản trị viên | FR-04, FR-05 | S1 |
| UC-16 | Cấu hình thủ tục, lịch ngày nghỉ | Quản trị viên | FR-27, FR-28 | S2 |
| UC-17 | Gửi thông báo trạng thái | Hệ thống → Dịch vụ email | FR-14 | S1 |

Bốn use case chính dưới đây là xương sống của luồng nghiệp vụ và được đặc tả đầy đủ. 13 use case còn lại có đặc tả trong tài liệu Use case v1.2.

### UC-04. Nộp hồ sơ trực tuyến

Bảng: Đặc tả UC-04 Nộp hồ sơ trực tuyến

| Mục | Nội dung |
|---|---|
| Tác nhân | Người dân |
| Tiền điều kiện | Đã đăng nhập; tài khoản đã xác thực email |
| Hậu điều kiện | Hồ sơ ở trạng thái Đã nộp, có mã tham chiếu; nhật ký ghi nhận; email xác nhận đã xếp hàng gửi |
| Luồng chính | 1. Chọn một thủ tục trong danh mục. 2. Hệ thống hiển thị biểu mẫu điện tử và danh sách thành phần. 3. Điền biểu mẫu, đính kèm tệp cho từng thành phần. 4. Bấm "Nộp hồ sơ". 5. Hệ thống kiểm tra biểu mẫu và đủ thành phần bắt buộc. 6. Hệ thống chuyển sang Đã nộp, cấp mã tham chiếu, hiển thị mã và gửi email |
| Luồng thay thế | 3a. Bấm "Lưu nháp": hồ sơ ở Nháp, kết thúc. 5a. Thiếu thành phần hoặc sai định dạng: báo từng trường lỗi, giữ dữ liệu, quay lại bước 3 |
| Ngoại lệ | 3b. Tệp sai loại hoặc quá dung lượng: từ chối tệp đó, nêu lý do. 6a. Gửi email lỗi: hồ sơ vẫn được nộp; thử lại tối đa 3 lần |
| Yêu cầu | FR-07, FR-08, FR-09, FR-14, FR-23 |

### UC-08. Tiếp nhận hồ sơ

Bảng: Đặc tả UC-08 Tiếp nhận hồ sơ

| Mục | Nội dung |
|---|---|
| Tác nhân | Cán bộ một cửa |
| Tiền điều kiện | Hồ sơ ở trạng thái Đã nộp |
| Hậu điều kiện | Hồ sơ ở Đã tiếp nhận, Chờ bổ sung hoặc Từ chối tiếp nhận; nhật ký và email đã ghi, xếp hàng |
| Luồng chính | 1. Mở hàng đợi, chọn hồ sơ. 2. Hệ thống hiển thị biểu mẫu và tệp. 3. Kiểm tra và chọn "Tiếp nhận"; với thủ tục chứng thực, đối chiếu bản chính và đánh dấu "đã đối chiếu". 4. Hệ thống cấp mã hồ sơ, tính ngày hẹn trả, chuyển Đã tiếp nhận, gửi email. 5. In phiếu hẹn nếu người dân có mặt |
| Luồng mở rộng | 3a. Yêu cầu bổ sung: chọn các thành phần thiếu, ghi hướng dẫn; chuyển Chờ bổ sung. 3b. Từ chối: nhập lý do bắt buộc; chuyển Từ chối tiếp nhận |
| Ngoại lệ | 3c. Hồ sơ vừa bị rút hoặc cán bộ khác vừa thao tác: báo hồ sơ không còn ở trạng thái Đã nộp (409), không cho thao tác |
| Yêu cầu | FR-14 → FR-17, FR-23, BR-01 → BR-03, BR-09 |

### UC-10. Thẩm định và trình phê duyệt

Bảng: Đặc tả UC-10 Thẩm định và trình phê duyệt

| Mục | Nội dung |
|---|---|
| Tác nhân | Công chức tư pháp – hộ tịch |
| Tiền điều kiện | Hồ sơ ở Đang xử lý và được giao cho CCTP này |
| Hậu điều kiện | Hồ sơ ở Chờ phê duyệt, có ý kiến thẩm định và dự thảo kết quả |
| Luồng chính | 1. Mở danh sách hồ sơ được giao, chọn hồ sơ. 2. Ghi ý kiến thẩm định, đính kèm dự thảo PDF. 3. Bấm "Trình phê duyệt". 4. Hệ thống chuyển Chờ phê duyệt, đưa vào danh sách của lãnh đạo |
| Luồng mở rộng | 2a. Cần xác minh (chỉ thủ tục xác nhận tình trạng hôn nhân): đánh dấu, nhập nơi xác minh; hệ thống cộng 3 ngày làm việc vào hạn, gửi email; hồ sơ vẫn ở Đang xử lý |
| Ngoại lệ | 3a. Thiếu ý kiến hoặc dự thảo: không cho trình, nêu lý do |
| Yêu cầu | FR-14, FR-19, FR-20, FR-23, BR-05 |

### UC-11. Phê duyệt kết quả

Bảng: Đặc tả UC-11 Phê duyệt kết quả

| Mục | Nội dung |
|---|---|
| Tác nhân | Lãnh đạo UBND xã |
| Tiền điều kiện | Hồ sơ ở Chờ phê duyệt |
| Hậu điều kiện | Hồ sơ ở Đã có kết quả (thời điểm này dùng để xét đúng hạn), hoặc quay về Đang xử lý |
| Luồng chính | 1. Mở danh sách chờ phê duyệt, chọn hồ sơ. 2. Hệ thống hiển thị hồ sơ, ý kiến, dự thảo. 3. Chọn "Phê duyệt", nhập lại mật khẩu. 4. Hệ thống xác thực mật khẩu, chuyển Đã có kết quả, ghi thời điểm có kết quả, dự thảo trở thành kết quả, gửi email |
| Luồng mở rộng | 3a. Chọn "Trả lại", nhập lý do: hồ sơ về Đang xử lý, báo cho CCTP |
| Ngoại lệ | 4a. Sai mật khẩu: không phê duyệt; sai 3 lần liên tiếp thì khoá thao tác phê duyệt 15 phút |
| Yêu cầu | FR-14, FR-21, FR-23, BR-07, BR-08 |

## 2.6 Tập tính năng cho buổi trình bày và độ truy vết

**Tính năng trình bày.** Buổi trình bày đi theo một hồ sơ chứng thực bản sao từ lúc nộp đến lúc trả kết quả. Cách làm này phủ đủ 5 tác nhân và cả 5 tiêu chí nghiệm thu. Kịch bản chi tiết ở mục 4.8.

Bảng: Tập tính năng trình bày

| # | Tính năng | Use case | Tiêu chí nghiệm thu |
|---|---|---|---|
| 1 | Xem danh mục, nộp hồ sơ trên điện thoại, đính kèm tệp, nhận mã tham chiếu và email | UC-03, UC-04, UC-17 | 1 |
| 2 | Tiếp nhận có đối chiếu bản chính, hệ thống tự tính ngày hẹn trả, in phiếu hẹn | UC-08 | 1, 2 |
| 3 | Yêu cầu bổ sung một lần và người dân nộp lại | UC-05, UC-08 | 1 |
| 4 | Phân công theo tải việc, thẩm định, trình | UC-09, UC-10 | 1 |
| 5 | Phê duyệt bằng ký mô phỏng, trả kết quả điện tử, người dân tải kết quả | UC-11, UC-12 | 1 |
| 6 | Tra cứu công khai chỉ thấy 4 trường | UC-07 | 4 |
| 7 | Dashboard đúng hạn, trễ hạn, xuất CSV | UC-13, UC-14 | 3 |
| 8 | Nhật ký không sửa được (thử `UPDATE` trong CSDL bị từ chối) | FR-23 | 4, 5 |

**Độ truy vết.** RTM được sinh tự động bằng `tools/req.py rtm` từ `requirements.csv` và các tag trên test. CI chặn merge nếu một yêu cầu mức M của sprint đã đến hạn mà chưa có test tham chiếu.

Bảng: Độ phủ truy vết tại commit `1c49d1d`

| Nhóm | Số yêu cầu | Có use case | Có mã nguồn | Có test tham chiếu |
|---|---|---|---|---|
| Yêu cầu chức năng | 28 | 28 | 28 | 28 |
| Yêu cầu phi chức năng | 10 | Không áp dụng | 10 | 10 |
| Quy tắc nghiệp vụ | 9 | 9 | 9 | 9 |
| **Tổng** | **47** | | | **47 (100%)** |

Phụ lục A có RTM rút gọn. RTM đầy đủ nằm ở `docs/test/eHoSo_RTM_v1.2.csv`.
