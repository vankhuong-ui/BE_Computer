package com.example.MyProject.REVIEW.controller;

import com.example.MyProject.ORDER.common.ApiResponse;
import com.example.MyProject.ORDER.common.BaseController;
import com.example.MyProject.REVIEW.service.ChatbotService;
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
            @RequestBody Map<String, String> request
    ) {

        String message = request.get("message");

        if (message == null || message.isBlank()) {
            return successDataResponse(
                    "Vui lòng nhập câu hỏi!",
                    null
            );
        }

        String response = chatbotService.chat(message);

        return successDataResponse(
                "Chat thành công!",
                response
        );
    }
}
