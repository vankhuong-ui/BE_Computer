package com.example.MyProject.REVIEW.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 3-layer content guardrail (input validation + output sanitization) for HK COMPUTER chatbot.
 */
@Slf4j
@Component
public class ChatbotGuardrail {

    public static final String DEFAULT_BLOCKED_COMPETITORS =
            "cellphones,gearvn,phong vu,phongvu,the gioi di dong,tgdd,fpt shop,fptshop,"
                    + "an phat,anphat,hacom,hanoi computer,hanoicomputer,nguyen kim,dien may xanh,"
                    + "shopee,lazada,tiki,sendo,tiktok shop,thinkpro,laptop88,phuc anh,phucanh,"
                    + "nguyen cong,nguyencong,hotgear,xgear,minh an";

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    private static final List<String> INJECTION_OR_OFFTOPIC_PHRASES = List.of(
            "bo qua huong dan",
            "bo qua moi huong dan",
            "bo qua tat ca",
            "ignore previous",
            "ignore all",
            "system prompt",
            "reveal prompt",
            "in ra prompt",
            "tiet lo prompt",
            "xem prompt",
            "dong vai",
            "gia vo la",
            "jailbreak",
            "tu gio hay tra loi",
            "api key",
            "mat khau database",
            "viet code",
            "giai bai tap"
    );

    private static final List<String> COMPETITOR_INPUT_PHRASES = List.of(
            "cho khac re hon",
            "ben khac re hon",
            "noi khac re hon",
            "shop khac re hon",
            "cua hang khac re hon",
            "mua o dau re hon",
            "so sanh voi shop khac",
            "so sanh gia voi",
            "ban dat hon",
            "gia cao hon ben khac"
    );

    private static final List<String> FORBIDDEN_OUTPUT_PHRASES = List.of(
            "noi khac re hon",
            "cho khac re hon",
            "ben khac re hon",
            "shop khac re hon",
            "cua hang khac re hon",
            "mua o cua hang khac",
            "mua o noi khac",
            "mua o cho khac",
            "mua o ben khac",
            "tham khao cua hang khac",
            "tham khao noi khac",
            "gia re hon o",
            "system prompt",
            "groq",
            "llama",
            "openai",
            "as an ai",
            "toi la mo hinh ngon ngu",
            "mo hinh ai"
    );

    private static final String DEFAULT_SAFE_REPLY =
            "HK COMPUTER chuyên cung cấp sản phẩm công nghệ chính hãng, đi kèm bảo hành điện tử 12–36 tháng "
                    + "và hỗ trợ sửa chữa tại cửa hàng lẫn tận nơi. Bạn cho mình biết sản phẩm hoặc tầm giá đang quan tâm để mình tư vấn nhé!";

    private static final String SCOPE_REFUSAL_REPLY =
            "Mình là trợ lý bán hàng của HK COMPUTER, chỉ hỗ trợ tư vấn sản phẩm, kiểm tra tồn kho, "
                    + "bảo hành điện tử, sửa chữa và thanh toán tại cửa hàng thôi ạ. Bạn cần tìm thiết bị hoặc dịch vụ nào cứ nhắn mình nhé!";

    private static final String LOYAL_PRICE_REPLY =
            "Tại HK COMPUTER, mọi sản phẩm đều là hàng chính hãng với giá niêm yết minh bạch, "
                    + "đi kèm bảo hành điện tử 12–36 tháng và hỗ trợ sửa chữa tại cửa hàng lẫn tận nơi. "
                    + "Bạn cho mình biết sản phẩm hoặc tầm giá mong muốn để mình gợi ý lựa chọn phù hợp nhất tại HK COMPUTER nhé!";

    private final List<String> blockedCompetitors;

    @Autowired
    public ChatbotGuardrail(
            @Value("${chatbot.blocked-competitors:" + DEFAULT_BLOCKED_COMPETITORS + "}")
            String blockedCompetitorsCsv) {
        this.blockedCompetitors = parseCompetitors(blockedCompetitorsCsv);
    }

    public ChatbotGuardrail() {
        this(DEFAULT_BLOCKED_COMPETITORS);
    }

    /**
     * Layer 1 check: validates user message length, blocks prompt injection and competitor comparisons.
     */
    public Optional<String> checkInput(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return Optional.of("Vui lòng nhập câu hỏi để HK COMPUTER hỗ trợ bạn nhé!");
        }
        String trimmed = rawMessage.trim();
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            return Optional.of(
                    "Câu hỏi hơi dài rồi ạ! Bạn vui lòng nhập ngắn gọn dưới "
                            + MAX_MESSAGE_LENGTH
                            + " ký tự để HK COMPUTER hỗ trợ nhanh hơn nhé.");
        }

        String alnum = toAlnumSpace(normalize(trimmed));
        if (containsAnyPhrase(alnum, INJECTION_OR_OFFTOPIC_PHRASES)) {
            log.warn("Chatbot input blocked by prompt-injection/off-topic guardrail");
            return Optional.of(SCOPE_REFUSAL_REPLY);
        }

        if (containsBlockedCompetitor(alnum) || containsAnyPhrase(alnum, COMPETITOR_INPUT_PHRASES)) {
            log.info("Chatbot input redirected by competitor/price-comparison guardrail");
            return Optional.of(LOYAL_PRICE_REPLY);
        }

        return Optional.empty();
    }

    /**
     * Layer 3 check: ensures the AI output does not leak competitors, internal details, or forbidden phrases.
     */
    public String sanitizeOutput(String aiReply, String safeFallbackReply) {
        String safeBackup = resolveSafeFallback(safeFallbackReply);
        if (aiReply == null || aiReply.isBlank()) {
            return safeBackup;
        }

        String normalized = normalize(aiReply);
        String alnum = toAlnumSpace(normalized);

        if (normalized.contains("gsk_")
                || containsBlockedCompetitor(alnum)
                || containsAnyPhrase(alnum, FORBIDDEN_OUTPUT_PHRASES)) {
            log.warn("Chatbot AI reply blocked by output guardrail; substituting safe fallback");
            return safeBackup;
        }

        return aiReply.trim();
    }

    /**
     * Normalizes text to accent-free lowercase Vietnamese for consistent matching.
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String replaced = text.replace('đ', 'd').replace('Đ', 'D');
        String decomposed = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        return COMBINING_MARKS.matcher(decomposed)
                .replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String toAlnumSpace(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return "";
        }
        return normalizedText
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> parseCompetitors(String csv) {
        String source = (csv == null || csv.isBlank()) ? DEFAULT_BLOCKED_COMPETITORS : csv;
        return Arrays.stream(source.split(","))
                .map(ChatbotGuardrail::normalize)
                .map(ChatbotGuardrail::toAlnumSpace)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private boolean containsBlockedCompetitor(String alnumText) {
        return containsAnyPhrase(alnumText, blockedCompetitors);
    }

    private boolean containsAnyPhrase(String alnumText, List<String> phrases) {
        if (alnumText == null || alnumText.isBlank()) {
            return false;
        }
        String paddedText = " " + alnumText + " ";
        for (String phrase : phrases) {
            if (!phrase.isBlank() && paddedText.contains(" " + phrase + " ")) {
                return true;
            }
        }
        return false;
    }

    private String resolveSafeFallback(String safeFallbackReply) {
        if (safeFallbackReply == null || safeFallbackReply.isBlank()) {
            return DEFAULT_SAFE_REPLY;
        }
        String normalizedFallback = normalize(safeFallbackReply);
        String alnumFallback = toAlnumSpace(normalizedFallback);
        if (normalizedFallback.contains("gsk_")
                || containsBlockedCompetitor(alnumFallback)
                || containsAnyPhrase(alnumFallback, FORBIDDEN_OUTPUT_PHRASES)) {
            return DEFAULT_SAFE_REPLY;
        }
        return safeFallbackReply.trim();
    }
}
