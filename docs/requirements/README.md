# Yêu cầu và truy vết

| File | Vai trò |
|---|---|
| `eHoSo_SRS_v1.0.pdf` | Đặc tả yêu cầu gốc (ISO/IEC/IEEE 29148) |
| `requirements.csv` | Danh mục máy đọc được: 28 FR, 10 NFR, 9 BR. **Nguồn sự thật** cho issue và RTM |

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
3. Chỉ số "Biến động yêu cầu" so `requirements.csv` hiện tại với tag `srs-v1.0` (gắn khi chốt SRS v1.0).

## Còn thiếu so với SRS
SRS mục 1.4 nhắc tới `eHoSo_KhaoSat_v1.0.md` (báo cáo khảo sát, mã KS-xx) và `eHoSo_RTM_v1.0.csv`. Hãy thêm báo cáo khảo sát vào thư mục này. RTM nay được sinh tự động, nên không cần file CSV riêng nữa.
