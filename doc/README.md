# Tài Liệu Kỹ Thuật Hệ Thống Bán Lẻ & Dịch Vụ Máy Tính — HK COMPUTER

Chào mừng đến với bộ tài liệu kỹ thuật của **HK COMPUTER** — nền tảng thương mại điện tử và quản lý dịch vụ hậu mãi chuyên biệt cho cửa hàng Laptop, PC, Màn hình và Phụ kiện công nghệ theo mô hình **Đa cơ sở (Multi-Branch Retail)**.

---

## 1. Giới thiệu dự án

**HK COMPUTER** giải quyết trọn vẹn vòng đời mua sắm và chăm sóc thiết bị công nghệ của khách hàng trên một nền tảng duy nhất:

- **Mua sắm đa kênh & Tồn kho đa cơ sở:** Tra cứu sản phẩm theo nhóm ngành hàng (`LAPTOP`, `PC`, `ACCESSORY`), lọc tồn kho thực tế theo từng tỉnh/thành phố và cơ sở cửa hàng, hỗ trợ cả **Giao tận nơi (`HOME_DELIVERY`)** lẫn **Nhận tại cửa hàng (`STORE_PICKUP`)**.
- **Thanh toán tự động thời gian thực:** Tích hợp tạo mã **VietQR qua SePay** (tự động đối soát giao dịch qua Webhook và đẩy kết quả tức thì xuống trình duyệt qua WebSocket) song song với cổng **VNPay** và **COD**.
- **Trợ lý AI tư vấn bán hàng (Hybrid Chatbot):** Kết hợp mô hình ngôn ngữ lớn trên **Groq AI (`qwen/qwen3.8-27b`)** có chuỗi model dự phòng tự động cùng bộ truy vấn CSDL nội bộ và **3 lớp rào chắn nội dung (Guardrails)**.
- **Hậu mãi số hóa:** Tính thẻ bảo hành từ đơn đã thanh toán với serial theo từng thiết bị (`BH-{orderId}-{productId}-{unitNumber}`), nhận yêu cầu bảo hành trực tuyến và **đặt yêu cầu lịch sửa chữa** tại cửa hàng hoặc tại nhà.

---

## 2. Công nghệ sử dụng (Tech Stack)

| Tầng | Công nghệ | Vai trò trong dự án |
| :--- | :--- | :--- |
| **Backend** | Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA | RESTful API, phân quyền RBAC (`USER`, `ADMIN`), xử lý nghiệp vụ |
| **Database** | PostgreSQL 16 + Flyway Migration (`V1` → `V20`) | Lưu trữ 16 bảng dữ liệu quan hệ (đã tinh gọn, gỡ bảng Voucher, chuẩn 3 chi nhánh trọng điểm) |
| **Cache & OTP** | Redis (Upstash TLS / Docker Redis) | Lưu mã OTP xác thực email (TTL 5 phút) và đồng bộ nhanh giỏ hàng (`cart:{accountId}`) |
| **Real-time** | Spring WebSocket (STOMP + SockJS) | Đẩy thông báo thanh toán SePay (`/topic/payment/{orderId}`) và biến động tồn kho (`/topic/inventory/{id}`) |
| **AI & Tích hợp ngoài** | Groq OpenAI-compatible API, SePay Webhook/VietQR, VNPay, Brevo SMTP API, Google OAuth2 | Chatbot tư vấn, thanh toán tự động, gửi email OTP, đăng nhập Google |
| **Frontend** | HTML5, CSS3 Design Tokens, Vanilla ES Modules (`FE_Computer`) | Giao diện khách hàng & trang quản trị Admin, nhẹ, không phụ thuộc bundler |
| **Hạ tầng Cloud** | Vercel (`vankhuongdev.id.vn`), Render Docker, Neon PostgreSQL, Upstash Redis | Triển khai CI/CD tự động từ nhánh `dev` trên GitHub |

---

## 3. Danh mục tài liệu trong thư mục `doc/`

### Kiến trúc & Dữ liệu nền tảng
1. [01-kien-truc-he-thong.md](./01-kien-truc-he-thong.md) — Kiến trúc tổng thể, phân tách module (`AUTH`, `PRODUCT`, `ORDER`, `REVIEW`), bảo mật, danh mục API và WebSocket.
2. [02-co-so-du-lieu.md](./02-co-so-du-lieu.md) — Sơ đồ ER tổng thể và mô tả chi tiết toàn bộ **17 bảng dữ liệu** cùng các chỉ mục tối ưu (`V1` → `V19`).

### Các luồng hoạt động chính (`doc/flows/`)
1. [flows/01-luong-xac-thuc-tai-khoan.md](./flows/01-luong-xac-thuc-tai-khoan.md) — Đăng ký, OTP qua Redis & Brevo, Đăng nhập JWT + HttpOnly Refresh Cookie, Đăng nhập Google OAuth2, Quên mật khẩu.
2. [flows/02-luong-gio-hang-dat-hang-kho-da-co-so.md](./flows/02-luong-gio-hang-dat-hang-kho-da-co-so.md) — Giỏ hàng đồng bộ PostgreSQL + Redis, áp mã Voucher, thuật toán phân bổ tồn kho đa cơ sở, hủy đơn hoàn kho & hoàn lượt Voucher.
3. [flows/03-luong-thanh-toan-sepay-vnpay.md](./flows/03-luong-thanh-toan-sepay-vnpay.md) — Tạo mã VietQR SePay, xử lý Webhook tự động chống trùng giao dịch, đẩy kết quả qua WebSocket và xác thực chữ ký VNPay HMAC-SHA512.
4. [flows/04-luong-ai-chatbot-hybrid.md](./flows/04-luong-ai-chatbot-hybrid.md) — Kiến trúc Hybrid Chatbot (Tầng phân tích DB nội bộ + Tầng Groq AI đa model dự phòng + 3 lớp Guardrail bảo vệ thương hiệu).
5. [flows/05-luong-bao-hanh-dat-lich-sua-chua.md](./flows/05-luong-bao-hanh-dat-lich-sua-chua.md) — Thẻ bảo hành theo danh mục (12/24/36 tháng), quy trình xử lý phiếu bảo hành, đặt yêu cầu sửa chữa (`AT_STORE` / `AT_HOME`), đánh giá sản phẩm và phiếu hỗ trợ.

---

## 4. Tiến độ dự án hiện tại

| Phân hệ | Hạng mục chức năng | Trạng thái | Ghi chú kỹ thuật |
| :--- | :--- | :---: | :--- |
| **Tài khoản & Bảo mật** | Đăng ký, OTP Email (Brevo + Redis), JWT + Refresh Cookie | Hoàn thành | Mật khẩu BCrypt, kiểm tra SĐT chuẩn 10 số `0xxxxxxxxx` |
| **Tài khoản & Bảo mật** | Đăng nhập Google OAuth2 & Đặt/Đổi mật khẩu | Hoàn thành | Redirect động theo `app.frontend-url` |
| **Sản phẩm & Kho** | Quản lý Thương hiệu, Danh mục (`LAPTOP`/`PC`/`ACCESSORY`), Sản phẩm | Hoàn thành | Hỗ trợ lọc đa tiêu chí, khoảng giá, từ khóa |
| **Sản phẩm & Kho** | Quản lý Chi nhánh (`branches`) & Tồn kho Đa cơ sở + Chuyển kho | Hoàn thành | Ràng buộc `UNIQUE(product_id, branch_id)` + WebSocket tồn kho |
| **Đơn hàng & Giỏ hàng** | Giỏ hàng (PostgreSQL + Redis), Đặt hàng `HOME_DELIVERY` & `STORE_PICKUP` | Hoàn thành | Tự động phân bổ kho 3 chi nhánh, tự dọn giỏ hàng khi đặt đơn & hủy đơn treo >30 phút |
| **Thanh toán** | VietQR SePay + Webhook tự động + WebSocket real-time | Hoàn thành | Idempotency `SEPAY_{id}`, kiểm tra API Key & gửi Email biên nhận + Serial bảo hành |
| **Thanh toán** | Cổng thanh toán VNPay Sandbox (HMAC-SHA512) | Hoàn thành | Đối chiếu chữ ký số, tổng tiền đơn hàng & kích hoạt bảo hành điện tử |
| **AI & Hậu mãi** | AI Chatbot Hybrid (Groq `qwen/qwen3.8-27b` + Tồn kho theo chi nhánh + 3 lớp Guardrail) | Hoàn thành | 12/12 unit test pass, bộ nhớ hội thoại Redis, giao diện Quick Chips & Typing Indicator |
| **AI & Hậu mãi** | Bảo hành điện tử & Đặt lịch sửa chữa (`AT_STORE` / `AT_HOME`), Đánh giá, Hỗ trợ | Hoàn thành | Thanh tiến trình thời hạn bảo hành, Timeline 4 bước cho Khách & bộ lọc/mẫu ghi chú KTV cho Admin |
| **Quản trị BI & UI/UX** | Cổng Admin **Creative Tim Paper Dashboard** + So sánh Laptop/PC + Mobile Bottom Nav | Hoàn thành | API `GET /api/admin/dashboard/summary`, chuẩn tách lớp 3 tầng, mượt từ `360px`+ |
| **Triển khai Cloud** | CI/CD Render (Backend Docker) + Vercel (`vankhuongdev.id.vn`) + Neon + Upstash | Hoàn thành | Tối ưu cache Docker layer & Google Maven Central mirror |

---

## 5. Phương hướng phát triển tiếp theo (Roadmap)

### Ngắn hạn (1 – 2 tháng)
1. **Quản lý Seri phần cứng thực tế (IMEI / Service Tag):** Gắn danh sách mã Serial nhà sản xuất cụ thể cho từng chiếc Laptop/Linh kiện khi xuất kho song song với mã bảo hành điện tử.
2. **Upload ảnh đám mây (Cloudinary / S3):** Cho phép khách hàng đính kèm hình ảnh lỗi thiết bị khi tạo phiếu yêu cầu bảo hành hoặc đặt lịch sửa chữa, và Admin tải ảnh sản phẩm trực tiếp.

### Trung & Dài hạn (3 – 6 tháng)
1. **Tích hợp đơn vị vận chuyển (GHN / GHTK):** Tự động tính phí vận chuyển theo khoảng cách từ chi nhánh xuất kho tới địa chỉ khách hàng và đồng bộ mã vận đơn theo thời gian thực.
2. **Gợi ý cấu hình Build PC thông minh:** Tự động kiểm tra tính tương thích linh kiện (CPU – Mainboard – RAM – Nguồn – Vỏ Case) khi khách hàng tự chọn cấu hình máy bàn.
