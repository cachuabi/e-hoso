# Chương 5. Kết luận (Conclusion)

## 5.1 Kết quả đạt được

- **Phạm vi.** Đủ 28/28 yêu cầu chức năng, 17 use case và 3 thủ tục. Phạm vi giữ nguyên dù hạn dự án bị rút gần 4 tuần (CR-02). Biến động yêu cầu tính theo nội dung là 0%.
- **Chất lượng.** 5/5 tiêu chí nghiệm thu MVP đạt trên môi trường tự động. 47/47 yêu cầu có test tham chiếu. Độ phủ tầng service 93,4%, không còn lỗi Critical.
- **Vùng dễ vỡ** đều có lớp bảo vệ riêng:
  - ngày hẹn trả có 17 ca kiểm thử;
  - tra cứu công khai dùng kiểu dữ liệu riêng chỉ 4 trường;
  - nhật ký bị CSDL từ chối sửa, xoá;
  - mọi chuyển trạng thái đi qua một điểm duy nhất.
- **Triển khai.** Hệ thống chạy bằng một lệnh trên cả x86 và arm64, dùng khoảng 440 MiB RAM sau luồng kiểm thử.
- **Tài liệu.** Bộ tài liệu đi từ khảo sát đến kiểm thử theo các chuẩn ISO/IEC/IEEE 29148, IEEE 1016, ISO/IEC/IEEE 29119-3, có truy vết hai chiều.

## 5.2 Hạn chế

1. Chưa kiểm thử với người dùng thật (CK-U02) và chưa khảo sát tại một UBND xã cụ thể. Tính dễ dùng mới được kiểm bằng công cụ.
2. Hiệu năng đo trên runner GitHub với dữ liệu nhỏ, chỉ thao tác đọc; chưa khẳng định NFR-01 trên đúng cấu hình 2 vCPU, 4 GB.
3. Bảo mật mới dừng ở quét thụ động và đối chiếu thiết kế; thư viện Java chưa được quét bằng công cụ.
4. Giới hạn tần suất tra cứu lưu trong bộ nhớ nên mất khi khởi động lại, và chỉ đếm theo địa chỉ. Lọc nhãn hạn trong bộ nhớ chỉ hợp với quy mô MVP.
5. Chưa có sao lưu tự động. Chứng chỉ HTTPS là chứng chỉ nội bộ.
6. Hai sprint bị gộp nên thiếu hai vòng phản hồi giữa kỳ. Các thành viên ít tham gia trực tiếp vào việc viết mã (Phụ lục C).
7. Thời hạn thủ tục xác nhận tình trạng hôn nhân chưa được đối chiếu lại với văn bản hiện hành (OI-02).

## 5.3 Bài học

- **Truy vết bằng công cụ giữ được kỷ luật, nhưng chưa đủ.** Một tag trên test không có nghĩa là test thật sự kiểm đúng nội dung yêu cầu. Lượt đối chiếu cuối phát hiện 11 ca của Test Plan còn thiếu khẳng định, và đã bổ sung.
- **Chạy đầu cuối sớm.** Các lỗi cấu hình (Caddy, SNI, CSP) chỉ lộ ra khi chạy đầu cuối trên hạ tầng thật.
- **Thời gian phải tiêm được.** JWT từng so hạn theo giờ hệ thống thay vì đồng hồ của ứng dụng, làm bộ test đỏ tuỳ theo ngày chạy. Mọi nơi dùng thời gian cần nhận đồng hồ qua tham số.
- **Đặt ràng buộc ở tầng thấp nhất có thể.** Các `CHECK` liên cột và trigger chỉ cho thêm nhật ký vẫn giữ đúng quy tắc khi mã ứng dụng có lỗi.
- **Quản lý thay đổi.** Ghi mọi quyết định vào sổ quyết định và qua yêu cầu thay đổi giúp rút lịch mà không làm phình phạm vi.
- **AI tăng tốc, nhưng trách nhiệm vẫn thuộc về nhóm.** Nhóm phải hiểu và bảo vệ được sản phẩm; số liệu phải có nguồn kiểm chứng.

## 5.4 Hướng phát triển

- Kết nối Cổng Dịch vụ công quốc gia và VNeID; ký số thật thay ký mô phỏng.
- Thêm thủ tục bằng cấu hình biểu mẫu; xử lý hồ sơ chờ bổ sung quá hạn (OI-01).
- Đưa giới hạn tần suất vào lưu trữ chung; sao lưu định kỳ; chứng chỉ HTTPS hợp lệ.
- Quét phụ thuộc Java trong CI; kiểm thử xâm nhập; đo hiệu năng trên đúng cấu hình với dữ liệu lớn.
- Kiểm thử khả dụng với người dùng thật, phỏng vấn cán bộ một cửa.
- Mở rộng cho nhiều xã: phân vùng dữ liệu theo đơn vị hành chính.

# Tài liệu tham khảo

1. Quốc hội (2014). *Luật Hộ tịch*, Luật số 60/2014/QH13.
2. Chính phủ (2015). *Nghị định 123/2015/NĐ-CP quy định chi tiết một số điều và biện pháp thi hành Luật Hộ tịch*.
3. Chính phủ (2015). *Nghị định 23/2015/NĐ-CP về cấp bản sao từ sổ gốc, chứng thực bản sao từ bản chính, chứng thực chữ ký và chứng thực hợp đồng, giao dịch*.
4. Chính phủ (2025). *Nghị định 118/2025/NĐ-CP về thực hiện thủ tục hành chính theo cơ chế một cửa, một cửa liên thông*.
5. ISO/IEC/IEEE 29148:2018. *Systems and software engineering – Life cycle processes – Requirements engineering*.
6. IEEE 1016-2009. *IEEE Standard for Information Technology – Systems Design – Software Design Descriptions*.
7. ISO/IEC/IEEE 29119-3:2021. *Software and systems engineering – Software testing – Part 3: Test documentation*.
8. OWASP Foundation (2021). *OWASP Top 10*.
9. W3C (2018). *Web Content Accessibility Guidelines (WCAG) 2.1*.
10. Nhóm 1 (2026). *e-HoSo: Project Plan; Báo cáo khảo sát v1.1; SRS v1.2; Use case v1.2; SDD v1.2; Test Plan v1.0; Báo cáo kiểm thử v1.0; RTM v1.2; ADR-001*. Kho `github.com/cachuabi/e-hoso`.
