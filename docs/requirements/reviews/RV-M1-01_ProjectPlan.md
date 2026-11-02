# Phiếu review RV-M1-01

| Mục | Nội dung |
|---|---|
| Sản phẩm | D01 – ProjectPlan.pdf (8 trang, tạo 28/10/2026 13:17) |
| Tác giả (vai) | Đỗ Văn Tiến (PM) |
| Người review (vai) | Mai Xuân Đạt (QA), đại diện nhóm – review do Claude thực hiện theo vai |
| Ngày | 28/10/2026 |
| Căn cứ | Đề bài P1ProjectPlan.pdf; HUONG_DAN mục 6; chạy lại `Drafts/build_projectplan.py` (thư mục tài liệu nhóm) |

## Checklist
| # | Tiêu chí | Đạt | Ghi chú |
|---|---|---|---|
| 1 | Đủ mục theo P1: Topic Name, Document Overview, Management Plan | Có | Mục 1, 2, 3 |
| 2 | Management Plan có: cơ cấu nhóm, team charter, mốc và sản phẩm, quy trình kỹ thuật (version control, môi trường, chuẩn), chỉ số (mục đích, cách thu thập, trách nhiệm) | Có | Mục 3.1 → 3.5 |
| 3 | Bảng công việc đủ 8 cột theo P1 | Có | Mục 3.6 |
| 4 | Có phân tích rủi ro | Có | Mục 4, R1–R8 |
| 5 | Tổng 28 công việc, 325 giờ, mỗi người 65 giờ | Có | Chạy lại script 28/10: tasks=28, total=325, mỗi người 65,0 |
| 6 | Nội dung khớp slide bản chốt (Group1) | Một phần | Các điểm suy ra ở phát hiện PP-01 |

## Phát hiện
| Mã | Mức | Vị trí | Vấn đề | Hướng sửa | Xử lý |
|---|---|---|---|---|---|
| PP-01 | major | Mục 3.6, 4 | 4 điểm Claude suy ra mà slide không ghi: (a) giờ từng công việc; (b) % hoàn thành tới 28/10; (c) K/T và biện pháp của R5–R8; (d) cột Kiểm thử (UAT) của Tiến | PM xác nhận hoặc sửa | Chờ PM |
| PP-02 | major | Hạn nộp | P1 hạn 15:59 ngày 27/10/2026; bản PDF này tạo 28/10/2026 | PM xác nhận bản đã nộp ngày 27/10 và việc có nộp bổ sung bản này không | Chờ PM |
| PP-03 | major | Meetings Repository | P1 yêu cầu "the appropriate meetings document"; D03 (biên bản họp khởi động) chưa có | PM cung cấp thông tin thật của buổi họp khởi động hoặc chuẩn bị (ngày, hình thức, người dự, nội dung, quyết định) để lập biên bản. Không có họp thì không lập | Chờ PM |

## Kết luận
**Đạt có điều kiện.** Nội dung đúng cấu trúc đề bài và đúng số liệu. Điều kiện: PM xử lý PP-01 → PP-03.
