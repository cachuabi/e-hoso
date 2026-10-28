# e-HoSo

Hệ thống tiếp nhận, xử lý và theo dõi hồ sơ thủ tục hành chính trực tuyến cấp xã/phường. Đây là Final Project môn INS6021 – Phát triển phần mềm (Trường Quốc tế – ĐHQGHN), Nhóm 1.

> Toàn bộ dữ liệu trong dự án là dữ liệu giả lập.

## Chạy thử

Yêu cầu: Java 21, Node.js 20+, Python 3.10+, Docker.

```bash
docker compose up -d                                 # PostgreSQL + Mailpit (http://localhost:8025)
cd src/backend && ./mvnw spring-boot:run             # API: http://localhost:8080, OpenAPI: /swagger-ui.html
cd src/frontend && npm install && npm run dev        # Web: http://localhost:5173
```

## Kiểm tra chất lượng

```bash
./scripts/check.sh      # = make check: tool tests, RTM, Checkstyle + test + JaCoCo, ESLint + Prettier + Vitest + build
```

## Cấu trúc

```
src/backend     Spring Boot 3.5 (Java 21)
src/frontend    React 19 + TypeScript (Vite)
docs/           Tài liệu: project, requirements, design, test
meetings/       Biên bản họp
tools/          RTM (req.py) và báo cáo chỉ số tuần (metrics.py)
```

## Quy trình tự động

| Bước | Tự động hoá |
|---|---|
| Yêu cầu → công việc | Mỗi FR/NFR trong `docs/requirements/requirements.csv` là một issue có nhãn MoSCoW, sprint và mốc |
| Mã nguồn → kiểm tra | CI chạy `make check` trên mỗi PR; `main` chỉ nhận PR có CI xanh và 1 người duyệt |
| Yêu cầu → test (RTM) | `tools/req.py rtm` dựng ma trận truy vết từ mã yêu cầu ghi trong test và chặn khi thiếu |
| Tiêu đề PR | Kiểm tra Conventional Commits |
| Chỉ số dự án | Mỗi Chủ nhật 20:05, bot tạo issue "Báo cáo chỉ số tuần" với 8 chỉ số trong Project Plan |
| Làm cùng AI | Claude Code với SDLC kit: `/task`, `/review-diff`, hook chặn lệnh nguy hiểm và chặn dừng khi check đỏ |

Xem [CONTRIBUTING.md](CONTRIBUTING.md) cho quy trình nhánh và review.
