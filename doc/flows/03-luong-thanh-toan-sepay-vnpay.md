# Luồng 03 — Thanh Toán Trực Tuyến (SePay VietQR + WebSocket & VNPay)

Tài liệu này mô tả chi tiết hai luồng thanh toán điện tử tích hợp trong hệ thống: **Chuyển khoản ngân hàng tự động qua SePay (VietQR + Webhook + WebSocket Real-time)** và **Cổng thanh toán VNPay (HMAC-SHA512)**.

---

## 1. Các thành phần tham gia trong Code

- **Cấu hình:** `ORDER/config/SePayConfig.java`, `ORDER/config/VNPayConfig.java`, `PRODUCT/config/WebSocketConfig.java`
- **Controller:** `ORDER/controller/PaymentController.java`
- **Service & DTO:** `ORDER/service/impl/IPaymentService.java`, `SePayPaymentInfoResponse.java`, `SePayWebhookRequest.java`, `VerifyPaymentRequest.java`
- **Frontend:** `checkout.html`, `order.html` (hiển thị modal QR + kết nối SockJS/STOMP + cơ chế polling dự phòng), `payment-result.html`, `admin-payment.html`

---

## 2. Luồng Thanh toán VietQR Tự động qua SePay

```mermaid
sequenceDiagram
    actor U as Khách hàng (App Ngân hàng)
    participant FE as Frontend (order.html / checkout.html)
    participant BE as PaymentController / IPaymentService
    participant WS as WebSocket (/topic/payment/{orderId})
    participant SP as SePay Server
    participant DB as PostgreSQL (orders, payments)

    FE->>BE: GET /api/payment/create-sepay/{orderId} (Bearer JWT)
    BE->>DB: Kiểm tra đơn thuộc tài khoản & chưa COMPLETED/CANCELLED
    BE-->>FE: Trả qrUrl (qr.sepay.vn), STK, số tiền & nội dung chuyển khoản "DH{orderId}"

    FE->>WS: Mở kết nối SockJS(/ws) & Subscribe "/topic/payment/{orderId}"
    Note over FE: Đồng thời bật polling dự phòng mỗi vài giây<br/>GET /api/payment/order/{orderId}/latest

    U->>SP: Quét mã VietQR trên điện thoại & chuyển khoản (nội dung DH{orderId})
    SP->>BE: POST /api/payment/sepay-webhook (Header Authorization: Apikey <key>)

    BE->>BE: 1. Đối chiếu SEPAY_WEBHOOK_API_KEY
    BE->>BE: 2. Lọc transferType == "in" (bỏ qua giao dịch tiền ra, trả 200 OK)
    BE->>DB: 3. Kiểm tra chống trùng lặp transactionId = "SEPAY_{id}" (Idempotency)
    BE->>BE: 4. Tách mã đơn bằng Regex: DH\s*(\d+) (giao dịch cá nhân trả 200 OK)
    BE->>DB: 5. Kiểm tra đơn tồn tại, chưa CANCELLED & transferAmount >= totalAmount
    alt Đủ tiền & Đơn hợp lệ
        BE->>DB: Cập nhật orders.status = "COMPLETED" & Lưu Payment (SUCCESS, code "00")
    else Chuyển thiếu tiền hoặc Đơn đã hủy
        BE->>DB: Lưu bản ghi Payment (FAILED, code "AMOUNT_MISMATCH" / "ORDER_CANCELLED")
    end
    BE->>WS: convertAndSend("/topic/payment/{orderId}", PaymentResponse)
    WS-->>FE: Nhận sự kiện SUCCESS tức thì -> Tự đóng Modal QR & hiện thông báo thành công!
    BE-->>SP: 200 OK
```

### Các chốt an toàn quan trọng trong `processSePayWebhook`:
1. **Xác thực nguồn gọi:** Kiểm tra header `Authorization: Apikey <SEPAY_WEBHOOK_API_KEY>` (nếu biến môi trường được cấu hình), chặn hoàn toàn webhook giả mạo từ bên ngoài.
2. **Idempotency (Chống cộng/duyệt trùng):** Mỗi giao dịch SePay được gắn mã duy nhất `SEPAY_{id}` và kiểm tra qua `paymentRepository.existsByTransactionId(txId)` trước khi xử lý.
3. **Không gây vòng lặp Retry cho giao dịch ngoài luồng:** Giao dịch tiền ra (`out`) hoặc chuyển khoản cá nhân không chứa tiền tố `DH{mã_đơn}` được ghi nhận log nhẹ và trả `HTTP 200 OK` thay vì ném lỗi `400`, giúp SePay không gửi lại nhiều lần.
4. **Bảo vệ đơn đã hủy (`CANCELLED`):** Nếu khách chuyển khoản muộn vào một đơn đã bị hủy (đã hoàn kho), hệ thống không tự động bật lại `COMPLETED` mà ghi nhận bản ghi `Payment` với mã `ORDER_CANCELLED` để Admin đối soát hoàn tiền.

---

## 3. Luồng Thanh toán qua Cổng VNPay Sandbox (`HMAC-SHA512`)

```mermaid
sequenceDiagram
    actor U as Khách hàng
    participant FE as Frontend
    participant BE as PaymentController / IPaymentService
    participant VNP as Cổng VNPay Sandbox
    participant DB as PostgreSQL

    FE->>BE: GET /api/payment/create-vnpay/{orderId}
    BE->>BE: Chuẩn hóa IPv4 (X-FORWARDED-FOR), tạo tham số vnp_Amount (x100),<br/>vnp_TxnRef = orderId, hết hạn sau 15 phút & ký HMAC-SHA512
    BE-->>FE: Trả paymentUrl
    FE->>VNP: Chuyển hướng trình duyệt sang trang thanh toán VNPay
    U->>VNP: Nhập thẻ test & xác nhận OTP trên VNPay
    VNP-->>FE: Redirect về vnpay.return-url (payment-result.html?vnp_Amount=...&vnp_SecureHash=...)
    FE->>BE: POST /api/payment/verify { params }
    BE->>BE: Sắp xếp tham số theo bảng chữ cái & kiểm tra chữ ký HMAC-SHA512
    BE->>DB: Đối chiếu khớp số tiền -> Cập nhật orders.status = "COMPLETED" (nếu vnp_ResponseCode == "00")
    BE->>DB: Lưu lịch sử giao dịch vào bảng payments (payment_gateway = "VNPAY")
    BE-->>FE: Trả PaymentResponse để hiển thị kết quả giao dịch
```
