# 02. Thiết Kế Cơ Sở Dữ Liệu (PostgreSQL & Flyway `V1` → `V19`)

Hệ thống sử dụng **PostgreSQL 16** kết hợp **Flyway Migration** tự động kiểm tra và nâng cấp lược đồ (`spring.jpa.hibernate.ddl-auto=validate`). Tổng cộng hệ thống gồm **17 bảng nghiệp vụ** (không tính bảng lịch sử `flyway_schema_history`), chia thành 4 phân hệ rõ ràng.

---

## 1. Sơ đồ Quan hệ Thực thể (ER Diagram)

```mermaid
erDiagram
    account ||--o{ refresh_tokens : "sở hữu"
    account ||--o{ user_addresses : "lưu địa chỉ"
    account ||--o{ cart : "giỏ hàng"
    account ||--o{ warranty_claims : "yêu cầu BH"
    account ||--o{ repair_bookings : "đặt lịch sửa"
    account ||--o{ supports : "gửi hỗ trợ"

    brands ||--o{ products : "thuộc hãng"
    categories ||--o{ products : "thuộc danh mục"
    branches ||--o{ inventory : "lưu kho tại"
    products ||--o{ inventory : "tồn kho"

    user_addresses ||--o{ orders : "giao tới"
    branches ||--o{ orders : "nhận tại quầy"
    vouchers ||--o{ orders : "áp mã giảm"

    orders ||--|{ order_items : "gồm các dòng"
    products ||--o{ order_items : "được mua"
    branches ||--o{ order_items : "xuất từ kho"

    orders ||--o{ payments : "lịch sử thanh toán"
    orders ||--o{ reviews : "đánh giá sau mua"
    products ||--o{ reviews : "nhận đánh giá"
    orders ||--o{ warranty_claims : "yêu cầu BH"
    products ||--o{ warranty_claims : "bảo hành SP"

    account {
        bigint id PK
        varchar accountname
        varchar gmail
        varchar password
        boolean enabled
        varchar role
        varchar googleid
    }
    branches {
        bigint id PK
        varchar code UK
        varchar name
        varchar city
        varchar address
        boolean active
    }
    categories {
        bigint id PK
        varchar name_categories
        varchar slug UK
        varchar group_type
    }
    brands {
        bigint id PK
        varchar name_brands
        varchar slug UK
        varchar logo
    }
    products {
        bigint id PK
        bigint brands_id FK
        bigint categories_id FK
        varchar name_product
        varchar slug UK
        decimal price
        text specs
        text image
    }
    inventory {
        bigint id PK
        bigint product_id FK
        bigint branch_id FK
        int soluong
        int selled
    }
    vouchers {
        bigint id PK
        varchar code UK
        varchar discount_type
        decimal discount_value
        decimal min_order_amount
        boolean first_order_only
        int usage_limit
        int used_count
        boolean active
    }
    orders {
        bigint id PK
        bigint useraddresses_id FK
        bigint pickup_branch_id FK
        bigint voucher_id FK
        varchar fulfillment_type
        decimal discount_amount
        decimal total_amount
        varchar status
        timestamp created_at
        timestamp paid_at
    }
    order_items {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        bigint branch_id FK
        int quantity
        decimal price
    }
    payments {
        bigint id PK
        bigint order_id FK
        varchar transaction_id
        varchar payment_gateway
        decimal amount
        varchar status
    }
    warranty_claims {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        bigint account_id FK
        varchar serial_code
        int unit_number
        varchar status
        timestamp warranty_expire_date
    }
    repair_bookings {
        bigint id PK
        bigint account_id FK
        varchar customer_phone
        varchar service_method
        varchar branch_name
        varchar appointment_date
        varchar status
    }
```

---

## 2. Chi tiết 17 bảng dữ liệu theo Phân hệ

### Phân hệ A — Tài khoản & Bảo mật (3 bảng)

| Bảng | Migration | Khóa chính / Ràng buộc | Mô tả các cột quan trọng |
| :--- | :---: | :--- | :--- |
| **`account`** | `V1`, `V14` | `id` (PK) | `accountname`, `gmail`, `password` (BCrypt), `enabled` (`false` cho tới khi xác thực OTP), `role` (`USER` / `ADMIN`), `googleid` (OAuth2). |
| **`refresh_tokens`** | `V2` | `id` (PK), `token` (UNIQUE), FK `account_id` | Lưu Refresh Token dạng UUID, `expiry_date` (`TIMESTAMPTZ`), `revoked`. Có index `idx_refresh_tokens_account_id`. |
| **`user_addresses`** | `V6` | `id` (PK), FK `account_id` | Sổ địa chỉ nhận hàng của khách (tối đa 5 địa chỉ/tài khoản): `username`, `phone` (10 số VN), `address`, `defaultaddress`. |

---

### Phân hệ B — Danh mục, Sản phẩm & Kho Đa Cơ Sở (5 bảng)

| Bảng | Migration | Khóa chính / Ràng buộc | Mô tả các cột quan trọng |
| :--- | :---: | :--- | :--- |
| **`branches`** | `V18` | `id` (PK), `code` (UNIQUE) | Hệ thống chi nhánh cửa hàng trên toàn quốc (`HN-THAIHA`, `HCM-QUAN10`...): `name`, `city`, `district`, `address`, `phone`, `opening_hours`, `services`, `active`. |
| **`brands`** | `V3` | `id` (PK), `slug` (UNIQUE) | Thương hiệu sản phẩm (`Asus`, `Dell`, `Lenovo`, `Logitech`...): `name_brands`, `slug`, `logo`. |
| **`categories`** | `V4`, `V18` | `id` (PK), `slug` (UNIQUE) | Danh mục sản phẩm kèm phân nhóm `group_type` (`LAPTOP` \| `PC` \| `ACCESSORY`). |
| **`products`** | `V5`, `V18` | `id` (PK), `slug` (UNIQUE), FK `brands_id`, `categories_id` | Thông tin sản phẩm: `name_product`, `price` (`DECIMAL(18,2)`), `specs` (`TEXT`), `image` (`TEXT`), `description` (`TEXT`). |
| **`inventory`** | `V7`, `V18` | `id` (PK), `UNIQUE(product_id, branch_id)`, FK `product_id`, `branch_id` | Tồn kho thực tế của **từng sản phẩm tại từng cơ sở**: `soluong` (tồn khả dụng), `selled` (đã bán). |

---

### Phân hệ C — Giỏ hàng, Khuyến mãi, Đơn hàng & Thanh toán (5 bảng)

| Bảng | Migration | Khóa chính / Ràng buộc | Mô tả các cột quan trọng |
| :--- | :---: | :--- | :--- |
| **`cart`** | `V13` | `id` (PK), FK `product_id`, `account_id` | Lưu bền vững giỏ hàng trong PostgreSQL (`quality` = số lượng), đồng bộ song song với Redis Hash `cart:{accountId}`. |
| **`vouchers`** | `V18` | `id` (PK), `code` (UNIQUE) | Mã giảm giá thương mại: `discount_type` (`FIXED` \| `PERCENT`), `discount_value`, `max_discount`, `min_order_amount`, `first_order_only` (ưu đãi khách mới), `usage_limit`, `used_count`, `start_date`, `end_date`, `active`. |
| **`orders`** | `V8`, `V18`, `V19` | `id` (PK), FK `useraddresses_id`, `pickup_branch_id`, `voucher_id` | Đơn hàng: `fulfillment_type` (`HOME_DELIVERY` \| `STORE_PICKUP`), `receiver_name`, `receiver_phone`, `voucher_code`, `discount_amount`, `total_amount`, `status` (`UNPAID` \| `COMPLETED` \| `CANCELLED`), `created_at`, `paid_at` (mốc bắt đầu bảo hành). |
| **`order_items`** | `V9`, `V18` | `id` (PK), FK `order_id`, `product_id`, `branch_id` | Chi tiết từng mặt hàng trong đơn, lưu cả `branch_id` (cơ sở xuất kho thực tế để hoàn kho chính xác nếu hủy đơn) và đơn giá `price` tại thời điểm mua. |
| **`payments`** | `V10` | `id` (PK), FK `order_id`, `CHECK (status IN ('PENDING','SUCCESS','FAILED'))` | Lịch sử giao dịch thanh toán: `transaction_id` (`SEPAY_{id}` hoặc mã GD VNPay), `payment_gateway` (`SEPAY` \| `VNPAY`), `amount`, `status`, `response_code`, `created_at`. |

---

### Phân hệ D — Hậu mãi, Bảo hành, Sửa chữa & Đánh giá (4 bảng)

| Bảng | Migration | Khóa chính / Ràng buộc | Mô tả các cột quan trọng |
| :--- | :---: | :--- | :--- |
| **`warranty_claims`** | `V15`, `V19` | `id` (PK), FK `order_id`, `product_id`, `account_id` | Yêu cầu bảo hành theo từng thiết bị: `serial_code` (`BH-{orderId}-{productId}-{unitNumber}`), `unit_number`, `issue_type`, `description`, `contact_phone`, `status`, `admin_note`, `warranty_months`, `warranty_expire_date`. Chỉ cho phép một yêu cầu đang xử lý trên mỗi thiết bị. |
| **`repair_bookings`** | `V16`, `V17`, `V19` | `id` (PK), FK `account_id` (nullable cho khách vãng lai) | Yêu cầu lịch sửa chữa kỹ thuật: `customer_name`, `customer_phone`, `service_method` (`AT_STORE` \| `AT_HOME`), `branch_name`, `home_address`, `device_model`, `service_type`, `issue_description`, `appointment_date`, `appointment_time`, `status`, `admin_note`. Ngày/giờ được lưu chuỗi ISO/HH:mm; trạng thái `PENDING` chưa giữ chỗ. |
| **`reviews`** | `V11` | `id` (PK), FK `order_id`, `product_id`, `CHECK (rating BETWEEN 1 AND 5)` | Đánh giá sản phẩm sau khi mua (chỉ đơn `COMPLETED`/`PAID` mới được đánh giá, mỗi sản phẩm trong 1 đơn chỉ đánh giá 1 lần): `rating`, `comment`, `created_at`. |
| **`supports`** | `V12` | `id` (PK), FK `account_id` | Phiếu gửi câu hỏi / hỗ trợ khách hàng: `subject`, `message`, `status` (`OPEN` / `IN_PROGRESS` / `CLOSED`), `created_at`. |

---

## 3. Các Chỉ mục (Indexes) tối ưu hiệu năng truy vấn

Được bổ sung tại các migration tương ứng để tối ưu tốc độ lọc sản phẩm, tra cứu tồn kho đa chi nhánh, phân trang đơn hàng và tra cứu lịch sửa chữa:

- `idx_refresh_tokens_account_id` trên `refresh_tokens(account_id)`
- `idx_inventory_branch_id` trên `inventory(branch_id)` + Unique index `uk_inventory_product_branch(product_id, branch_id)`
- `idx_products_categories_id` trên `products(categories_id)`
- `idx_products_brands_id` trên `products(brands_id)`
- `idx_products_price` trên `products(price)`
- `idx_orders_status_created` trên `orders(status, created_at DESC)`
- `idx_orders_voucher_id` trên `orders(voucher_id)`
- `idx_vouchers_active` trên `vouchers(active)`
- `uk_warranty_active_claim_per_unit` trên `warranty_claims(order_id, product_id, unit_number)` cho các phiếu đang xử lý
- `idx_repair_bookings_schedule` trên `repair_bookings(appointment_date, appointment_time, status)`
