package com.example.MyProject.REVIEW.controller;

import com.example.MyProject.AUTH.service.RedisRateLimiterService;
import com.example.MyProject.ORDER.common.ApiResponse;
import com.example.MyProject.ORDER.common.BaseController;
import com.example.MyProject.REVIEW.service.ChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController extends BaseController {

    private final ChatbotService chatbotService;
    private final RedisRateLimiterService rateLimiterService;

    @PostMapping
    public ApiResponse<String> chat(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) {

        String message = request != null ? request.get("message") : null;

        if (message == null || message.isBlank()) {
            return successDataResponse(
                    "Vui lòng nhập câu hỏi!",
                    null
            );
        }

        String clientKey = resolveClientKey(request, httpRequest);
        if (!rateLimiterService.allowRequest("chat:" + clientKey, 15, Duration.ofMinutes(1))) {
            return successDataResponse(
                    "Tạm giới hạn tần suất",
                    "Bạn đang gửi tin nhắn hơi nhanh. Vui lòng đợi vài giây rồi hỏi tiếp để trợ lý HK COMPUTER phản hồi chính xác nhất nhé!"
            );
        }

        String response = chatbotService.chat(message, clientKey);

        return successDataResponse(
                "Chat thành công!",
                response
        );
    }

    private String resolveClientKey(Map<String, String> request, HttpServletRequest httpRequest) {
        String sessionId = request != null ? request.get("sessionId") : null;
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionId.trim().replaceAll("[^a-zA-Z0-9_-]", "");
        }
        if (httpRequest == null) {
            return "anonymous";
        }
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr() != null ? httpRequest.getRemoteAddr() : "anonymous";
    }
}
