# Chương 1. Mô tả tổng quan (Overall Description)

## 1.1 Bối cảnh và động lực

Từ ngày 01/7/2025, Việt Nam vận hành chính quyền địa phương hai cấp. Nhiều thủ tục trước đây do cấp huyện giải quyết được chuyển về cấp xã, làm khối lượng hồ sơ tại bộ phận một cửa của UBND xã, phường tăng lên. Phần lớn hồ sơ tư pháp – hộ tịch vẫn được tiếp nhận bằng giấy, ghi sổ thủ công và theo dõi hạn bằng bảng tính.

Ba thủ tục được chọn cho MVP là chứng thực bản sao từ bản chính, đăng ký khai sinh và cấp Giấy xác nhận tình trạng hôn nhân. Lý do chọn: đây là các thủ tục phát sinh thường xuyên ở cấp xã, có thời hạn ngắn (trong ngày hoặc 3 ngày làm việc) nên việc tính hạn sai hay sót hạn ảnh hưởng ngay đến người dân, và ba thủ tục đủ khác nhau để kiểm chứng thiết kế. Một thủ tục cần đối chiếu bản chính, một thủ tục có biểu mẫu riêng theo luật, một thủ tục có thể gia hạn do xác minh.

## 1.2 Phát biểu bài toán

Nhóm xác định 14 phát hiện khảo sát. Bảy phát hiện KS-01 → KS-07 là vấn đề của người dùng; bảy phát hiện KS-08 → KS-14 là quy định nghiệp vụ rút từ văn bản pháp luật.

Bảng: Các vấn đề của người dùng (KS-01 → KS-07)

| Mã | Vấn đề | Người chịu ảnh hưởng | Yêu cầu đáp ứng |
|---|---|---|---|
| KS-01 | Phải đến trực tiếp để biết cần giấy tờ gì và để nộp hồ sơ giấy; hồ sơ thiếu thì quay lại nhiều lần | Người dân | FR-01, FR-06 → FR-09, FR-11, BR-04 |
| KS-02 | Sau khi nộp không biết hồ sơ đang ở bước nào, khi nào có kết quả | Người dân, CBMC | FR-12 → FR-14, FR-22 |
| KS-03 | Tiếp nhận ghi sổ thủ công; mã hồ sơ, giấy hẹn lập bằng tay; quyền của từng cán bộ không rõ | CBMC | FR-04, FR-09, FR-15, FR-16, BR-01 |
| KS-04 | Hạn xử lý theo dõi bằng sổ và bảng tính, không có cảnh báo sắp đến hạn, quá hạn | CBMC, CCTP | FR-24, BR-06 |
| KS-05 | Phân công bằng chuyển tay; không biết mỗi người đang giữ bao nhiêu hồ sơ | CBMC, CCTP | FR-18, FR-19 |
| KS-06 | Lãnh đạo thiếu số liệu đúng hạn, trễ hạn; việc ký duyệt và trả lại không được ghi nhận | Lãnh đạo | FR-21, FR-25, FR-26, BR-07 |
| KS-07 | Không có vết xử lý: ai làm gì, lúc nào, với hồ sơ nào | Lãnh đạo, cán bộ | FR-23 |

Bảng: Các quy định nghiệp vụ (KS-08 → KS-14)

| Mã | Quy định | Căn cứ | Yêu cầu đáp ứng |
|---|---|---|---|
| KS-08 | Chứng thực bản sao giải quyết trong ngày, hoặc ngày làm việc tiếp theo nếu nhận sau 15 giờ; phải xuất trình bản chính để đối chiếu | Nghị định 23/2015/NĐ-CP | FR-16, FR-17, BR-02 |
| KS-09 | Đăng ký khai sinh giải quyết ngay khi nhận đủ giấy tờ; tờ khai theo mẫu riêng | Luật Hộ tịch 2014, Điều 16 | FR-07, BR-02 |
| KS-10 | Xác nhận tình trạng hôn nhân trong 03 ngày làm việc, được kéo dài khi cần xác minh; giấy có giá trị 6 tháng | Nghị định 123/2015/NĐ-CP, Điều 22, 23 | FR-20, BR-02, BR-05, BR-08 |
| KS-11 | Thời hạn tính theo ngày làm việc, trừ thứ Bảy, Chủ nhật, ngày nghỉ lễ; có ngày làm bù | Lịch nghỉ lễ hằng năm | FR-17, FR-28, BR-03 |
| KS-12 | Mốc 15 giờ: nhận sau 15 giờ mà không giải quyết ngay được thì trả trong ngày làm việc tiếp theo | Luật Hộ tịch 2014, Điều 5 | FR-17, BR-03 |
| KS-13 | Thời hạn tính từ khi cơ quan tiếp nhận hồ sơ hợp lệ, không phải từ lúc nộp trực tuyến | Nghị định 118/2025/NĐ-CP | BR-02 |
| KS-14 | Hồ sơ chưa đủ thì hướng dẫn bổ sung đầy đủ, một lần | Nghị định 118/2025/NĐ-CP | FR-11, FR-16, BR-09 |

**Phát biểu bài toán.** Xây dựng một ứng dụng web cho một UBND xã, cho phép người dân nộp và theo dõi hồ sơ của 3 thủ tục mà không phải đến trực tiếp để hỏi. Cán bộ phải xử lý được hồ sơ theo đúng trình tự một cửa, với ngày hẹn trả tự tính đúng quy định, cảnh báo hạn, phân công có số liệu tải việc và nhật ký không sửa được. Lãnh đạo phải có số liệu đúng hạn, trễ hạn.

## 1.3 Mục tiêu, phạm vi và ngoài phạm vi

**Mục tiêu.** Dự án đặt 5 tiêu chí nghiệm thu MVP (SRS mục 11):

1. Một hồ sơ của mỗi thủ tục đi trọn từ Nháp đến Đã trả kết quả trên bản demo.
2. Ngày hẹn trả đúng với mọi ca kiểm thử của BR-03: trước và sau 15:00, thứ Sáu, ngày nghỉ lễ.
3. Dashboard hiển thị đúng số hồ sơ đúng hạn, trễ hạn cho bộ dữ liệu kiểm thử.
4. Mọi yêu cầu mức M có ít nhất một ca kiểm thử đạt theo RTM.
5. Không còn lỗi mức Critical.

**Phạm vi.** 28 yêu cầu chức năng chia 6 nhóm: tài khoản và phân quyền, nộp hồ sơ, tra cứu và thông báo, xử lý hồ sơ, theo dõi và báo cáo, quản trị. Kèm theo là 10 yêu cầu phi chức năng, 9 quy tắc nghiệp vụ và 8 yêu cầu dữ liệu.

**Ngoài phạm vi.** Kết nối thật với Cổng Dịch vụ công quốc gia và VNeID; thanh toán phí, lệ phí; ký số thật (chỉ ký mô phỏng bằng mật khẩu); ứng dụng di động gốc; vận hành cho nhiều xã cùng lúc; tự động huỷ hồ sơ chờ bổ sung quá lâu (QD-06).

## 1.4 Nguồn thu thập yêu cầu và giới hạn

Yêu cầu được thu thập từ hai nguồn:

- **Chuyên gia nghiệp vụ.** Anh Đỗ Văn Tiến, trưởng nhóm, đang làm chuyên gia thực tế tại Bộ Tư pháp. Anh cung cấp hiểu biết về quy trình một cửa và các khó khăn thường gặp. Các vấn đề KS-01 → KS-07 được hình thành dựa trên kinh nghiệm thực tế của chuyên gia này.
- **Khảo sát văn bản pháp luật.** Luật Hộ tịch 2014, Nghị định 123/2015/NĐ-CP (hộ tịch), Nghị định 23/2015/NĐ-CP (chứng thực), Nghị định 118/2025/NĐ-CP (cơ chế một cửa). Đây là nguồn của KS-08 → KS-14, tức các quy tắc về thời hạn, mốc 15 giờ, ngày làm việc và nguyên tắc bổ sung một lần.

Yêu cầu được ghi theo ISO/IEC/IEEE 29148 trong SRS (v1.0 → v1.2). Tệp `docs/requirements/requirements.csv` là nguồn sự thật để sinh issue và RTM.

**Giới hạn.**

- Nhóm chưa khảo sát tại một UBND xã cụ thể và chưa kiểm thử với người dùng thật (CK-U02). Tính dễ dùng mới được kiểm bằng công cụ tự động.
- Thời hạn và mức gia hạn khi xác minh của thủ tục xác nhận tình trạng hôn nhân chưa được đối chiếu lại với Bộ thủ tục hành chính đang công bố (OI-02, trạng thái: ?). Nếu khác, chỉ cần sửa cấu hình thủ tục (FR-27), không phải sửa mã (giả định A1).

## 1.5 Tóm tắt sản phẩm

**Đối tượng sử dụng.** Bảng dưới đây tóm tắt 7 tác nhân, trong đó 6 là người dùng và 1 là hệ thống ngoài.

Bảng: Tác nhân và nhu cầu được đáp ứng

| Tác nhân | Số lượng dự kiến | Nhu cầu nghiệp vụ được đáp ứng |
|---|---|---|
| Khách | Không giới hạn | Xem danh mục thủ tục, thành phần, thời hạn; tra cứu hồ sơ bằng mã và 4 số cuối định danh |
| Người dân | Vài nghìn | Nộp hồ sơ trực tuyến, đính kèm tệp, bổ sung, rút, theo dõi trạng thái, nhận email, tải kết quả điện tử |
| Cán bộ một cửa (CBMC) | 2–4 | Hàng đợi hồ sơ, tiếp nhận, yêu cầu bổ sung một lần, từ chối, phân công theo tải việc, in phiếu hẹn, trả kết quả |
| Công chức tư pháp – hộ tịch (CCTP) | 2–3 | Thẩm định hồ sơ được giao, đánh dấu xác minh (gia hạn 3 ngày làm việc), dự thảo kết quả, trình |
| Lãnh đạo UBND xã | 1–3 | Phê duyệt bằng ký mô phỏng hoặc trả lại có lý do; dashboard đúng hạn, trễ hạn; xuất CSV |
| Quản trị viên | 1 | Quản lý tài khoản cán bộ, cấu hình thời hạn và thành phần thủ tục, lịch ngày nghỉ |
| Dịch vụ email | 1 | Nhận thư thông báo qua SMTP |

**Tính năng chính.**

- Biểu mẫu điện tử riêng cho từng thủ tục, sinh từ cấu hình JSON; lưu nháp; đính kèm tệp theo thành phần, kiểm tra loại tệp theo nội dung.
- Vòng đời hồ sơ 10 trạng thái, 12 hành động, kiểm tra phía máy chủ; mọi chuyển trạng thái ghi nhật ký trong cùng giao dịch.
- Tự cấp mã tham chiếu (TC-xxxxxxxx) khi nộp và mã hồ sơ (HS-yyMMdd-nnnn) khi tiếp nhận; tự tính ngày hẹn trả theo mốc 15:00 và lịch ngày làm việc.
- Nhãn hạn "Còn hạn", "Sắp đến hạn", "Quá hạn" bằng chữ và màu; dashboard đúng hạn, trễ hạn theo thủ tục và theo CCTP.
- Email theo 10 loại sự kiện, gửi qua hàng đợi có thử lại.
- Tra cứu công khai chỉ trả 4 trường, không lộ thông tin cá nhân.

**Giao diện tổng quát.** Giao diện là một SPA tiếng Việt gồm 3 phân hệ: cổng người dân (thiết kế cho điện thoại từ 360 px), phân hệ cán bộ và phân hệ quản trị. Hình dưới là màn hình danh mục thủ tục trên điện thoại. Các màn hình khác ở mục 4.4.

![Giao diện danh mục thủ tục trên điện thoại (360 px)](docs/report/img/01-danh-muc.png){w=6}

## 1.6 Quy trình phát triển và tổ chức nhóm

**Tổ chức nhóm.** Nhóm có 5 thành viên với 5 vai trò: PM kiêm BA, Tech Lead, Backend kiêm CSDL, Frontend kiêm UI/UX, QA kiêm DevOps (bảng phân công ở phần đầu). Project Plan dự trù 325 giờ, 65 giờ mỗi người.

**Mô hình quy trình.** Nhóm dùng mô hình lặp theo mốc, gồm 6 mốc từ M0 đến M5 và 2 sprint xây dựng.

Bảng: Các mốc của dự án

| Mốc | Nội dung | Sản phẩm chính |
|---|---|---|
| M0 | Khởi động | Project Plan, slide |
| M1 | Yêu cầu | Báo cáo khảo sát, SRS v1.1, 17 use case, review SRS, RTM v1.0 |
| M2 | Thiết kế | SDD (IEEE 1016), 12 sơ đồ UML, ERD, OpenAPI 3, prototype, Test Plan, quyết định kho tệp |
| M3 | Sprint 1 | Tài khoản, nộp hồ sơ, tra cứu, thông báo, nhật ký |
| M4 | Sprint 2 | Xử lý hồ sơ, theo dõi hạn, dashboard, quản trị |
| M5 | Kết thúc | Báo cáo kiểm thử, RTM v1.2, hướng dẫn sử dụng, video demo, báo cáo cuối kỳ |

**Thay đổi lịch.** Có hai yêu cầu thay đổi được duyệt:

- **CR-01** (28/10/2026) đồng bộ lịch trong SRS với Project Plan.
- **CR-02** (04/11/2026, QD-11) rút ngày kết thúc từ 06/12/2026 xuống 10/11/2026 và giữ nguyên phạm vi. Vì vậy Sprint 1 và Sprint 2 được gộp làm một đợt ngày 04–05/11/2026.

**Quy trình kỹ thuật.**

- Mã nguồn nằm trên GitHub (`cachuabi/e-hoso`). Mỗi việc làm trên một nhánh riêng và vào `main` qua pull request có squash merge. Nhánh `main` được bảo vệ: phải qua PR, có Code Owners duyệt theo vai và CI xanh.
- Mọi kiểm tra chạy qua một cổng chất lượng duy nhất `./scripts/check.sh`: test công cụ, kiểm tra RTM, `mvnw verify` (Checkstyle, JUnit, JaCoCo), rồi lint, format, test và build frontend.
- Mỗi test ghi mã yêu cầu nó kiểm chứng (JUnit `@Tag("BR-03")`, Vitest `it('[FR-12] ...')`), nên RTM được sinh tự động.
- Tài liệu được duyệt theo 3 tầng: tự kiểm, review chéo theo phiếu RV-xx, PM duyệt mốc. Mọi quyết định ghi vào sổ quyết định QD-xx.

**Chế độ làm việc từ 28/10/2026.** Do các thành viên bận công việc, PM giao cho trợ lý AI (Claude) thực hiện công việc theo vai của từng thành viên, theo đúng quy trình trên. PM duyệt thay những người duyệt được chỉ định. Chi tiết và mức độ dùng AI ở Phụ lục C.
