# Luồng 05 — Bảo Hành Điện Tử, Đặt Lịch Sửa Chữa, Đánh Giá & Hỗ Trợ

Tài liệu này mô tả các luồng dịch vụ kỹ thuật và chăm sóc sau bán hàng của **HK COMPUTER**: tra cứu thẻ bảo hành tính từ dữ liệu đơn hàng đã thanh toán, yêu cầu bảo hành, đặt lịch sửa chữa (tại cửa hàng hoặc tận nơi), đánh giá sản phẩm đã mua và phiếu hỗ trợ trực tuyến.

---

## 1. Các thành phần tham gia trong Code

- **Bảo hành điện tử:** `WarrantyController.java`, `IWarrantyService.java`, `WarrantyClaimRepository.java`, `WarrantyClaim.java`
- **Đặt lịch sửa chữa:** `RepairBookingController.java`, `IRepairBookingService.java`, `RepairBookingRepository.java`, `RepairBooking.java`
- **Đánh giá sản phẩm:** `ReviewController.java`, `IReviewService.java`, `ReviewRepository.java`, `detailproductuser.html`, `order.html`, `review.html`
- **Hỗ trợ khách hàng:** `SupportController.java`, `ISupportService.java`, `SupportRepository.java`, `support.html`, `admin-support.html`

---

## 2. Luồng Bảo hành Điện tử Tự động (`Warranty`)

### 2.1. Thẻ bảo hành điện tử
Thẻ được tính từ các đơn ở trạng thái **`COMPLETED`/`PAID`** khi khách xem thẻ của mình hoặc tra cứu. Thẻ không được lưu riêng; bảng `warranty_claims` chỉ lưu các yêu cầu xử lý. Mốc bắt đầu là `orders.paid_at`, được ghi khi thanh toán thành công hoặc Admin xác nhận đơn đã thanh toán. Với dữ liệu cũ không có giao dịch thành công, migration dùng `created_at` làm mốc thay thế.
- **Mã Serial định danh từng thiết bị:** `BH-{orderId}-{productId}-{unitNumber}` (ví dụ: `BH-15-8-2`). `unitNumber` bắt đầu từ 1 và không vượt quá tổng số lượng sản phẩm đó trong đơn.
- **Thời hạn bảo hành tự động theo danh mục (`resolveWarrantyMonths`):**
  - Linh kiện lõi (`CPU`, `Main`, `VGA`, `RAM`): **36 tháng**.
  - Phụ kiện (`Chuột`, `Bàn phím`, `Phụ kiện`...): **12 tháng**.
  - Laptop, PC & các danh mục còn lại: **24 tháng**.
- **Tra cứu (`GET /api/warranty/lookup?keyword=...`):** Yêu cầu đăng nhập. Khách chỉ xem đơn của mình; Admin có thể tra cứu mọi đơn. Có thể nhập serial từng thiết bị, serial cũ `BH-{orderId}-{productId}` để xem các thiết bị cùng dòng hàng, hoặc mã đơn `DH15` / `15`.

### 2.2. Luồng Gửi & Xử lý Yêu cầu Bảo hành (`POST /api/warranty/claims`)

```mermaid
sequenceDiagram
    actor U as Khách hàng
    actor A as Kỹ thuật viên / Admin
    participant BE as WarrantyController / IWarrantyService
    participant DB as PostgreSQL (orders, warranty_claims)

    U->>BE: POST /api/warranty/claims { orderId, productId, unitNumber, issueType, description, contactPhone }
    BE->>BE: Kiểm tra SĐT 10 số (PhoneValidator)
    BE->>DB: 1. Kiểm tra đơn thuộc tài khoản & status == COMPLETED/PAID
    BE->>DB: 2. Kiểm tra sản phẩm, unitNumber nằm trong số lượng đã mua & chưa hết hạn
    BE->>DB: 3. Chặn phiếu đang xử lý trùng trên cùng thiết bị
    BE->>DB: Lưu WarrantyClaim (PENDING, serialCode = BH-{orderId}-{productId}-{unitNumber})
    BE-->>U: Trả thông tin phiếu yêu cầu bảo hành

    A->>BE: PATCH /api/warranty/admin/{id}/status?status=... { adminNote }
    BE->>DB: Chỉ cho phép chuyển trạng thái hợp lệ & ghi chú kỹ thuật
    Note over DB: PENDING -> RECEIVED -> REPAIRING -> COMPLETED; REJECTED từ các trạng thái đang xử lý
```

---

## 3. Luồng Đặt lịch Sửa chữa Kỹ thuật (`Repair Booking`)

Hỗ trợ **cả khách vãng lai (chưa đăng nhập)** lẫn **khách đã đăng nhập** gửi yêu cầu đặt lịch:

```mermaid
flowchart TD
    A["Khách gửi yêu cầu: POST /api/repairs"] --> B["Kiểm tra thông tin, SĐT 10 số, ngày ISO & giờ HH:mm"]
    B --> C{"Hình thức dịch vụ (serviceMethod)?"}
    C -- "AT_STORE (Mặc định)" --> D["Chọn chi nhánh đang hoạt động bằng tên hoặc mã"]
    C -- "AT_HOME (Sửa tận nơi)" --> E["Bắt buộc nhập Địa chỉ nhà (homeAddress)<br/>Gán nhãn: Sửa tại nhà — {homeAddress}"]
    D & E --> F["Nếu khách đang đăng nhập -> Tự động gắn account_id & email"]
    F --> G["Lưu bảng repair_bookings (status = PENDING)"]
    G --> H["Admin xác nhận và cập nhật tiến độ<br/>(PENDING -> CONFIRMED -> IN_PROGRESS -> COMPLETED / CANCELLED)"]
```

Ngày hẹn phải là ngày hiện tại hoặc tương lai; giờ nhận `HH:mm` hoặc khoảng `HH:mm - HH:mm` và phải còn trong tương lai. Chi nhánh được đối chiếu với danh sách đang hoạt động, nhưng hiện chưa kiểm tra giờ mở cửa hoặc năng lực kỹ thuật viên/chặn trùng khung giờ. Lịch ở trạng thái `PENDING` mới là yêu cầu tiếp nhận, chưa giữ chỗ. Trạng thái chỉ chuyển theo thứ tự `PENDING -> CONFIRMED -> IN_PROGRESS -> COMPLETED`, có thể hủy trước khi hoàn tất.

---

## 4. Luồng Đánh giá Sản phẩm Đã mua (`Review`) & Hỗ trợ (`Support`)

1. **Đánh giá sản phẩm xác thực mua hàng (`POST /api/reviews`):**
   - Chỉ khách hàng **sở hữu đơn hàng** đó, đơn đã **`COMPLETED`/`PAID`**, sản phẩm thực sự nằm trong `order_items` của đơn và **chưa từng đánh giá** sản phẩm đó trong đơn (`existsByOrderIdAndProductId == false`) mới được phép gửi điểm sao (`1–5`) và nhận xét.
   - Đánh giá hiển thị công khai trên trang chi tiết sản phẩm (`GET /api/reviews/product/{productId}`).
2. **Phiếu hỗ trợ trực tuyến (`POST /api/support`):**
   - Khách đăng nhập gửi chủ đề (`subject`) và nội dung cần tư vấn/khiếu nại (`message`), theo dõi danh sách tại `GET /api/support/my-requests`.
   - Admin tiếp nhận và cập nhật trạng thái xử lý (`OPEN` → `IN_PROGRESS` → `CLOSED`) qua `PATCH /api/support/admin/{id}/status`.
