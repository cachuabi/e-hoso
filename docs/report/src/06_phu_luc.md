# Phụ lục

## Phụ lục A. Ma trận truy vết rút gọn

Sinh từ `docs/test/eHoSo_RTM_v1.2.csv`: mỗi yêu cầu, use case liên quan và số test tham chiếu. Tên test, module và endpoint của từng yêu cầu có ở tệp RTM đầy đủ.

Bảng: Ma trận truy vết rút gọn (47 yêu cầu, 47 có test)

| Mã | Use case | Test | Mã | Use case | Test | Mã | Use case | Test |
|---|---|---|---|---|---|---|---|---|
| FR-01 | UC-01 | 2 | FR-17 | UC-08 | 2 | NFR-05 | UC-04–06, UC-08–12 | 3 |
| FR-02 | UC-02 | 1 | FR-18 | UC-09 | 2 | NFR-06 | UC-13 | 2 |
| FR-03 | UC-02 | 1 | FR-19 | UC-10 | 3 | NFR-07 | Toàn hệ thống | 1 |
| FR-04 | UC-15 | 1 | FR-20 | UC-10 | 2 | NFR-08 | Toàn hệ thống | 1 |
| FR-05 | Mọi UC | 3 | FR-21 | UC-11 | 3 | NFR-09 | Toàn hệ thống | 2 |
| FR-06 | UC-03 | 4 | FR-22 | UC-12 | 3 | NFR-10 | UC-08, UC-13 | 3 |
| FR-07 | UC-04 | 4 | FR-23 | UC-04–06, UC-08–12 | 3 | BR-01 | UC-04, UC-08 | 2 |
| FR-08 | UC-04 | 6 | FR-24 | UC-13 | 3 | BR-02 | UC-08 | 1 |
| FR-09 | UC-04 | 4 | FR-25 | UC-14 | 1 | BR-03 | UC-08 | 1 |
| FR-10 | UC-06 | 3 | FR-26 | UC-14 | 1 | BR-04 | UC-06 | 3 |
| FR-11 | UC-05 | 2 | FR-27 | UC-16 | 2 | BR-05 | UC-10 | 2 |
| FR-12 | UC-07 | 2 | FR-28 | UC-16 | 1 | BR-06 | UC-13 | 2 |
| FR-13 | UC-07 | 2 | NFR-01 | Toàn hệ thống | 2 | BR-07 | UC-11, UC-14 | 3 |
| FR-14 | UC-17 | 3 | NFR-02 | UC-04 | 1 | BR-08 | UC-11 | 1 |
| FR-15 | UC-08 | 2 | NFR-03 | UC-01–02 | 4 | BR-09 | UC-05, UC-08 | 1 |
| FR-16 | UC-08 | 5 | NFR-04 | UC-07, UC-12 | 2 |  |  |  |

## Phụ lục B. Hướng dẫn chạy demo

**Yêu cầu.** Docker và tối thiểu 8 GB RAM.

1. Lấy mã nguồn: `git clone https://github.com/cachuabi/e-hoso.git`, rồi vào thư mục `e-hoso`.
2. Khởi chạy: `docker compose up -d --build`.
3. Mở `https://localhost` và chấp nhận cảnh báo chứng chỉ nội bộ. Trước khi tải tệp, mở `https://localhost:9443` một lần và chấp nhận cảnh báo tương tự.
4. Xem email thử nghiệm tại `http://localhost:8025` (Mailpit).
5. Đăng nhập bằng các tài khoản demo trong bảng dưới. Mật khẩu chung là `Demo12345`.
6. Xoá toàn bộ dữ liệu để demo lại từ đầu: `docker compose down -v`.

Bảng: Tài khoản demo (dữ liệu giả lập)

| Vai trò | Email | Ghi chú |
|---|---|---|
| Người dân | `dan@ehoso.local` | 4 số cuối định danh: `1234` |
| Cán bộ một cửa | `motcua@ehoso.local` | |
| Công chức tư pháp – hộ tịch | `tuphap@ehoso.local` | |
| Lãnh đạo | `lanhdao@ehoso.local` | |
| Quản trị viên | `admin@ehoso.local` | |

**Kiểm tra chất lượng.** Chạy `./scripts/check.sh` tại thư mục repo; chỉ exit code 0 mới là đạt. Kiểm thử đầu cuối chạy bằng workflow `E2E` trên GitHub Actions, bằng chứng lưu ở nhánh `evidence` theo thư mục mã commit.

## Phụ lục C. Khai báo sử dụng AI

Nhóm dùng **Claude (Anthropic)** trong suốt dự án, qua Claude Code và Claude.

Bảng: Mức độ sử dụng AI theo hạng mục

| Hạng mục | Mức độ dùng AI |
|---|---|
| Tài liệu M0 → M5 và báo cáo này | Claude soạn theo vai từng thành viên, theo outline và quyết định của PM |
| Sơ đồ UML, DFD | Claude viết mã PlantUML; sơ đồ được đối chiếu với mã nguồn |
| Mã nguồn, test, CI, hạ tầng | Claude viết toàn bộ theo chỉ đạo của PM ngày 04/11/2026 |
| Quyết định phạm vi, lịch, phương án | PM quyết định, Claude đề xuất; ghi ở sổ quyết định QD-xx |
| Duyệt và merge | Theo quy trình 3 tầng; PM duyệt mốc; Claude không tự merge |
| Kiểm thử | Claude viết và chạy qua GitHub Actions và trên máy PM. Thành viên tự chạy lại: ? |

**Lý do.** Các thành viên bận công việc. PM chủ động giao Claude hoàn thiện sản phẩm để kịp hạn 10/11/2026 (CR-02).

**Kiểm soát.** Mọi số liệu trong báo cáo lấy từ log CI, lần chạy `./scripts/check.sh` và nhánh `evidence`. Chỗ chưa biết ghi `?`.

**Trách nhiệm.** Nhóm chịu trách nhiệm về nội dung và tự trình bày khi bảo vệ.
