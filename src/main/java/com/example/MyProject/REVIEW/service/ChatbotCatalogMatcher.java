package com.example.MyProject.REVIEW.service;

import com.example.MyProject.PRODUCT.entity.Inventory;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.service.InventoryService;
import com.example.MyProject.PRODUCT.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ChatbotCatalogMatcher {

    private static final int MAX_SEARCH_RESULTS = 8;
    private static final int MAX_LIST_RESULTS = 15;
    private static final int MAX_COMPACT_CATALOG_SIZE = 20;

    private static final Pattern RANGE_PRICE_PATTERN = Pattern.compile(
            "(?:\\btu\\s+)?(\\d+(?:[.,]\\d+)?)\\s*(?:(trieu|tr|cu|nghin|ngan|k)\\b)?\\s*(?:den|toi|-|~)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|cu|nghin|ngan|k)\\b"
    );
    private static final Pattern MAX_PRICE_PATTERN = Pattern.compile(
            "(?:\\b(?:duoi|toi da|khong qua|it hon|re hon)\\b|<=|<)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|cu|nghin|ngan|k)\\b"
    );
    private static final Pattern MIN_PRICE_PATTERN = Pattern.compile(
            "(?:\\b(?:tren|tu|hon)\\b|>=|>)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|cu|nghin|ngan|k)\\b(?:\\s*tro len)?"
    );
    private static final Pattern APPROX_PRICE_PATTERN = Pattern.compile(
            "\\b(?:tam gia|muc gia|tam|khoang)\\b\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|cu|nghin|ngan|k)\\b"
    );

    private static final Set<String> GENERIC_PRODUCT_WORDS = Set.of("may", "san", "pham", "thiet", "bi");

    private static final Set<String> STOP_WORDS = Set.copyOf(Arrays.asList(
            "toi", "minh", "em", "anh", "chi", "ban", "shop", "cua", "hang", "hk", "computer",
            "oi", "ad", "admin", "cho", "hoi", "xin", "vui", "long", "tu", "van", "tim", "kiem",
            "muon", "mua", "can", "xem", "co", "khong", "ko", "hong", "nhung", "nao", "gi", "the",
            "sao", "vay", "nhi", "nhe", "nha", "ah", "ak", "uh", "la", "va", "voi", "hay", "hoac",
            "de", "duoc", "dc", "nay", "kia", "do", "day", "con", "het", "ton", "kho", "bao",
            "nhieu", "gia", "tien", "vnd", "dong", "trieu", "tr", "cu", "nghin", "ngan", "duoi",
            "tren", "den", "tam", "khoang", "muc", "san", "pham", "sp", "loai", "mau", "chiec",
            "cai", "bo", "thuong", "hieu", "re", "nhat", "tot", "chat", "luong", "chinh", "hien",
            "tai", "trong", "he", "thong", "ve", "tin", "tiet", "so", "tinh", "trang", "hanh",
            "sua", "chua", "thanh", "toan", "chao", "hello", "hi", "alo", "cam", "on", "thank",
            "thanks", "you", "da", "biet", "gioi", "thieu", "danh", "sach", "tat", "ca", "cac",
            "mot", "vai", "giup", "sach", "che", "quy", "trinh", "thuc", "phuong", "dich", "vu",
            "dat", "lich", "tra", "cuu", "yeu", "cau", "huong", "dan", "thoi", "gian", "dia",
            "qua", "sepay", "vietqr", "vnpay", "cod", "serial", "dien", "chuyen", "khoan",
            "quet", "ma", "qr", "mat", "tan", "noi", "nha", "ky", "thuat", "vien", "win", "sinh",
            "loi", "phan", "cung", "doi", "ben", "web", "website", "don", "lam", "mo", "vo",
            "vao", "di", "trang", "gio", "ngay", "ho", "tro", "chuc", "nang", "cao", "thap",
            "it", "nho", "khac", "nua", "xong", "di"
    ));

    private final ProductService productService;
    private final InventoryService inventoryService;

    public List<ProductWithStock> loadCompactCatalogIfSmall() {
        List<Products> all = productService.findAllCatalogProductsWithBrandAndCategory();
        if (all == null || all.isEmpty() || all.size() > MAX_COMPACT_CATALOG_SIZE) {
            return Collections.emptyList();
        }
        return attachStocks(all.stream().limit(MAX_LIST_RESULTS).toList());
    }

    public List<ProductWithStock> findMatchingProducts(
            List<String> searchTokens,
            PriceRange priceRange,
            boolean isProductList,
            boolean isMostExpensive,
            boolean isCheapest) {

        List<Products> allProducts = productService.findAllCatalogProductsWithBrandAndCategory();
        if (allProducts == null || allProducts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Products> priceFiltered = allProducts.stream()
                .filter(p -> priceRange == null || priceRange.matches(p.getPrice()))
                .toList();

        if (priceFiltered.isEmpty()) {
            return Collections.emptyList();
        }

        boolean isPriceExtremum = isMostExpensive || isCheapest;
        boolean onlyGenericTokens = !searchTokens.isEmpty()
                && searchTokens.stream().allMatch(GENERIC_PRODUCT_WORDS::contains);

        List<Products> candidates;
        if (searchTokens.isEmpty() || (isPriceExtremum && onlyGenericTokens)) {
            candidates = priceFiltered;
        } else {
            List<ScoredProduct> scored = new ArrayList<>();
            for (Products product : priceFiltered) {
                ScoredProduct sp = scoreProduct(product, searchTokens);
                if (sp.matchedTokens() > 0) {
                    scored.add(sp);
                }
            }

            if (scored.isEmpty()) {
                return Collections.emptyList();
            }

            List<ScoredProduct> primaryMatches = scored.stream()
                    .filter(sp -> sp.primaryMatchedTokens() > 0)
                    .toList();
            List<ScoredProduct> pool = !primaryMatches.isEmpty() ? primaryMatches : scored;

            List<ScoredProduct> fullMatches = pool.stream()
                    .filter(sp -> sp.matchedTokens() == searchTokens.size())
                    .toList();

            List<ScoredProduct> chosen = !fullMatches.isEmpty() ? fullMatches : pool;

            if (!isPriceExtremum) {
                chosen = chosen.stream()
                        .sorted(Comparator
                                .comparingInt(ScoredProduct::primaryMatchedTokens).reversed()
                                .thenComparing(Comparator.comparingInt(ScoredProduct::matchedTokens).reversed())
                                .thenComparing(Comparator.comparingInt(ScoredProduct::score).reversed()))
                        .limit(MAX_SEARCH_RESULTS)
                        .toList();
            }

            candidates = chosen.stream().map(ScoredProduct::product).toList();
        }

        List<Products> selected;
        if (isMostExpensive) {
            selected = candidates.stream()
                    .sorted(Comparator.comparing(
                            Products::getPrice,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(MAX_SEARCH_RESULTS)
                    .toList();
        } else if (isCheapest) {
            selected = candidates.stream()
                    .sorted(Comparator.comparing(
                            Products::getPrice,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .limit(MAX_SEARCH_RESULTS)
                    .toList();
        } else {
            int limit = isProductList ? MAX_LIST_RESULTS : MAX_SEARCH_RESULTS;
            selected = candidates.stream().limit(limit).toList();
        }

        return attachStocks(selected);
    }

    private List<ProductWithStock> attachStocks(List<Products> selected) {
        List<Long> ids = selected.stream()
                .map(Products::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Integer> stockMap = new HashMap<>();
        Map<Long, Map<String, Integer>> cityStockMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<Inventory> inventories = inventoryService.findInventoriesByProductIds(ids);
            if (inventories != null) {
                for (Inventory inv : inventories) {
                    if (inv.getProducts() != null && inv.getProducts().getId() != null) {
                        if (inv.getBranch() != null && !inv.getBranch().isActive()) {
                            continue;
                        }
                        int qty = Math.max(0, inv.getSoluong());
                        Long pid = inv.getProducts().getId();
                        stockMap.merge(pid, qty, Integer::sum);
                        if (inv.getBranch() != null && inv.getBranch().getCity() != null && !inv.getBranch().getCity().isBlank()) {
                            cityStockMap.computeIfAbsent(pid, k -> new LinkedHashMap<>())
                                    .merge(inv.getBranch().getCity().trim(), qty, Integer::sum);
                        }
                    }
                }
            }
        }

        return selected.stream()
                .map(p -> new ProductWithStock(
                        p,
                        stockMap.getOrDefault(p.getId(), 0),
                        cityStockMap.getOrDefault(p.getId(), Collections.emptyMap())
                ))
                .toList();
    }

    private ScoredProduct scoreProduct(Products product, List<String> tokens) {
        String name = ChatbotGuardrail.toAlnumSpace(ChatbotGuardrail.normalize(product.getNameProduct()));
        String brand = product.getBrands() != null
                ? ChatbotGuardrail.toAlnumSpace(ChatbotGuardrail.normalize(product.getBrands().getNameBrands()))
                : "";
        String category = product.getCategories() != null
                ? ChatbotGuardrail.toAlnumSpace(ChatbotGuardrail.normalize(product.getCategories().getNameCategories()))
                : "";
        String specs = ChatbotGuardrail.toAlnumSpace(ChatbotGuardrail.normalize(product.getSpecs()));
        String desc = ChatbotGuardrail.toAlnumSpace(ChatbotGuardrail.normalize(product.getDescription()));

        int matchedCount = 0;
        int primaryMatchedCount = 0;
        int totalScore = 0;

        for (String token : tokens) {
            int tokenScore = 0;
            boolean inPrimary = false;
            if (containsWordOrSub(name, token)) {
                tokenScore += 4;
                inPrimary = true;
            }
            if (containsWordOrSub(brand, token) || containsWordOrSub(category, token)) {
                tokenScore += 2;
                inPrimary = true;
            }
            if (containsWordOrSub(specs, token) || containsWordOrSub(desc, token)) {
                tokenScore += 1;
            }
            if (tokenScore > 0) {
                matchedCount++;
                totalScore += tokenScore;
                if (inPrimary) {
                    primaryMatchedCount++;
                }
            }
        }

        return new ScoredProduct(product, primaryMatchedCount, matchedCount, totalScore);
    }

    private boolean containsWordOrSub(String fieldText, String token) {
        return !fieldText.isBlank() && !token.isBlank() && fieldText.contains(token);
    }

    public String detectRequestedCity(String alnum) {
        if (alnum == null || alnum.isBlank()) {
            return null;
        }
        if (containsAnyPhrase(alnum, "ha noi", "thai ha", "dong da", "hn")) {
            return "Hà Nội";
        }
        if (containsAnyPhrase(alnum, "tp hcm", "tphcm", "ho chi minh", "sai gon", "quan 10", "hcm")) {
            return "TP.HCM";
        }
        if (containsAnyPhrase(alnum, "da nang", "hai chau", "thanh khe", "dn")) {
            return "Đà Nẵng";
        }
        return null;
    }

    public String stripCityPhrases(String alnum) {
        if (alnum == null || alnum.isBlank()) {
            return "";
        }
        return (" " + alnum + " ")
                .replaceAll("\\s+(ha noi|thai ha|dong da|tp hcm|tphcm|ho chi minh|sai gon|quan 10|da nang|hai chau|thanh khe|chi nhanh|co so|khu vuc|o|tai)\\s+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public List<String> extractSearchTokens(String alnumText) {
        if (alnumText == null || alnumText.isBlank()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : alnumText.split("\\s+")) {
            if (token.length() < 2 || STOP_WORDS.contains(token) || token.matches("^\\d+$")) {
                continue;
            }
            tokens.add(token);
        }
        return tokens.stream().distinct().toList();
    }

    public PriceExtraction extractPriceRange(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return new PriceExtraction(null, "");
        }

        Matcher rangeMatcher = RANGE_PRICE_PATTERN.matcher(normalized);
        if (rangeMatcher.find()) {
            String endUnit = rangeMatcher.group(4);
            String startUnit = rangeMatcher.group(2) != null ? rangeMatcher.group(2) : endUnit;
            BigDecimal min = parseAmount(rangeMatcher.group(1), startUnit);
            BigDecimal max = parseAmount(rangeMatcher.group(3), endUnit);
            if (min != null && max != null && min.compareTo(max) > 0) {
                BigDecimal tmp = min;
                min = max;
                max = tmp;
            }
            String remaining = replaceMatch(normalized, rangeMatcher.start(), rangeMatcher.end());
            return new PriceExtraction(new PriceRange(min, max), remaining);
        }

        Matcher maxMatcher = MAX_PRICE_PATTERN.matcher(normalized);
        if (maxMatcher.find()) {
            BigDecimal max = parseAmount(maxMatcher.group(1), maxMatcher.group(2));
            String remaining = replaceMatch(normalized, maxMatcher.start(), maxMatcher.end());
            return new PriceExtraction(new PriceRange(null, max), remaining);
        }

        Matcher minMatcher = MIN_PRICE_PATTERN.matcher(normalized);
        if (minMatcher.find()) {
            BigDecimal min = parseAmount(minMatcher.group(1), minMatcher.group(2));
            String remaining = replaceMatch(normalized, minMatcher.start(), minMatcher.end());
            return new PriceExtraction(new PriceRange(min, null), remaining);
        }

        Matcher approxMatcher = APPROX_PRICE_PATTERN.matcher(normalized);
        if (approxMatcher.find()) {
            BigDecimal center = parseAmount(approxMatcher.group(1), approxMatcher.group(2));
            if (center != null) {
                BigDecimal min = center.multiply(BigDecimal.valueOf(0.75));
                BigDecimal max = center.multiply(BigDecimal.valueOf(1.25));
                String remaining = replaceMatch(normalized, approxMatcher.start(), approxMatcher.end());
                return new PriceExtraction(new PriceRange(min, max), remaining);
            }
        }

        return new PriceExtraction(null, normalized);
    }

    private String replaceMatch(String text, int start, int end) {
        return (text.substring(0, start) + " " + text.substring(end)).replaceAll("\\s+", " ").trim();
    }

    private BigDecimal parseAmount(String numberRaw, String unit) {
        if (numberRaw == null || unit == null) {
            return null;
        }
        try {
            BigDecimal number = new BigDecimal(numberRaw.replace(',', '.'));
            return switch (unit) {
                case "tr", "trieu", "cu" -> number.multiply(BigDecimal.valueOf(1_000_000L));
                case "k", "nghin", "ngan" -> number.multiply(BigDecimal.valueOf(1_000L));
                default -> number;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean containsAnyPhrase(String alnumText, String... phrases) {
        if (alnumText == null || alnumText.isBlank()) {
            return false;
        }
        String padded = " " + alnumText + " ";
        for (String phrase : phrases) {
            if (padded.contains(" " + phrase + " ")) {
                return true;
            }
        }
        return false;
    }

    public record PriceRange(BigDecimal min, BigDecimal max) {
        public boolean matches(BigDecimal price) {
            if (price == null) {
                return false;
            }
            if (min != null && price.compareTo(min) < 0) {
                return false;
            }
            return max == null || price.compareTo(max) <= 0;
        }

        public String describe() {
            NumberFormat nf = NumberFormat.getIntegerInstance(Locale.of("vi", "VN"));
            if (min != null && max != null) {
                return "từ " + nf.format(min.longValue()) + "₫ đến " + nf.format(max.longValue()) + "₫";
            }
            if (max != null) {
                return "dưới " + nf.format(max.longValue()) + "₫";
            }
            if (min != null) {
                return "trên " + nf.format(min.longValue()) + "₫";
            }
            return "";
        }
    }

    public record PriceExtraction(PriceRange range, String remainingText) {
    }

    private record ScoredProduct(Products product, int primaryMatchedTokens, int matchedTokens, int score) {
    }

    public record ProductWithStock(Products product, int stock, Map<String, Integer> stockByCity) {
    }
}
