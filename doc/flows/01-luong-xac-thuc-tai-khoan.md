# Luồng 01 — Xác Thực Tài Khoản, OTP Redis/Brevo, JWT & Google OAuth2

Tài liệu này mô tả chi tiết luồng đăng ký, xác thực OTP qua email, đăng nhập phát hành cặp Token (JWT + HttpOnly Refresh Cookie), tự động làm mới phiên và đăng nhập bằng Google OAuth2.

---

## 1. Các thành phần tham gia trong Code

- **Controller:** `AUTH/controller/AccountController.java`, `AUTH/controller/UseradressController.java`
- **Security & Filter:** `AUTH/security/SecurityConfig.java`, `JwtAuthenticationFilter.java`, `JwtService.java`, `OAuth2SuccessHandler.java`, `CustomOAuth2UserService.java`
- **Service & Common:** `AccountService.java` / `AccountServiceImpl.java` (xử lý toàn bộ nghiệp vụ đăng nhập, phát hành/làm mới/thu hồi RefreshToken, kiểm tra Rate Limit OTP 60s), `EmailVerificationService.java` (gửi mail qua `common/BrevoEmailClient.java`), `RedisOtpServiceImpl.java`, `RedisRateLimiterService.java`, `RefreshTokenService.java`, `UseraddressServiceImpl.java`, `common/PhoneValidator.java`
- **Frontend:** `dang-ky.html`, `xac-thuc-otp.html`, `dang-nhap.html`, `assets/js/auth.js`, `assets/js/api.js`

---

## 2. Luồng Đăng ký & Kích hoạt OTP qua Redis + Brevo

```mermaid
sequenceDiagram
    actor U as Khách hàng
    participant FE as Frontend (dang-ky / xac-thuc-otp)
    participant BE as AccountController / IAccountService
    participant RD as Redis (otp:{gmail})
    participant BR as Brevo SMTP API v3
    participant DB as PostgreSQL (account, refresh_tokens)

    U->>FE: Nhập Họ tên, Gmail, Mật khẩu (>= 8 ký tự)
    FE->>BE: POST /api/account/register
    BE->>DB: Kiểm tra trùng gmail -> Lưu Account (enabled = false, role = USER, BCrypt)
    BE-->>FE: 200 OK ("Create Account successs")

    FE->>BE: POST /api/account/send-code?gmail=...
    BE->>RD: Lưu mã OTP 6 chữ số (TTL = 5 phút)
    BE->>BR: POST https://api.brevo.com/v3/smtp/email
    alt Gửi mail lỗi
        BE->>RD: Xóa OTP vừa lưu
        BE-->>FE: 400 Bad Request
    else Gửi mail thành công
        BE-->>FE: 200 OK ("Đã gửi mã OTP")
    end

    U->>FE: Nhập mã OTP nhận từ Gmail
    FE->>BE: POST /api/account/check-code?gmail=...&code=...
    BE->>RD: Đối chiếu otp:{gmail} -> Xóa OTP sau khi khớp
    BE->>DB: Cập nhật account.enabled = true & Tạo RefreshToken (7 ngày)
    BE-->>FE: Set-Cookie: refreshToken (HttpOnly, Secure, SameSite=None) + AccountResponse
    FE->>BE: POST /api/account/refresh-token (kèm Cookie)
    BE-->>FE: Trả accessToken (JWT 15 phút) -> Đăng nhập hoàn tất
```

---

## 3. Luồng Đăng nhập Thường, Làm mới JWT tự động & Đăng xuất

```mermaid
sequenceDiagram
    participant FE as Frontend (api.js / auth.js)
    participant FLT as JwtAuthenticationFilter
    participant BE as AccountController
    participant DB as PostgreSQL

    FE->>BE: POST /api/account/login { gmail, password }
    BE->>DB: Xác thực BCrypt & enabled == true -> Xóa RefreshToken cũ, tạo UUID mới
    BE-->>FE: Set-Cookie: refreshToken (7 ngày) + thông tin tài khoản
    FE->>BE: POST /api/account/refresh-token (credentials: include)
    BE->>DB: Kiểm tra refreshToken chưa hết hạn & chưa bị thu hồi
    BE-->>FE: { accessToken: "<JWT 15m>" } -> Lưu vào sessionStorage

    Note over FE,FLT: Khi gọi các API cần quyền mà JWT hết hạn (HTTP 401)
    FE->>FLT: GET /api/orders/my-orders (Bearer JWT hết hạn)
    FLT-->>FE: 401 Unauthorized
    FE->>BE: Tự động xếp hàng request -> POST /api/account/refresh-token
    BE-->>FE: Cấp accessToken mới
    FE->>FLT: Tự động phát lại GET /api/orders/my-orders với JWT mới
    FLT-->>FE: 200 OK (Người dùng không bị văng phiên)
```

---

## 4. Luồng Đăng nhập Google OAuth2

1. Trình duyệt chuyển hướng tới `GET /oauth2/authorization/google`.
2. Sau khi người dùng xác thực với Google, `CustomOAuth2UserService` kiểm tra `gmail` trong bảng `account`:
   - Nếu chưa tồn tại: tự động khởi tạo tài khoản mới với `enabled = true`, `role = USER`, gắn `googleid`.
   - Nếu đã tồn tại: cập nhật `googleid` (nếu chưa có).
3. `OAuth2SuccessHandler` tạo `RefreshToken` (gắn vào cookie `HttpOnly`) và sinh `accessToken` JWT, sau đó redirect về `${app.frontend-url}/trangchu.html#accessToken=<JWT>`.
4. Đối với tài khoản tạo qua Google chưa có mật khẩu nội bộ (`password == null`), người dùng có thể đặt mật khẩu lần đầu qua `POST /api/account/set-gg-password` (kiểm tra bằng `GET /api/account/check-password`).

---

## 5. Bảo vệ Sổ địa chỉ nhận hàng (`Useraddresses`)

- Mỗi tài khoản được lưu tối đa **5 địa chỉ nhận hàng**.
- Trường `phone` bắt buộc đi qua `PhoneValidator.validateAndNormalize(...)` đảm bảo đúng **10 chữ số bắt đầu bằng `0`** (`^0\d{9}$`).
- Mọi thao tác cập nhật (`PUT /api/useraddresses/{id}/update`) và xóa (`DELETE /api/useraddresses/{id}/delete`) đều kiểm tra `findByIdAndAccountId(id, account.getId())` để chống truy cập chéo tài khoản (IDOR).
