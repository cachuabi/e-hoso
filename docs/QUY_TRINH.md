# Quy trình làm việc của nhóm (từ 28/10/2026)

Theo chỉ đạo của PM Đỗ Văn Tiến (QD-03, 28/10/2026): nhóm không đủ thời gian làm từng việc, nên Claude làm thay mọi vai theo đúng quy trình sdlc-kit (`.claude/sdlc/RULES.md`). Thành viên vẫn đọc hiểu phần của mình để tự trình bày khi bảo vệ. Báo cáo cuối kỳ có phụ lục ghi nhận phần việc Claude làm và cơ chế duyệt dưới đây.

**Nguyên tắc trung thực:** mọi hồ sơ quy trình phản ánh đúng việc đã xảy ra. Không lập biên bản cho cuộc họp không diễn ra; không ghi tên một người là người duyệt hay người tham dự khi người đó chưa xác nhận. Việc Claude làm ghi rõ "do Claude thực hiện theo vai …".

## Vai trò

| Vai | Thành viên | Điều phối mốc |
|---|---|---|
| PM + BA | Đỗ Văn Tiến | M1 |
| Tech Lead | Mai Thành Khang | M2 |
| Backend + CSDL | Đoàn Quang Anh | M3 |
| Frontend + UI/UX | Đỗ Văn Phương | M4 |
| QA + DevOps | Mai Xuân Đạt | M5 |

## Duyệt tài liệu: 3 tầng

| Tầng | Ai | Hồ sơ | Kết quả |
|---|---|---|---|
| 1. Tự kiểm | Claude, vai tác giả | Mục tự kiểm trong bản nháp | Chờ review |
| 2. Review chéo | Claude, vai người duyệt được chỉ định | Phiếu `RV-<Mốc>-<số>` trong `docs/<thư mục>/reviews/` (mẫu `docs/templates/PhieuReview.md`) | Đã review: việc phụ thuộc chạy tiếp |
| 3. Phê duyệt | PM (người thật), gộp theo mốc | `PheDuyet_<Mốc>.md` (mẫu `docs/templates/PheDuyetMoc.md`) | Xong |

## Duyệt mã nguồn và tài liệu trên GitHub (QD-04)

- Mỗi thay đổi đi qua nhánh riêng và Pull Request; `main` được bảo vệ: 1 người duyệt, CI xanh.
- PM mời thành viên vào repo (quyền Write) và gán người duyệt cho từng task; thành viên được gán duyệt PR trên GitHub. PM là người duyệt cuối khi cần.
- Người duyệt bắt buộc theo phạm vi thư mục: `.github/CODEOWNERS`; nhánh `main` bật "Require review from Code Owners" (QD-08).
- Người thực hiện: Assignee của issue là thành viên chịu trách nhiệm phần việc theo vai (nắm nội dung để trình bày khi bảo vệ).
- PR do tài khoản PM mở thì PM không tự duyệt được trên GitHub; PM chỉ dùng quyền Admin để bỏ qua quy định trong trường hợp khẩn cấp và ghi lý do vào PR.

| GitHub | Thành viên | Vai |
|---|---|---|
| `cachuabi` | Đỗ Văn Tiến | PM + BA |
| `KhangHandome` | Mai Thành Khang | Tech Lead |
| `quanganh2302` | Đoàn Quang Anh | Backend + CSDL |
| `dovanphuong99a0-cmd` | Đỗ Văn Phương | Frontend + UI/UX |
| `maixuandat08` | Mai Xuân Đạt | QA + DevOps |

Package backend theo module (khớp `CODEOWNERS`, SDD dùng đúng tên này): `account`, `auth`, `workflow` (Khang); `dossier`, `file`, `notification`, `audit`, `dashboard`, `catalog` và migration CSDL `resources/db` (Quang Anh).
- Claude không merge. Người duyệt PR là cổng cuối cùng.

## Quyết định

- Quyết định trong vai (kỹ thuật, thiết kế): Claude quyết theo vai phụ trách, ghi vào sổ quyết định (QD-xx); PM xác nhận ở cổng mốc.
- Thay đổi yêu cầu, phạm vi, lịch: Claude soạn đề xuất; PM quyết. Trên GitHub là issue nhãn `decision`.
- Điểm dừng bắt buộc của `RULES.md` (push, PR, merge, xoá dữ liệu, thêm package, dịch vụ, schema): hỏi PM ngay.

## Họp và báo cáo

| Loại | Khi nào | Hồ sơ |
|---|---|---|
| Họp tuần | Chủ nhật 20:30, 15–20 phút | `meetings/YYYY-MM-DD_hop-tuan-<Mốc>.md`: Claude soạn chương trình và dự thảo trước; hoàn thiện sau họp theo phản hồi của PM (người dự, quyết định) |
| Tuần không họp | – | `meetings/YYYY-MM-DD_bao-cao-tuan.md`, ghi rõ không có họp |
| Nhật ký ngày (standup bất đồng bộ) | Ngày có hoạt động | `meetings/daily/YYYY-MM-DD.md` (mẫu `docs/templates/NhatKyNgay.md`), lấy từ hoạt động thật |
