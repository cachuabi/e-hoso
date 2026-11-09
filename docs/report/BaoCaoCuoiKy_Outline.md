# e-HoSo – Outline Báo cáo cuối kỳ (đã chốt 09/11/2026)

Hạn nộp: 12/11/2026. Độ dài mục tiêu: 45–50 trang Word. Bản tiếng Việt trước, dùng định dạng chuẩn của nhóm: bìa, lịch sử phiên bản, Times New Roman, bảng có viền. Khi dịch sang tiếng Anh, anh Tiến sẽ gửi mẫu báo cáo của Trường Quốc tế để áp dụng.

Nguyên tắc: sơ đồ phải vẽ đủ ở những mục đề bài yêu cầu "all"; phần diễn giải chi tiết chỉ dành cho các case chính, phần còn lại trình bày bằng bảng gọn.

## Quyết định đã chốt
- Nguồn yêu cầu từ người dùng (mục 1.4): ghi anh Đỗ Văn Tiến là chuyên gia nghiệp vụ, đang làm chuyên gia thực tế tại Bộ Tư pháp, kết hợp khảo sát văn bản pháp luật (KS-08 → KS-14).
- Ngày trình bày cuối (mốc "April 10th" là phần sót từ mẫu đề): để trống.
- "Hyperparameter & architecture choices": trình bày thành các phương án kiến trúc đã cân nhắc (ví dụ ADR-001 chọn Garage thay MinIO) và các tham số cấu hình vận hành (mốc 15:00, hạn JWT, giới hạn tệp, số lần gửi lại email).
- PR #51 (tài liệu M2) và PR #52 (mã chạy được) đã merge vào `main` ngày 09/11/2026, CI đạt.

## Outline (khoảng 49 trang, khoảng 31 hình)

**Phần đầu (4 tr):** bìa, phân công thành viên, mục lục, danh mục hình và bảng, tóm tắt.

**Chương 1. Overall Description (5 tr)**
- 1.1 Bối cảnh và động lực
- 1.2 Phát biểu bài toán (KS-01 → KS-14)
- 1.3 Mục tiêu, phạm vi, ngoài phạm vi
- 1.4 Nguồn thu thập yêu cầu (chuyên gia nghiệp vụ, văn bản pháp luật) và giới hạn
- 1.5 Tóm tắt sản phẩm: tính năng, đối tượng sử dụng, nhu cầu nghiệp vụ được đáp ứng, giao diện tổng quát
- 1.6 Quy trình phát triển và tổ chức nhóm

**Chương 2. System Requirements (9 tr)**
- 2.1 Yêu cầu giao diện: giao diện người dùng, REST API, SMTP, kho tệp S3
- 2.2 28 yêu cầu chức năng (MoSCoW)
- 2.3 10 yêu cầu phi chức năng
- 2.4 Quy tắc nghiệp vụ BR-01 → BR-09 và yêu cầu dữ liệu DR-01 → DR-08
- 2.5 Use case: sơ đồ đủ 17 UC, bảng tóm tắt, kịch bản chi tiết cho UC-04, UC-08, UC-10, UC-11
- 2.6 Tập tính năng cho buổi trình bày và độ truy vết (RTM)

**Chương 3. System Modeling (16 tr)**
- 3.1 Mô hình quy trình: sơ đồ ngữ cảnh, DFD mức 1, DFD mức 2 (xử lý hồ sơ), mô tả process, data store, data flow, external entity, các thay đổi so với M2
- 3.2 Activity: 3 sơ đồ swimlane, cùng nhau phủ đủ 17 UC
  - Người dân: UC-01 → UC-07
  - Cán bộ xử lý: UC-08 → UC-13
  - Lãnh đạo và quản trị: UC-14 → UC-17
- 3.3 Sequence: UC-04, UC-08, UC-10, UC-11
- 3.4 State machine cho 6 đối tượng: Hồ sơ (chi tiết), Tài khoản, Mã xác thực, Tệp đính kèm, Yêu cầu bổ sung, Thông báo
- 3.5 Class: sơ đồ lớp miền và sơ đồ lớp thiết kế đủ 7 controller (Auth, AdminUser, Catalog, Dossier, Attachment, Workflow, Dashboard), service, repository, có chiều điều hướng
- 3.6 Data model: ERD, mô tả thực thể, thuộc tính, quan hệ; các thay đổi so với M2
- 3.7 Data schema: 13 bảng với kiểu dữ liệu, ràng buộc, PK/FK
  - Trình bày đầy đủ: `dossier`, `attachment`, `processing_log`
  - Còn lại: bảng gọn

**Chương 4. System Development (10 tr)**
- 4.1 Kiến trúc: tổng quan, mô hình client–server, ánh xạ logic → vật lý (phần cứng, phần mềm, thủ công)
- 4.2 Nền tảng, ngôn ngữ, công cụ; các phương án và tham số đã cân nhắc
- 4.3 API `/api/v1` và ma trận phân quyền
- 4.4 Giao diện (8 ảnh từ nhánh `evidence`)
- 4.5 Quản lý dữ liệu: migration, dữ liệu giả lập, kho tệp, sao lưu
- 4.6 Bảo mật và độ tin cậy
- 4.7 Kết quả kiểm thử và phân tích
- 4.8 Kịch bản demo

**Chương 5. Conclusion (2 tr):** kết quả đạt được, hạn chế, bài học, hướng phát triển.

**Tài liệu tham khảo (1 tr). Phụ lục (2 tr):** A. RTM rút gọn; B. Hướng dẫn chạy demo; C. Khai báo sử dụng AI.

## Việc tiếp theo
1. ~~Vẽ 13 sơ đồ còn thiếu bằng PlantUML: DFD mức 1 và mức 2; 3 activity; 5 state machine; class thiết kế và kiến trúc.~~ **Xong 09/11:** sơ đồ 14 → 26 trong `docs/design/uml/`. Sơ đồ lớp thiết kế tách 2 hình (24, 24b) cho dễ đọc, nên tổng cộng là 14 hình mới.
2. Viết bản nháp tiếng Việt (.docx), rồi gửi anh Tiến duyệt. **Bản nháp xong 09/11:** `docs/report/eHoSo_BaoCaoCuoiKy_v2.0.docx`, nguồn Markdown ở `docs/report/src/`, dựng lại bằng `python docs/report/build_docx.py`. **Chờ anh Tiến duyệt.**
3. Sau khi chốt bản cuối: dịch sang tiếng Anh theo mẫu của Trường Quốc tế.

## Nguồn sẵn có
- Repo `cachuabi/e-hoso`, nhánh `main`:
  - `docs/requirements`: SRS, Use case v1.2, RTM v1.2
  - `docs/design`: SDD v1.2, 13 sơ đồ .puml, ERD, OpenAPI, `V1__init.sql`
  - `docs/test`: Test Report, Báo cáo cuối kỳ v1.0 (11 trang)
- Nhánh `evidence`, commit `591470a`: 14 ảnh màn hình, `demo.webm`, kết quả k6, ZAP, axe
