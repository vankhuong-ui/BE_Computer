package com.example.MyProject.AUTH.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Redis-backed sliding/fixed-window rate limiter and short chatbot conversation history store.
 * Fails open gracefully if Redis is temporarily unavailable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimiterService {

    private static final int MAX_CHAT_HISTORY_TURNS = 6; // 3 user + 3 assistant messages
    private static final Duration CHAT_HISTORY_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public boolean allowRequest(String bucketKey, int maxRequests, Duration window) {
        if (bucketKey == null || bucketKey.isBlank() || maxRequests <= 0) {
            return true;
        }
        try {
            String key = "rl:" + bucketKey.trim();
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, window);
            }
            return count == null || count <= maxRequests;
        } catch (Exception e) {
            log.debug("Redis rate limiter bypassed due to connection issue: {}", e.getMessage());
            return true;
        }
    }

    public List<String> getChatHistory(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return Collections.emptyList();
        }
        try {
            String key = "chat:hist:" + sessionKey.trim();
            List<String> items = redisTemplate.opsForList().range(key, 0, -1);
            return items != null ? items : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void appendChatExchange(String sessionKey, String userMsg, String assistantMsg) {
        if (sessionKey == null || sessionKey.isBlank() || userMsg == null || assistantMsg == null) {
            return;
        }
        try {
            String key = "chat:hist:" + sessionKey.trim();
            String compactUser = "Khách: " + truncate(userMsg.replaceAll("\\s+", " ").trim(), 180);
            String compactAssistant = "HK: " + truncate(assistantMsg.replaceAll("\\s+", " ").trim(), 220);
            redisTemplate.opsForList().rightPushAll(key, compactUser, compactAssistant);
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_CHAT_HISTORY_TURNS) {
                redisTemplate.opsForList().trim(key, size - MAX_CHAT_HISTORY_TURNS, -1);
            }
            redisTemplate.expire(key, CHAT_HISTORY_TTL);
        } catch (Exception e) {
            log.debug("Could not persist chat history to Redis: {}", e.getMessage());
        }
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
