# Tài Liệu Kỹ Thuật Hệ Thống Bán Lẻ & Dịch Vụ Máy Tính — HK COMPUTER

Chào mừng đến với bộ tài liệu kỹ thuật của **HK COMPUTER** — nền tảng thương mại điện tử và quản lý dịch vụ hậu mãi chuyên biệt cho cửa hàng Laptop, PC, Màn hình và Phụ kiện công nghệ theo mô hình **Đa cơ sở (Multi-Branch Retail)**.

---

## 1. Giới thiệu dự án

**HK COMPUTER** giải quyết trọn vẹn vòng đời mua sắm và chăm sóc thiết bị công nghệ của khách hàng trên một nền tảng duy nhất:

- **Mua sắm đa kênh & Tồn kho 3 chi nhánh trọng điểm:** Tra cứu sản phẩm theo nhóm ngành hàng (`LAPTOP`, `PC`, `ACCESSORY`), lọc tồn kho thực tế theo từng tỉnh/thành phố và cơ sở cửa hàng (`Hà Nội`, `TP.HCM`, `Đà Nẵng`), hỗ trợ cả **Giao tận nơi (`HOME_DELIVERY`)** lẫn **Nhận tại cửa hàng (`STORE_PICKUP`)**.
- **Thanh toán tự động thời gian thực:** Tích hợp tạo mã **VietQR qua SePay** (tự động đối soát giao dịch qua Webhook và đẩy kết quả tức thì xuống trình duyệt qua WebSocket) song song với cổng **VNPay** và **COD**.
- **Trợ lý AI tư vấn bán hàng (Hybrid Chatbot):** Kết hợp mô hình ngôn ngữ lớn trên **Groq AI (`qwen/qwen3.8-27b`)** có chuỗi model dự phòng tự động cùng bộ phân tích sản phẩm/giá/chi nhánh `ChatbotCatalogMatcher` và **3 lớp rào chắn nội dung (Guardrails)**.
- **Hậu mãi số hóa:** Tính thẻ bảo hành từ đơn đã thanh toán với serial theo từng thiết bị (`BH-{orderId}-{productId}-{unitNumber}`), nhận yêu cầu bảo hành trực tuyến và **đặt lịch sửa chữa** tại cửa hàng (`AT_STORE`) hoặc tận nơi (`AT_HOME`).

---

## 2. Công nghệ sử dụng (Tech Stack)

| Tầng | Công nghệ | Vai trò trong dự án |
| :--- | :--- | :--- |
| **Backend** | Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA, MapStruct | RESTful API chuẩn 3 tầng (`Controller -> Service -> Repository`), phân quyền RBAC (`USER`, `ADMIN`), gói dùng chung `common/` tinh gọn |
| **Database** | PostgreSQL 16 + Flyway Migration (`V1` → `V20`) | Lưu trữ 15 bảng nghiệp vụ đang hoạt động (đã tinh gọn Voucher & module Support trùng lặp, chuẩn 3 chi nhánh trọng điểm) |
| **Cache & OTP** | Redis 7 (Upstash TLS / Docker Redis) | Lưu mã OTP xác thực email (TTL 5 phút), đồng bộ giỏ hàng (`cart:{accountId}`), Rate Limit & bộ nhớ hội thoại Chatbot |
| **Real-time** | Spring WebSocket (STOMP + SockJS) | Đẩy thông báo thanh toán SePay tức thì (`/topic/payment/{orderId}`) xuống trình duyệt khách hàng |
| **AI & Tích hợp ngoài** | Groq OpenAI-compatible API, SePay Webhook/VietQR, VNPay, Brevo HTTP API (`BrevoEmailClient`), Google OAuth2 | Chatbot tư vấn, thanh toán tự động, gửi email OTP & biên nhận bảo hành, đăng nhập Google |
| **Frontend** | HTML5, CSS3 Design Tokens, Vanilla ES Modules (`FE_Computer`) | Giao diện khách hàng & trang quản trị Paper Dashboard, nhẹ, không phụ thuộc bundler |
| **Hạ tầng Cloud** | Vercel (`vankhuongdev.id.vn`), Render Docker, Neon PostgreSQL, Upstash Redis | Triển khai CI/CD tự động từ nhánh `dev` trên GitHub; môi trường Local gói gọn trong 1 file `docker-compose.yml` |

---

## 3. Danh mục tài liệu trong thư mục `doc/`

### Kiến trúc & Dữ liệu nền tảng
1. [01-kien-truc-he-thong.md](./01-kien-truc-he-thong.md) — Kiến trúc tổng thể, phân tách gói `common/`, `config/DemoDataSeeder` và 4 phân hệ (`AUTH`, `PRODUCT`, `ORDER`, `REVIEW`), bảo mật, danh mục API và WebSocket.
2. [02-co-so-du-lieu.md](./02-co-so-du-lieu.md) — Sơ đồ ER tổng thể và mô tả chi tiết **15 bảng dữ liệu nghiệp vụ** cùng các chỉ mục tối ưu (`V1` → `V20`).

### Các luồng hoạt động chính (`doc/flows/`)
1. [flows/01-luong-xac-thuc-tai-khoan.md](./flows/01-luong-xac-thuc-tai-khoan.md) — Đăng ký, OTP qua Redis & `BrevoEmailClient`, Đăng nhập JWT + HttpOnly Refresh Cookie, Đăng nhập Google OAuth2, Quên mật khẩu.
2. [flows/02-luong-gio-hang-dat-hang-kho-da-co-so.md](./flows/02-luong-gio-hang-dat-hang-kho-da-co-so.md) — Giỏ hàng đồng bộ PostgreSQL + Redis, thuật toán phân bổ tồn kho 3 chi nhánh trọng điểm, hủy đơn treo >30 phút & hoàn kho tự động.
3. [flows/03-luong-thanh-toan-sepay-vnpay.md](./flows/03-luong-thanh-toan-sepay-vnpay.md) — Tạo mã VietQR SePay, xử lý Webhook tự động chống trùng giao dịch, đẩy kết quả qua WebSocket và xác thực chữ ký VNPay HMAC-SHA512.
4. [flows/04-luong-ai-chatbot-hybrid.md](./flows/04-luong-ai-chatbot-hybrid.md) — Kiến trúc Hybrid Chatbot (`ChatbotCatalogMatcher` + Groq AI đa model dự phòng + 3 lớp Guardrail bảo vệ thương hiệu).
5. [flows/05-luong-bao-hanh-dat-lich-sua-chua.md](./flows/05-luong-bao-hanh-dat-lich-sua-chua.md) — Thẻ bảo hành theo danh mục (12/24/36 tháng), quy trình xử lý phiếu bảo hành, đặt lịch sửa chữa (`AT_STORE` / `AT_HOME`) và đánh giá sản phẩm xác thực mua hàng.

---

## 4. Tiến độ dự án hiện tại

| Phân hệ | Hạng mục chức năng | Trạng thái | Ghi chú kỹ thuật |
| :--- | :--- | :---: | :--- |
| **Kiến trúc & Kiểm thử** | Ranh giới module qua Service Contract, chuẩn `*ServiceImpl`, `BusinessException` vs `500` | Hoàn thành | 0 cảnh báo MapStruct, `GlobalExceptionHandler` phân tách chuẩn `400`/`401`/`403`/`500` (`log.error`), test tự động BE & FE |
| **Tài khoản & Bảo mật** | Đăng ký, OTP Email (Brevo + Redis), JWT + Refresh Cookie, Google OAuth2 | Hoàn thành | Mật khẩu BCrypt, kiểm tra SĐT chuẩn 10 số `0xxxxxxxxx`, sổ địa chỉ cá nhân chống IDOR |
| **Sản phẩm & Kho** | Quản lý Thương hiệu, Danh mục (`LAPTOP`/`PC`/`ACCESSORY`), Sản phẩm, Kho 3 chi nhánh | Hoàn thành | Ràng buộc `UNIQUE(product_id, branch_id)`, nhập/điều chỉnh/luân chuyển kho nội bộ |
| **Đơn hàng & Giỏ hàng** | Giỏ hàng (PostgreSQL + Redis), Đặt hàng `HOME_DELIVERY` & `STORE_PICKUP` | Hoàn thành | Tự động phân bổ kho 3 chi nhánh, tự dọn giỏ hàng khi đặt đơn & hủy đơn treo >30 phút |
| **Thanh toán** | VietQR SePay + Webhook tự động + WebSocket real-time & VNPay Sandbox | Hoàn thành | Idempotency `SEPAY_{id}`, chữ ký HMAC-SHA512, gửi Email biên nhận + Serial bảo hành |
| **AI & Hậu mãi** | AI Chatbot Hybrid (`ChatbotServiceImpl` + `ChatbotCatalogMatcher` + `ChatbotGuardrail`) | Hoàn thành | 12/12 unit test Chatbot pass, tồn kho theo thành phố, bộ nhớ hội thoại Redis, Quick Chips |
| **AI & Hậu mãi** | Bảo hành điện tử, Đặt lịch sửa chữa (`AT_STORE` / `AT_HOME`) & Đánh giá sản phẩm | Hoàn thành | Thanh tiến trình thời hạn bảo hành, Timeline 4 bước cho Khách & bộ lọc/mẫu ghi chú KTV cho Admin |
| **Quản trị BI & UI/UX** | Cổng Admin **Creative Tim Paper Dashboard** + So sánh Laptop/PC + Mobile Bottom Nav | Hoàn thành | API `GET /api/admin/dashboard/summary`, chuẩn tách lớp 3 tầng, mượt từ `360px`+ |

---

## 5. Phương hướng phát triển tiếp theo (Roadmap)

### Ngắn hạn (1 – 2 tháng)
1. **Quản lý Seri phần cứng thực tế (IMEI / Service Tag):** Gắn danh sách mã Serial nhà sản xuất cụ thể cho từng chiếc Laptop/Linh kiện khi xuất kho song song với mã bảo hành điện tử.
2. **Upload ảnh đám mây (Cloudinary / S3):** Cho phép khách hàng đính kèm hình ảnh lỗi thiết bị khi tạo phiếu yêu cầu bảo hành hoặc đặt lịch sửa chữa, và Admin tải ảnh sản phẩm trực tiếp.

### Trung & Dài hạn (3 – 6 tháng)
1. **Tích hợp đơn vị vận chuyển (GHN / GHTK):** Tự động tính phí vận chuyển theo khoảng cách từ chi nhánh xuất kho tới địa chỉ khách hàng và đồng bộ mã vận đơn theo thời gian thực.
2. **Gợi ý cấu hình Build PC thông minh:** Tự động kiểm tra tính tương thích linh kiện (CPU – Mainboard – RAM – Nguồn – Vỏ Case) khi khách hàng tự chọn cấu hình máy bàn.
