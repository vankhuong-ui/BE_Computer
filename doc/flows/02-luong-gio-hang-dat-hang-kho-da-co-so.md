# Luồng 02 — Giỏ Hàng (DB + Redis) & Đặt Hàng Kho 3 Chi Nhánh Trọng Điểm

Tài liệu này mô tả luồng đồng bộ giỏ hàng hai tầng (PostgreSQL + Redis), thuật toán phân bổ tồn kho trên **3 chi nhánh trọng điểm (Hà Nội · TP.HCM · Đà Nẵng)** khi đặt hàng và cơ chế hoàn kho tự động khi hủy đơn hoặc đơn treo quá hạn.

---

## 1. Các thành phần tham gia trong Code

- **Giỏ hàng:** `CartController.java`, `ICartService.java`, `IRedisCartService.java`, `CartRepository.java`
- **Tồn kho & Chi nhánh:** `BranchController.java`, `BranchService.java`, `IBranchService.java`, `InventoryController.java`, `IInventoryService.java`, `InventoryWebSocketService.java`
- **Đơn hàng & Cron:** `OrderController.java`, `IOrdersService.java`, `OrderExpirationScheduler.java`, `OrderNotificationService.java`, `OrdersRepository.java`
- **Frontend:** `cart.html`, `checkout.html`, `order.html`, `admin-inventory.html`, `admin-orders.html`

---

## 2. Cơ chế Giỏ hàng Hai tầng (Write-Through PostgreSQL + Redis Hash)

- **Cấu trúc trên Redis:** Mỗi khách hàng có một Redis Hash mang khóa `cart:{accountId}`, trong đó `field = productId` và `value = quantity`.
- **Ghi (`addCart`, `updateCart`, `deleteCart`):** Ghi bền vững vào bảng `cart` trong PostgreSQL trước (trong `@Transactional`), sau đó cập nhật đồng bộ ngay vào `cart:{accountId}` trên Redis.
- **Đọc (`showCart`):**
  1. Ưu tiên đọc nhanh danh sách `(productId, quantity)` từ Redis `cart:{accountId}`, kết hợp 1 câu truy vấn `JOIN FETCH` duy nhất từ `cartRepository.findByAccountId(...)` để lấy đầy đủ thông tin sản phẩm mà **không bị lỗi N+1 query**.
  2. Nếu Redis trống (ví dụ sau khi flush cache), tự động đọc từ PostgreSQL và nạp ngược lại (warm-up) vào Redis.

---

## 3. Luồng Đặt hàng Đa cơ sở & Áp dụng Voucher (`POST /api/orders/create`)

```mermaid
flowchart TD
    A["Khách bấm Đặt hàng tại checkout.html"] --> B{"Hình thức nhận hàng?"}

    B -- "STORE_PICKUP (Nhận tại quầy)" --> C["Kiểm tra chi nhánh pickupBranchId đang active"]
    C --> D["Trừ kho trực tiếp tại chi nhánh đã chọn<br/>(decreaseStockAtBranch)"]
    D -- "Không đủ tồn kho" --> ERR1["Rollback & Báo lỗi thiếu hàng tại cơ sở"]

    B -- "HOME_DELIVERY (Giao tận nơi)" --> E["Thuật toán phân bổ kho tự động 3 mức ưu tiên:"]
    E --> E1["1. Cơ sở khách đang chọn trên thanh vị trí (preferredBranchId)"]
    E1 --> E2["2. Cơ sở cùng Tỉnh/Thành phố với địa chỉ giao (nhiều hàng nhất)"]
    E2 --> E3["3. Cơ sở còn tồn kho dồi dào nhất toàn quốc"]
    E3 --> F["Trừ kho nguyên tử tại cơ sở được chọn & lưu branch_id vào order_items"]

    D -- "Thành công" --> G["Phát WebSocket /topic/inventory/{invId} cập nhật tổng tồn kho"]
    F --> G

    G --> H{"Có nhập mã Voucher?"}
    H -- "Có" --> I["Kiểm tra: active, hạn dùng, usageLimit, minOrderAmount,<br/>firstOrderOnly (đơn đầu), mỗi tài khoản dùng tối đa 1 lần"]
    I -- "Hợp lệ" --> J["Tính discountAmount & tăng lượt dùng nguyên tử SQL<br/>(voucherRepository.incrementUsageIfAvailable chống vượt trần)"]
    H -- "Không" --> K["discountAmount = 0"]
    J --> L["Lưu Orders (status = UNPAID) & tự động xóa SP đã đặt khỏi Giỏ hàng (DB + Redis)"]
    K --> L
    L --> M["Chuyển sang bước Thanh toán (SePay VietQR / VNPay / Thanh toán sau)"]
```

---

## 4. Vòng đời Trạng thái Đơn hàng, Hủy Đơn Treo Tự Động (>30 phút) & Hoàn kho

- **Cron tự động dọn đơn treo (`OrderExpirationScheduler.java`):** Chạy định kỳ mỗi `5 phút`, tự động tìm các đơn `UNPAID` quá `30 phút` kể từ lúc tạo và gọi `ordersService.updateStatus(id, "CANCELLED")` để giải phóng tồn kho chi nhánh và lượt Voucher bị chiếm giữ.

```mermaid
stateDiagram-v2
    [*] --> UNPAID : Tạo đơn (Đã giữ tồn kho & lượt Voucher)
    UNPAID --> COMPLETED : SePay Webhook / VNPay xác nhận / Admin duyệt
    UNPAID --> CANCELLED : Khách tự hủy hoặc Admin hủy đơn
    COMPLETED --> CANCELLED : Admin hủy đơn đặc biệt

    note right of CANCELLED
        Tự động gọi restoreStockForOrder():
        1. Cộng trả số lượng về đúng branch_id của từng OrderItem
        2. Phát WebSocket cập nhật lại tồn kho trên UI
        3. Giảm voucher.usedCount đi 1 (hoàn lượt mã giảm giá)
        4. Chốt cứng trạng thái (không cho đổi ngược từ CANCELLED)
    end note
```

---

## 5. Nghiệp vụ Quản trị Kho Đa Cơ Sở dành cho Admin

Trong `IInventoryService.java` và `admin-inventory.html`, Admin có 4 công cụ điều phối hàng hóa trên toàn hệ thống chi nhánh:
1. **Khởi tạo / Nhập kho hàng loạt (`POST /api/inventory/create`):** Nhập hàng vào 1 cơ sở cụ thể hoặc bật cờ `allBranches = true` để phân bổ đồng loạt cho tất cả cơ sở đang hoạt động.
2. **Nhập thêm hàng (`PUT /api/inventory/{id}/soluong`):** Cộng dồn lượng hàng mới về kho chi nhánh.
3. **Kiểm kê điều chỉnh (`PUT /api/inventory/{id}/adjust`):** Chốt lại con số tồn kho thực tế sau kiểm kê kho vật lý.
4. **Điều chuyển nội bộ giữa các chi nhánh (`POST /api/inventory/transfer`):** Trừ tồn tại `fromBranchId` và cộng sang `toBranchId` trong cùng một giao dịch `@Transactional`, đảm bảo tổng tồn toàn chuỗi nhất quán.
