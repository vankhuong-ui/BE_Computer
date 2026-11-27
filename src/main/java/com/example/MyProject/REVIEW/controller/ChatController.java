package com.example.MyProject.REVIEW.controller;

import com.example.MyProject.common.ApiResponse;
import com.example.MyProject.common.BaseController;
import com.example.MyProject.REVIEW.service.ChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController extends BaseController {

    private final ChatbotService chatbotService;

    @PostMapping
    public ApiResponse<String> chat(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        String message = request != null ? request.get("message") : null;
        if (message == null || message.isBlank()) {
            return successDataResponse("Vui lòng nhập câu hỏi!", null);
        }

        String sessionId = request.get("sessionId");
        String forwarded = httpRequest != null ? httpRequest.getHeader("X-Forwarded-For") : null;
        String clientIp = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : (httpRequest != null ? httpRequest.getRemoteAddr() : null);

        String response = chatbotService.chatFromClient(message, sessionId, clientIp);
        return successDataResponse("Chat thành công!", response);
    }
}
