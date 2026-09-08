
package com.example.MyProject.REVIEW.service.ipml;

import com.example.MyProject.PRODUCT.entity.Inventory;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.repository.InventoryRepository;
import com.example.MyProject.PRODUCT.repository.ProductRepository;
import com.example.MyProject.REVIEW.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IChatbootService implements ChatbotService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestClient restClient;


    // =====================================================
    // CHAT
    // =====================================================

    @Override
    public String chat(String message) {

        // Lấy dữ liệu cần thiết từ database
        String databaseData = getDatabaseData(message);


        // =================================================
        // TẠO PROMPT
        // =================================================

        String prompt = """
                Bạn là chatbot hỗ trợ khách hàng của cửa hàng.

                Hãy trò chuyện với khách hàng một cách tự nhiên,
                thân thiện và giống như một nhân viên bán hàng.

                QUY TẮC:

                1. Nếu khách hàng chào hỏi như:
                   "xin chào", "hello", "hi", "chào bạn", "alo"...
                   → Hãy chào lại một cách tự nhiên và thân thiện.
                   → Có thể giới thiệu ngắn gọn rằng bạn có thể hỗ trợ
                     tìm sản phẩm, giá sản phẩm và tình trạng còn hàng.

                2. Nếu khách hàng cảm ơn:
                   → Hãy trả lời tự nhiên như:
                     "Không có gì nhé!" hoặc câu tương tự.

                3. Nếu khách hàng nói chuyện thông thường:
                   → Hãy trả lời tự nhiên, lịch sự.
                   → Không được nói rằng không tìm thấy dữ liệu database.

                4. Nếu khách hỏi về sản phẩm:
                   → Chỉ sử dụng thông tin sản phẩm có trong DATABASE.
                   → Không được tự bịa giá, thông số hoặc mô tả sản phẩm.

                5. Nếu khách hỏi về tồn kho:
                   → Chỉ sử dụng số lượng tồn kho trong DATABASE.
                   → Không được tự đoán số lượng hàng.

                6. Nếu DATABASE có thông tin:
                   → Trả lời trực tiếp câu hỏi.
                   → Diễn đạt tự nhiên, dễ hiểu.
                   → Không cần nói "theo database".

                7. Nếu khách hỏi về sản phẩm nhưng không có dữ liệu:
                   → Trả lời:
                     "Mình chưa tìm thấy thông tin về sản phẩm này trong hệ thống."

                8. Nếu khách hỏi vấn đề không liên quan đến:
                   - sản phẩm
                   - giá sản phẩm
                   - thông tin sản phẩm
                   - tồn kho
                   - tình trạng còn hàng

                   → Hãy lịch sự nói rằng bạn chủ yếu hỗ trợ
                     thông tin về sản phẩm và tình trạng hàng.

                9. Không được bịa thông tin.

                10. Luôn trả lời bằng tiếng Việt.

                11. Trả lời ngắn gọn, tự nhiên, không máy móc.

                =========================
                DATABASE
                =========================

                %s

                =========================
                CÂU HỎI KHÁCH HÀNG
                =========================

                %s

                =========================

                Hãy trả lời khách hàng.
                """.formatted(
                databaseData,
                message
        );


        // =================================================
        // GỌI GEMINI
        // =================================================

        return callGemini(prompt);
    }


    // =====================================================
    // XÁC ĐỊNH LOẠI CÂU HỎI
    // =====================================================

    private String getDatabaseData(String message) {

        String lower = message.toLowerCase().trim();


        // =================================================
        // 1. DANH SÁCH SẢN PHẨM
        // =================================================

        if (lower.contains("có những sản phẩm")
                || lower.contains("có sản phẩm gì")
                || lower.contains("danh sách sản phẩm")
                || lower.contains("sản phẩm nào")
                || lower.contains("shop có gì")
                || lower.contains("bán những gì")
                || lower.contains("cửa hàng bán gì")) {

            return getProductList();
        }


        // =================================================
        // 2. HỎI TỒN KHO
        // =================================================

        if (lower.contains("còn hàng")
                || lower.contains("tồn kho")
                || lower.contains("có hàng")
                || lower.contains("còn bao nhiêu")
                || lower.contains("số lượng")
                || lower.contains("hết hàng")
                || lower.contains("còn không")
                || lower.contains("còn sản phẩm")) {

            return getInventory(message);
        }


        // =================================================
        // 3. KIỂM TRA CÓ PHẢI CÂU HỎI VỀ SẢN PHẨM KHÔNG
        // =================================================

        List<Products> products =
                productRepository
                        .findByNameProductContainingIgnoreCase(message)
                        .stream()
                        .limit(5)
                        .toList();


        if (!products.isEmpty()) {

            return getProducts(message);
        }


        // =================================================
        // 4. HỘI THOẠI THÔNG THƯỜNG
        // =================================================

        return """
                Không có dữ liệu database cần truy vấn.

                Đây là câu nói chuyện thông thường của khách hàng.
                Hãy trả lời tự nhiên, thân thiện và ngắn gọn.

                Không được nói:
                "Tôi không tìm thấy thông tin này trong hệ thống."

                Không cần đề cập đến database.
                """;
    }


    // =====================================================
    // DANH SÁCH SẢN PHẨM
    // =====================================================

    private String getProductList() {

        List<Products> products =
                productRepository.findAll(
                        PageRequest.of(0, 20)
                ).getContent();


        if (products.isEmpty()) {

            return "Cửa hàng hiện chưa có sản phẩm.";
        }


        StringBuilder data = new StringBuilder();

        data.append("Danh sách sản phẩm hiện có:\n");


        for (Products product : products) {

            data.append("- ")
                    .append(product.getNameProduct())
                    .append(" | Giá: ")
                    .append(product.getPrice())
                    .append("\n");
        }


        return data.toString();
    }


    // =====================================================
    // TÌM THÔNG TIN SẢN PHẨM
    // =====================================================

    private String getProducts(String message) {

        List<Products> products =
                productRepository
                        .findByNameProductContainingIgnoreCase(message)
                        .stream()
                        .limit(5)
                        .toList();


        if (products.isEmpty()) {

            return "Không tìm thấy sản phẩm phù hợp.";
        }


        StringBuilder data = new StringBuilder();


        for (Products product : products) {

            data.append("--------------------\n");

            data.append("Tên sản phẩm: ")
                    .append(product.getNameProduct())
                    .append("\n");

            data.append("Giá: ")
                    .append(product.getPrice())
                    .append("\n");

            data.append("Thông số: ")
                    .append(product.getSpecs())
                    .append("\n");

            data.append("Mô tả: ")
                    .append(product.getDescription())
                    .append("\n");
        }


        return data.toString();
    }


    // =====================================================
    // TỒN KHO
    // =====================================================

    private String getInventory(String message) {

        List<Products> products =
                productRepository
                        .findByNameProductContainingIgnoreCase(message)
                        .stream()
                        .limit(5)
                        .toList();


        if (products.isEmpty()) {

            return "Không tìm thấy sản phẩm để kiểm tra tồn kho.";
        }


        StringBuilder data = new StringBuilder();


        for (Products product : products) {

            Inventory inventory =
                    inventoryRepository
                            .findByProducts_Id(product.getId())
                            .orElse(null);


            int stock = inventory == null
                    ? 0
                    : inventory.getSoluong();


            data.append("--------------------\n");

            data.append("Tên sản phẩm: ")
                    .append(product.getNameProduct())
                    .append("\n");

            data.append("Giá: ")
                    .append(product.getPrice())
                    .append("\n");

            data.append("Tồn kho: ")
                    .append(stock)
                    .append("\n");
        }


        return data.toString();
    }


    // =====================================================
    // GỌI GEMINI
    // =====================================================

    private String callGemini(String prompt) {

        Map<String, Object> requestBody =
                Map.of(
                        "contents",
                        List.of(
                                Map.of(
                                        "parts",
                                        List.of(
                                                Map.of(
                                                        "text",
                                                        prompt
                                                )
                                        )
                                )
                        )
                );


        try {

            Map<?, ?> response =
                    restClient.post()
                            .uri(apiUrl)
                            .header(
                                    "x-goog-api-key",
                                    apiKey
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(requestBody)
                            .retrieve()
                            .body(Map.class);


            if (response == null) {

                return "Gemini không trả về dữ liệu.";
            }


            List<?> candidates =
                    (List<?>) response.get("candidates");


            if (candidates == null
                    || candidates.isEmpty()) {

                return "Gemini không có câu trả lời.";
            }


            Map<?, ?> candidate =
                    (Map<?, ?>) candidates.get(0);


            Map<?, ?> content =
                    (Map<?, ?>) candidate.get("content");


            if (content == null) {

                return "Gemini không có nội dung trả lời.";
            }


            List<?> parts =
                    (List<?>) content.get("parts");


            if (parts == null
                    || parts.isEmpty()) {

                return "Gemini không có nội dung trả lời.";
            }


            Map<?, ?> part =
                    (Map<?, ?>) parts.get(0);


            return String.valueOf(
                    part.get("text")
            );


        } catch (Exception e) {

            e.printStackTrace();

            return "Không thể kết nối tới Gemini.";
        }
    }
}

