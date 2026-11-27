# HK COMPUTER — Backend System (`BE_Computer`)

Hệ thống Backend thương mại điện tử & dịch vụ hậu mãi đa cơ sở cho cửa hàng Laptop, PC và Phụ kiện công nghệ **HK COMPUTER** (xây dựng bằng **Java 21 + Spring Boot 4.1 + PostgreSQL + Redis + WebSocket**).

- **Website triển khai thực tế:** `https://vankhuongdev.id.vn`
- **Tài liệu kỹ thuật chi tiết (`doc/`):** Xem đầy đủ tại **[doc/README.md](./doc/README.md)**
- **Hệ thống chi nhánh trọng điểm (3 cơ sở):** Hà Nội (Thái Hà) · TP.HCM (Quận 10) · Đà Nẵng (Hải Châu)
- **Tài khoản mẫu khởi tạo sẵn (Local / Test — điều khiển qua `app.seed.enabled`):**
  - Admin: `admin@hkcomputer.vn` / `Admin@123`
  - Khách hàng: `khachhang@hkcomputer.vn` / `User@123` (hoặc `khach@hkcomputer.vn` / `Khach@123`)

---

## Danh mục Tài liệu trong [`doc/`](./doc/)

- [Tổng quan dự án, Tiến độ & Định hướng phát triển](./doc/README.md)
- [01. Kiến trúc tổng thể hệ thống & Danh mục API](./doc/01-kien-truc-he-thong.md)
- [02. Thiết kế Cơ sở dữ liệu (15 bảng nghiệp vụ, Flyway V1–V20)](./doc/02-co-so-du-lieu.md)
- **Các luồng nghiệp vụ chính:**
  - [Luồng 01 — Xác thực, OTP Redis/Brevo, JWT & Google OAuth2](./doc/flows/01-luong-xac-thuc-tai-khoan.md)
  - [Luồng 02 — Giỏ hàng & Đặt hàng Kho 3 Chi Nhánh Trọng Điểm](./doc/flows/02-luong-gio-hang-dat-hang-kho-da-co-so.md)
  - [Luồng 03 — Thanh toán SePay VietQR (Webhook + WebSocket) & VNPay](./doc/flows/03-luong-thanh-toan-sepay-vnpay.md)
  - [Luồng 04 — AI Chatbot Hybrid (Groq + Catalog Matcher + 3-Layer Guardrails)](./doc/flows/04-luong-ai-chatbot-hybrid.md)
  - [Luồng 05 — Bảo hành điện tử, Đặt lịch sửa chữa & Đánh giá sản phẩm](./doc/flows/05-luong-bao-hanh-dat-lich-sua-chua.md)

---

## Khởi chạy nhanh ở máy Local

1. Bật PostgreSQL 16 và Redis 7 bằng một lệnh Docker duy nhất:
   ```bash
   docker compose up -d
   ```
2. Sao chép `.env.example` sang `.env` và điền các biến môi trường cần thiết.
3. Chạy ứng dụng:
   ```bash
   ./gradlew bootRun
   ```
