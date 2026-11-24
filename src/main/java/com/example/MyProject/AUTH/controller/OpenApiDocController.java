package com.example.MyProject.AUTH.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

/**
 * Lightweight, zero-dependency OpenAPI 3.0.3 & Swagger UI controller compatible with Spring Boot 4.1 / Spring 7.
 */
@RestController
public class OpenApiDocController {

    private final RequestMappingHandlerMapping handlerMapping;

    public OpenApiDocController(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping
    ) {
        this.handlerMapping = handlerMapping;
    }

    @GetMapping(value = "/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> openApiSpec() {
        Map<String, Map<String, Object>> paths = new TreeMap<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod method = entry.getValue();
            String pkg = method.getBeanType().getPackageName();
            if (!pkg.startsWith("com.example.MyProject")) {
                continue;
            }

            Set<String> patterns = info.getPatternValues();
            Set<RequestMethod> httpMethods = info.getMethodsCondition().getMethods();
            if (httpMethods.isEmpty()) {
                httpMethods = Set.of(RequestMethod.GET);
            }

            String tag = resolveModuleTag(pkg, method.getBeanType().getSimpleName());

            for (String pattern : patterns) {
                if ("/v3/api-docs".equals(pattern) || "/swagger-ui.html".equals(pattern)) {
                    continue;
                }
                Map<String, Object> operations = paths.computeIfAbsent(pattern, k -> new LinkedHashMap<>());
                for (RequestMethod rm : httpMethods) {
                    Map<String, Object> op = new LinkedHashMap<>();
                    op.put("tags", List.of(tag));
                    op.put("summary", method.getBeanType().getSimpleName() + "." + method.getMethod().getName());
                    op.put("operationId", method.getBeanType().getSimpleName() + "_" + method.getMethod().getName() + "_" + rm.name());

                    List<Map<String, Object>> params = new ArrayList<>();
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{([^}]+)}").matcher(pattern);
                    while (m.find()) {
                        params.add(Map.of(
                                "name", m.group(1),
                                "in", "path",
                                "required", true,
                                "schema", Map.of("type", "string")
                        ));
                    }
                    if (!params.isEmpty()) {
                        op.put("parameters", params);
                    }

                    if (rm == RequestMethod.POST || rm == RequestMethod.PUT || rm == RequestMethod.PATCH) {
                        op.put("requestBody", Map.of(
                                "required", false,
                                "content", Map.of("application/json", Map.of("schema", Map.of("type", "object")))
                        ));
                    }

                    op.put("responses", Map.of(
                            "200", Map.of("description", "Success"),
                            "400", Map.of("description", "Validation / Business Error"),
                            "401", Map.of("description", "Unauthorized")
                    ));
                    op.put("security", List.of(Map.of("bearerAuth", List.of())));
                    operations.put(rm.name().toLowerCase(Locale.ROOT), op);
                }
            }
        }

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.3");
        spec.put("info", Map.of(
                "title", "HK COMPUTER — Multi-Branch E-Commerce & After-Sales API",
                "version", "2.0.0",
                "description", "Tài liệu REST API trực quan của hệ thống HK COMPUTER (Auth, Catalog, Multi-Branch Inventory, Voucher, Orders, SePay/VNPay, Hybrid AI Chatbot, Electronic Warranty & Repair Booking)."
        ));
        spec.put("paths", paths);
        spec.put("components", Map.of(
                "securitySchemes", Map.of(
                        "bearerAuth", Map.of(
                                "type", "http",
                                "scheme", "bearer",
                                "bearerFormat", "JWT"
                        )
                )
        ));
        return spec;
    }

    @GetMapping(value = "/swagger-ui.html", produces = MediaType.TEXT_HTML_VALUE)
    public String swaggerUiHtml() {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>HK COMPUTER — Swagger API Explorer</title>
                    <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui.css" />
                    <style>
                        body { margin: 0; background: #F4F3EF; }
                        .topbar { display: none; }
                    </style>
                </head>
                <body>
                    <div id="swagger-ui"></div>
                    <script src="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui-bundle.js"></script>
                    <script>
                        window.onload = () => {
                            SwaggerUIBundle({
                                url: '/v3/api-docs',
                                dom_id: '#swagger-ui',
                                deepLinking: true,
                                persistAuthorization: true
                            });
                        };
                    </script>
                </body>
                </html>
                """;
    }

    private String resolveModuleTag(String pkg, String controllerName) {
        if (pkg.contains(".AUTH")) return "1. AUTH — Tài khoản & Sổ địa chỉ";
        if (pkg.contains(".PRODUCT")) return "2. PRODUCT — Sản phẩm, Chi nhánh & Tồn kho";
        if (pkg.contains(".ORDER")) return "3. ORDER — Giỏ hàng, Voucher, Đơn hàng & Thanh toán";
        if (pkg.contains(".REVIEW")) return "4. REVIEW — AI Chatbot, Bảo hành, Sửa chữa & Đánh giá";
        return controllerName;
    }
}
