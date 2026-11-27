package com.example.MyProject.contract;

import com.example.MyProject.ORDER.controller.OrderController;
import com.example.MyProject.ORDER.controller.PaymentController;
import com.example.MyProject.ORDER.service.OrdersService;
import com.example.MyProject.ORDER.service.PaymentService;
import com.example.MyProject.PRODUCT.controller.BranchController;
import com.example.MyProject.PRODUCT.dto.branch.BranchResponse;
import com.example.MyProject.PRODUCT.service.BranchService;
import com.example.MyProject.REVIEW.controller.ChatController;
import com.example.MyProject.REVIEW.service.ChatbotService;
import com.example.MyProject.common.exception.BusinessException;
import com.example.MyProject.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApiContractControllerTest {

    @Mock private BranchService branchService;
    @Mock private OrdersService ordersService;
    @Mock private PaymentService paymentService;
    @Mock private ChatbotService chatbotService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new BranchController(branchService),
                        new OrderController(ordersService),
                        new PaymentController(paymentService),
                        new ChatController(chatbotService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void branchesEndpointReturnsStandardApiResponseEnvelopeWithThreeCoreBranches() throws Exception {
        when(branchService.listPublicBranches(null)).thenReturn(List.of(
                new BranchResponse(1L, "HN-THAIHA", "HK COMPUTER - Thái Hà", "Hà Nội", "Q. Đống Đa", "123 Thái Hà", "024 7300 6801", "8:00 - 21:00", "Showroom", true, 16L, 120L),
                new BranchResponse(2L, "HCM-QUAN10", "HK COMPUTER - Quận 10", "TP.HCM", "Q. 10", "456 CMT8", "028 7300 6804", "8:00 - 21:00", "Showroom", true, 16L, 95L),
                new BranchResponse(3L, "DN-HAICHAU", "HK COMPUTER - Hải Châu", "Đà Nẵng", "Q. Thanh Khê", "142 Hàm Nghi", "0236 730 6807", "8:00 - 20:30", "Showroom", true, 16L, 80L)
        ));

        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.result.length()").value(3))
                .andExpect(jsonPath("$.result[0].code").value("HN-THAIHA"))
                .andExpect(jsonPath("$.result[1].code").value("HCM-QUAN10"))
                .andExpect(jsonPath("$.result[2].code").value("DN-HAICHAU"));
    }

    @Test
    void businessExceptionMapsToHttp400WhileUnexpectedRuntimeMapsToHttp500() throws Exception {
        doThrow(new BusinessException("Sản phẩm không đủ số lượng tồn kho"))
                .when(ordersService).createOrser(any());

        String orderJson = """
                {
                  "AddressesId": 1,
                  "fulfillmentType": "HOME_DELIVERY",
                  "orderItems": [{ "productId": 5, "quantity": 99 }]
                }
                """;

        mockMvc.perform(post("/api/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(999))
                .andExpect(jsonPath("$.message").value("Sản phẩm không đủ số lượng tồn kho"));

        when(chatbotService.chatFromClient(any(), any(), any()))
                .thenThrow(new IllegalStateException("Redis cluster socket broken"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"xin chao\",\"sessionId\":\"s1\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Hệ thống đang gặp sự cố tạm thời, vui lòng thử lại sau"));
    }

    @Test
    void invalidSePayWebhookApiKeyMapsToHttp403Forbidden() throws Exception {
        when(paymentService.processSePayWebhook(any(), any()))
                .thenThrow(new SecurityException("Invalid SePay Webhook API Key"));

        mockMvc.perform(post("/api/payment/sepay-webhook")
                        .header("Authorization", "Apikey invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"transferType\":\"in\",\"transferAmount\":100000,\"content\":\"DH1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
