-- Dữ liệu khởi tạo: 3 thủ tục, thành phần hồ sơ, lịch nghỉ (FR-27, FR-28 thay bằng seed; SRS mục 3.6).
INSERT INTO procedure (code, name, deadline_unit, deadline_days, applies_15h_cutoff,
                       allows_verification, requires_original_check, result_validity_months,
                       documents_to_present, form_schema)
VALUES
('CHUNG_THUC', 'Chứng thực bản sao từ bản chính', 'SAME_DAY', NULL, true, false, true, NULL,
 'Bản chính giấy tờ để đối chiếu; giấy tờ tuỳ thân',
 '{"fields":[{"key":"hoTen","label":"Họ và tên người yêu cầu","type":"text","required":true},
 {"key":"tenGiayTo","label":"Tên giấy tờ cần chứng thực","type":"text","required":true},
 {"key":"soBan","label":"Số bản cần chứng thực","type":"number","required":true}]}'),
('KHAI_SINH', 'Đăng ký khai sinh', 'SAME_DAY', NULL, true, false, false, NULL,
 'Giấy tờ tuỳ thân của người đi đăng ký; bản chính giấy chứng sinh',
 '{"fields":[{"key":"hoTenTre","label":"Họ và tên trẻ","type":"text","required":true},
 {"key":"ngaySinh","label":"Ngày sinh","type":"date","required":true},
 {"key":"gioiTinh","label":"Giới tính","type":"select","options":["Nam","Nữ"],"required":true},
 {"key":"noiSinh","label":"Nơi sinh","type":"text","required":true},
 {"key":"hoTenMe","label":"Họ và tên mẹ","type":"text","required":true},
 {"key":"hoTenCha","label":"Họ và tên cha","type":"text","required":false}]}'),
('XAC_NHAN_HON_NHAN', 'Xác nhận tình trạng hôn nhân', 'WORKING_DAYS', 3, false, true, false, 6,
 'Giấy tờ tuỳ thân',
 '{"fields":[{"key":"hoTen","label":"Họ và tên người yêu cầu","type":"text","required":true},
 {"key":"ngaySinh","label":"Ngày sinh","type":"date","required":true},
 {"key":"tinhTrang","label":"Tình trạng hôn nhân","type":"select","options":["Chưa kết hôn","Đã ly hôn","Góa"],"required":true},
 {"key":"mucDich","label":"Mục đích sử dụng","type":"text","required":true}]}');

INSERT INTO dossier_component (procedure_id, name, requirement, sort_order)
SELECT p.id, c.name, c.requirement, c.sort_order
FROM procedure p
JOIN (VALUES
  ('CHUNG_THUC', 'Bản sao cần chứng thực', 'REQUIRED', 1),
  ('KHAI_SINH', 'Giấy chứng sinh', 'REQUIRED', 1),
  ('KHAI_SINH', 'Giấy chứng nhận kết hôn của cha mẹ', 'CONDITIONAL', 2),
  ('XAC_NHAN_HON_NHAN', 'Bản chụp giấy tờ tuỳ thân', 'REQUIRED', 1),
  ('XAC_NHAN_HON_NHAN', 'Giấy tờ chứng minh tình trạng hôn nhân trước đây', 'CONDITIONAL', 2)
) AS c (code, name, requirement, sort_order) ON c.code = p.code;

INSERT INTO holiday (day, type, description) VALUES
('2027-01-01', 'HOLIDAY', 'Tết Dương lịch (dữ liệu minh hoạ)');
