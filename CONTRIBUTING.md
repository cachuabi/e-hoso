# Quy trình đóng góp (GitHub Flow)

1. **Nhận việc**: mỗi việc là một issue trên GitHub Projects, tối đa 8 giờ, có ước lượng giờ và story points.
2. **Tạo nhánh từ `main`**: `git switch -c feat/FR-09-nop-ho-so` (tiền tố `feat/`, `fix/`, `docs/`, `test/`, `chore/`).
3. **Làm theo TDD**: test ghi mã yêu cầu (`@Tag("FR-09")` hoặc `it('[FR-09] ...')`).
4. **Kiểm tra trước khi đẩy**: `./scripts/check.sh` phải xanh.
5. **Mở Pull Request**:
   - Tiêu đề theo Conventional Commits và có mã yêu cầu, ví dụ `feat(dossier): nộp hồ sơ (FR-09)`.
   - Điền mẫu PR, kèm `Closes #<số issue>`.
6. **Review**: cần ít nhất **1 người duyệt** và CI xanh (nhánh `main` được bảo vệ). Mục tiêu duyệt trong 48 giờ.
7. **Merge**: chọn *Squash and merge*. Cập nhật "Thực tế (giờ)" trong issue rồi đóng issue.

## Định nghĩa hoàn thành (Team Charter)
Đã review và merge, CI xanh, có unit test, tài liệu cập nhật, task chuyển Done.

## Làm cùng Claude Code
Repo đã cài SDLC kit (`.claude/`). Trong `claude`, gõ `/task 12` để Claude làm issue #12 theo đúng quy trình trên, hoặc `/review-diff` để tự review trước khi mở PR. Các hook chặn push thẳng lên `main` và không cho Claude dừng khi `check.sh` còn đỏ.
