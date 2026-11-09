::: cover
top: TRƯỜNG QUỐC TẾ – ĐẠI HỌC QUỐC GIA HÀ NỘI · HỌC PHẦN INS6021 PHÁT TRIỂN PHẦN MỀM
title: e-HoSo
subtitle: BÁO CÁO CUỐI KỲ
desc: Hệ thống tiếp nhận, xử lý và theo dõi hồ sơ thủ tục hành chính trực tuyến cấp xã/phường
version: Bản nháp tiếng Việt · Phiên bản 2.0 · 09/11/2026
team: **Nhóm 1**
members: Đỗ Văn Tiến · Mai Thành Khang · Đoàn Quang Anh · Đỗ Văn Phương · Mai Xuân Đạt
teacher: Giảng viên: Trương Công Đoàn
place: Hà Nội, 11/2026
:::

## Lịch sử phiên bản

Bảng: Lịch sử phiên bản tài liệu

| Phiên bản | Ngày | Người thực hiện | Nội dung |
|---|---|---|---|
| 1.0 | 05/11/2026 | Mai Xuân Đạt điều phối, Claude thực hiện theo vai | Báo cáo tóm tắt 11 trang, số liệu từ CI commit `591470a` |
| 2.0 (nháp) | 09/11/2026 | Claude soạn theo outline đã chốt ngày 09/11/2026 | Viết lại theo cấu trúc 5 chương của đề bài; thêm DFD mức 1, mức 2, 3 sơ đồ activity, 5 sơ đồ trạng thái, sơ đồ lớp thiết kế, 2 sơ đồ kiến trúc; số liệu kiểm thử cập nhật trên `main` (commit `1c49d1d`) và lần chạy E2E cho `39648eb`. Đã review chéo theo vai (RV-M5-01, RV-M5-02). **Chờ anh Đỗ Văn Tiến duyệt** |

**Ngày trình bày cuối:** ? *(để trống theo quyết định ngày 09/11/2026; mốc "April 10th" trong mẫu đề là phần sót lại)*.

## Phân công thành viên

Phân công theo Project Plan. Cột "Đóng góp thực tế" và "Giờ thực tế" do từng thành viên tự khai trước khi nộp. Chỗ chưa có bằng chứng ghi `?`.

Bảng: Phân công thành viên Nhóm 1

| Thành viên | Vai trò | Phần phụ trách theo kế hoạch | Đóng góp thực tế | Giờ thực tế |
|---|---|---|---|---|
| Đỗ Văn Tiến | Trưởng nhóm (PM), phân tích nghiệp vụ (BA), chuyên gia nghiệp vụ | Kế hoạch, SRS, nguồn yêu cầu, UAT, duyệt mốc | Quyết định QD-01 → QD-13, CR-01, CR-02; duyệt M1, M2; merge PR #46, #47, #49 (tài khoản `cachuabi`); PR #51, #52 được merge ngày 09/11/2026 | ? |
| Mai Thành Khang | Trưởng kỹ thuật (Tech Lead) | Kiến trúc, SDD, backend tài khoản, luồng xử lý | Duyệt PR #47 ngày 03/11/2026 | ? |
| Đoàn Quang Anh | Backend, cơ sở dữ liệu | Khảo sát, ERD, API, hồ sơ, tệp, email | ? | ? |
| Đỗ Văn Phương | Frontend, UI/UX | Prototype, giao diện, hướng dẫn sử dụng | ? | ? |
| Mai Xuân Đạt | Kiểm thử (QA), DevOps | Test Plan, RTM, kiểm thử, CI | ? | ? |

[[PAGEBREAK]]

## Mục lục

[[TOC]]

[[PAGEBREAK]]

## Danh mục hình

[[LOF]]

## Danh mục bảng

[[LOT]]

## Danh mục từ viết tắt

Bảng: Từ viết tắt

| Viết tắt | Nghĩa |
|---|---|
| BR | Quy tắc nghiệp vụ (Business Rule) |
| CBMC | Cán bộ một cửa |
| CCTP | Công chức tư pháp – hộ tịch |
| CSDL | Cơ sở dữ liệu |
| DFD | Sơ đồ luồng dữ liệu (Data Flow Diagram) |
| DR | Yêu cầu dữ liệu (Data Requirement) |
| FR, NFR | Yêu cầu chức năng, yêu cầu phi chức năng |
| JWT | JSON Web Token |
| KS | Phát hiện khảo sát |
| MVP | Sản phẩm khả dụng tối thiểu |
| RBAC | Phân quyền theo vai trò |
| RTM | Ma trận truy vết yêu cầu |
| SPA | Ứng dụng web một trang |
| UC | Use case |

## Tóm tắt

e-HoSo số hoá toàn trình 3 thủ tục tư pháp – hộ tịch cấp xã: chứng thực bản sao từ bản chính, đăng ký khai sinh, cấp Giấy xác nhận tình trạng hôn nhân. Người dân nộp và theo dõi hồ sơ trực tuyến. Cán bộ một cửa, công chức tư pháp – hộ tịch và lãnh đạo UBND xã xử lý hồ sơ trên một vòng đời 10 trạng thái do máy chủ kiểm soát. Hệ thống tự tính ngày hẹn trả theo mốc 15:00 và lịch ngày làm việc, gửi email theo từng sự kiện, ghi nhật ký không sửa được và thống kê tỷ lệ đúng hạn cho lãnh đạo.

Sản phẩm là một monolith Spring Boot chia 9 module, kèm SPA React. Toàn bộ hệ thống chạy bằng một lệnh `docker compose up` gồm proxy Caddy, PostgreSQL 17, kho tệp Garage tương thích S3 và Mailpit.

Bảng: Kết quả chính của dự án

| Kết quả | Giá trị |
|---|---|
| Phạm vi đã xây dựng | 28/28 yêu cầu chức năng, 17 use case, 3 thủ tục |
| Truy vết | 47/47 yêu cầu FR, NFR, BR có test tham chiếu |
| Test backend | 79 test đạt; độ phủ dòng tầng service 93,4% (1058/1133 dòng), ngưỡng 70% |
| Test frontend | 5 test đạt |
| Đầu cuối trên Docker Compose | 9/9 test Playwright đạt trên x86 và arm64 |
| Tiêu chí nghiệm thu MVP | 5/5 đạt trên môi trường tự động |
| Chưa kiểm | 3 người dùng thật (CK-U02); hiệu năng trên đúng máy 2 vCPU, 4 GB; Safari, Edge thật |

Toàn bộ dữ liệu trong hệ thống và trong báo cáo là dữ liệu giả lập (NFR-04). Việc dùng AI được khai báo ở Phụ lục C.
