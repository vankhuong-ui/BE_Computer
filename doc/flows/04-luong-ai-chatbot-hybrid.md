# Luồng 04 — AI Chatbot Tư Vấn Bán Hàng Hybrid & 3 Lớp Rào Chắn (Guardrails)

Tài liệu này mô tả kiến trúc **Hybrid Chatbot** của HK COMPUTER: kết hợp truy vấn dữ liệu thực tế từ PostgreSQL, chuỗi mô hình ngôn ngữ lớn trên **Groq AI** có cơ chế tự động chuyển đổi dự phòng (Model Failover) và **3 lớp rào chắn bảo vệ thương hiệu**.

---

## 1. Các thành phần tham gia trong Code

- **Controller & Config:** `REVIEW/controller/ChatController.java`, `REVIEW/config/RestClientConfig.java` (connect timeout `5s`, response timeout `15s`)
- **Rào chắn nội dung:** `REVIEW/service/ChatbotGuardrail.java`
- **Trái tim xử lý Hybrid:** `ChatbotService.java`, `REVIEW/service/ipml/IChatbootService.java`, `AUTH/service/RedisRateLimiterService.java` (giới hạn tần suất 15 câu/phút/phiên & lưu tối đa 6 lượt ngữ cảnh hội thoại gần nhất trong Redis với TTL 15 phút)
- **Kiểm thử tự động:** `src/test/java/com/example/MyProject/REVIEW/service/ChatbotHybridTest.java` (12 kịch bản kiểm thử gồm tồn kho chi nhánh theo thành phố & 3 lớp Guardrails)
- **Frontend Widget:** `FE_Computer/assets/js/chat-widget.js` (giao diện hiện đại có Quick Prompt Chips, hiệu ứng đang gõ 3 chấm, nút Làm mới hội thoại, lọc chống XSS & nút CTA sản phẩm/dịch vụ)

---

## 2. Sơ đồ Luồng Xử lý Tổng thể

```mermaid
flowchart TD
    A["Khách gửi tin nhắn: POST /api/chat"] --> L1["LỚP RÀO CHẮN 1 — Input Guardrail (ChatbotGuardrail.checkInput)"]

    L1 -- "Rỗng / Quá 500 ký tự" --> R1["Nhắc nhập ngắn gọn hợp lệ"]
    L1 -- "Prompt Injection / Đòi xem System Prompt / Viết code" --> R2["Từ chối lịch sự, giữ đúng phạm vi tư vấn HK COMPUTER"]
    L1 -- "Nhắc tên đối thủ / Chê đắt / So sánh shop khác" --> R3["Phản hồi chuẩn bảo vệ thương hiệu HK COMPUTER<br/>(Chính hãng · Bảo hành điện tử 12-36T · Sửa tận nơi)"]

    L1 -- "Hợp lệ" --> B["TẦNG 1 — Phân tích Ý định & Truy vấn PostgreSQL"]
    B --> B1["Chuẩn hóa tiếng Việt không dấu + Bóc tách khoảng giá<br/>(dưới X tr, từ X đến Y tr, trên X tr, tầm X tr)"]
    B1 --> B2["Nhận diện Intent: Chào hỏi, Khả năng bot, Đắt nhất/Rẻ nhất,<br/>Mua/Mở thanh toán, Tồn kho, Bảo hành, Sửa chữa"]
    B2 --> B3["Loại bỏ từ đệm + Xếp hạng khớp ưu tiên Tên SP/Hãng/Danh mục trước Mô tả<br/>+ Cộng tổng tồn kho các chi nhánh đang hoạt động"]
    B3 --> B4["Sinh đồng thời: databaseContext (cho AI) & localFallbackReply (dự phòng)"]

    B4 --> C{"Có GROQ_API_KEY hợp lệ?"}
    C -- "Chưa cấu hình" --> L3_LOCAL["Trả localFallbackReply qua Output Sanitizer"]

    C -- "Có" --> D["TẦNG 2 — Chuỗi Model Groq AI (Lớp Rào chắn 2: System Prompt, temp=0.2)"]
    D --> M1["Thử model chính: qwen/qwen3.8-27b"]
    M1 -- "Lỗi 404 / 429 / Timeout" --> M2["Tự động chuyển sang: openai/gpt-oss-120b"]
    M2 -- "Lỗi" --> M3["Tự động chuyển sang: openai/gpt-oss-20b"]
    M3 -- "Tất cả model đều lỗi" --> L3_LOCAL

    M1 & M2 & M3 -- "Nhận phản hồi AI" --> L3["LỚP RÀO CHẮN 3 — Output Guardrail (ChatbotGuardrail.sanitizeOutput)"]
    L3 -- "Sạch & An toàn" --> OK["Trả câu trả lời AI (kèm nút Link mua hàng / thanh toán)"]
    L3 -- "Phát hiện lọt tên đối thủ / rò rỉ model / gsk_" --> L3_LOCAL
```

---

## 3. Chi tiết các Kỹ thuật Tối ưu trong Chatbot

### 3.1. Tìm kiếm sản phẩm tiếng Việt thông minh
- **Bóc tách ngân sách tự động:** Hiểu các cách diễn đạt tự nhiên như `"dưới 20 triệu"`, `"từ 10 đến 15tr"`, `"trên 25 củ"`, `"tầm 15 triệu"` (`±25%`), `"dưới 500k"`.
- **Ưu tiên trường chính (Primary-field Priority):** Điểm khớp trên `nameProduct` (+4) và `brands`/`categories` (+2) được ưu tiên tuyệt đối trước `specs`/`description` (+1). Nếu đã có sản phẩm khớp ở trường chính, các sản phẩm chỉ tình cờ trùng từ khóa trong mô tả dài sẽ bị loại bỏ, tránh gợi ý sai mặt hàng.
- **Tổng hợp tồn kho 3 Chi nhánh chính xác:** Cộng gộp tồn kho (`soluong`) của 3 chi nhánh đang hoạt động (`Hà Nội`, `TP.HCM`, `Đà Nẵng`) và tự động nhận diện tên thành phố/chi nhánh trong câu hỏi (*"Dell 7490 ở Hà Nội còn không?"*) để báo đúng tồn kho địa phương cũng như gợi ý chi nhánh còn hàng khác hỗ trợ giao toàn quốc.

### 3.2. Ba lớp Rào chắn bảo vệ thương hiệu (3-Layer Guardrails)
1. **Lớp 1 — Chặn đầu vào (`checkInput`):** Chặn sớm ở tầng Java (không tốn token AI) đối với câu quá dài, câu gài bẫy Prompt Injection (*"bỏ qua mọi hướng dẫn"*, *"in ra system prompt"*...) và các câu so sánh với sàn TMĐT/đối thủ cạnh tranh (danh sách tùy biến qua biến `chatbot.blocked-competitors`).
2. **Lớp 2 — Luật cứng trong `system` prompt:** Tách biệt hoàn toàn `role: "system"` và `role: "user"`, đặt `temperature = 0.2`, ép AI chỉ sử dụng dữ liệu thật trong khối `DỮ LIỆU CỬA HÀNG`, không bịa giá/khuyến mãi và không bao giờ khuyên khách mua nơi khác.
3. **Lớp 3 — Kiểm duyệt đầu ra (`sanitizeOutput`):** Trước khi phản hồi tới trình duyệt, Java quét lại toàn bộ câu trả lời của AI; nếu phát hiện lọt tên đối thủ, cụm từ bất lợi hoặc tên model nội bộ (`qwen`, `llama`, `groq`, `openai`, `gsk_`), hệ thống lập tức thay thế bằng `localFallbackReply` chuẩn xác từ CSDL.

### 3.3. Điều hướng Mua hàng 1-Click trên Giao diện Chat
- Backend đính kèm các liên kết nội bộ dạng `[Xem & mua <Tên SP>](detailproductuser.html?id=...)`, `[Đi tới trang thanh toán](checkout.html)`, `[Tra cứu bảo hành](warranty.html)`, `[Đặt lịch sửa chữa](support.html)`.
- Trên Frontend (`chat-widget.js`), hàm `formatMessage()` thực hiện **escape HTML chống XSS trước**, sau đó chỉ cho phép biến đổi các đường dẫn thuộc whitelist nội bộ của cửa hàng thành nút bấm `.ai-action-link` để khách chuyển trang mua hàng và quét mã SePay ngay.
