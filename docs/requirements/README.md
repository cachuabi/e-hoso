# Yêu cầu và truy vết

| File | Vai trò |
|---|---|
| `eHoSo_SRS_v1.1.pdf` | **Đặc tả yêu cầu hiện hành** (ISO/IEC/IEEE 29148): áp dụng CR-01 (lịch theo Project Plan) và các phát hiện review; nội dung 47 yêu cầu giữ nguyên như v1.0 |
| `eHoSo_SRS_v1.0.pdf` | Bản gốc, giữ để đối chiếu (tag `srs-v1.0`) |
| `requirements.csv` | Danh mục máy đọc được: 28 FR, 10 NFR, 9 BR. **Nguồn sự thật** cho issue, RTM và SRS v1.1 |
| `eHoSo_KhaoSat_v1.0.pdf` | Báo cáo khảo sát: nguồn KS-01 → KS-14 của các yêu cầu |
| `eHoSo_UseCase_v1.0.pdf` | Sơ đồ và đặc tả đủ 17 use case |
| `eHoSo_RTM_v1.0.pdf`, `.csv` | RTM ở mốc M1: yêu cầu ↔ nguồn ↔ use case. Từ M3, phần test được sinh tự động (bên dưới) |
| `eHoSo_SRS_Review_v1.0.pdf` | Báo cáo review SRS v1.0 (10 phát hiện) và quyết định CR-01 |

## Ma trận truy vết (RTM) được sinh tự động

Không ai phải cập nhật RTM bằng tay. Test ghi mã yêu cầu mà nó kiểm chứng:

```java
@Test
@Tag("BR-03")
void tiepNhanSau15hThiHanLa17hNgayLamViecKeTiep() { ... }
```

```ts
it('[FR-12] không trả về thông tin cá nhân khi tra cứu', () => { ... });
```

`python3 tools/req.py rtm` quét test backend, frontend và `tests/` rồi in độ phủ. Trên mỗi PR, CI ghi bảng RTM đầy đủ vào phần Summary của job.

**Cổng chặn:** biến `RTM_ENFORCE_SPRINTS` trong `Makefile` liệt kê các sprint mà mọi yêu cầu mức **M** đều phải có test. Khi đến mốc:
- M3 (hết Sprint 1): đặt `S1`.
- M4 (hết Sprint 2): đặt `S1,S2`.
- M5: đặt `S1,S2,ALL`, khớp tiêu chí nghiệm thu số 4 của SRS.

CI cũng báo đỏ nếu test ghi một mã không có trong danh mục (gõ nhầm `FR-99`).

## Thay đổi yêu cầu

1. Sửa `requirements.csv` trong một PR, ghi lý do trong mô tả PR (quy trình yêu cầu thay đổi do PM duyệt).
2. Nếu thêm FR/NFR mới, chạy `python3 tools/req.py issues --repo cachuabi/e-hoso` để tạo issue tương ứng.
3. Chỉ số "Biến động yêu cầu" so `requirements.csv` hiện tại với tag `srs-v1.0`. Chỉ tính thay đổi ở **nội dung** (`statement`) và **mức ưu tiên**. Sửa siêu dữ liệu (tên ngắn, nguồn, sprint, mốc) không tính là thay đổi yêu cầu.
