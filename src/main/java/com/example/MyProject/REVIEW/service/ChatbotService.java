package com.example.MyProject.REVIEW.service;

public interface ChatbotService {

    String chat(String message);

    default String chat(String message, String sessionKey) {
        return chat(message);
    }

    default String chatFromClient(String message, String sessionId, String clientIp) {
        return chat(message, sessionId != null && !sessionId.isBlank() ? sessionId : clientIp);
    }
}
