package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.util.SsrfUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceCheckService {

    private final ScriptBalanceParser scriptBalanceParser;

    private final WebClient webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .build();

    public record BalanceInfo(
            boolean success,
            String balance,
            String currency,
            String planType,
            String planName,
            boolean isAvailable,
            String message
    ) {
        public static BalanceInfo ok(String balance, String currency, boolean isAvailable) {
            return new BalanceInfo(true, balance, currency, "prepaid", null, isAvailable, "查询成功");
        }

        public static BalanceInfo plan(String planName, boolean isAvailable) {
            return new BalanceInfo(true, null, null, "subscription", planName, isAvailable, "查询成功");
        }

        public static BalanceInfo fail(String message) {
            return new BalanceInfo(false, null, null, null, null, false, message);
        }
    }

    public BalanceInfo check(String baseUrl, String apiKey, String authType, String balanceUrl, String balanceScript) {
        String lowerBaseUrl = baseUrl != null ? baseUrl.toLowerCase() : "";

        String url;
        String provider;
        if (balanceUrl != null && !balanceUrl.isBlank()) {
            url = balanceUrl;
            provider = "custom";
        } else if (lowerBaseUrl.contains("deepseek")) {
            url = appendPath(baseUrl, "/user/balance");
            provider = "deepseek";
        } else if (lowerBaseUrl.contains("moonshot") || lowerBaseUrl.contains("kimi")) {
            url = appendPath(baseUrl, "/v1/users/me/balance");
            provider = "kimi";
        } else if (lowerBaseUrl.contains("dashscope") || lowerBaseUrl.contains("aliyuncs")) {
            url = appendPath(baseUrl, "/api/v1/user/balance");
            provider = "dashscope";
        } else {
            return BalanceInfo.fail("该服务商不支持自动余额查询，请在高级设置中手动配置余额查询地址");
        }

        try {
            SsrfUtil.validateUrl(url);
            log.info("查询余额: provider={}, url={}", provider, url);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .header(buildAuthHeader(authType), buildAuthValue(authType, apiKey))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                return BalanceInfo.fail("余额查询返回空数据");
            }

            log.debug("余额查询响应: provider={}", provider);
            // Use custom parse script if provided
            if (balanceScript != null && !balanceScript.isBlank()) {
                ScriptBalanceParser.BalanceParseResult scriptResult = scriptBalanceParser.execute(balanceScript, response);
                if (scriptResult != null) {
                    return BalanceInfo.ok(scriptResult.balance(), scriptResult.currency(), scriptResult.isAvailable());
                }
                log.warn("自定义解析脚本执行失败，回退到内置解析");
            }
            return parseBalance(provider, response);

        } catch (WebClientResponseException e) {
            log.error("余额查询HTTP错误: status={}", e.getStatusCode().value());
            int status = e.getStatusCode().value();
            if (status == 401 || status == 403) {
                return BalanceInfo.fail("API Key 无效或已过期");
            }
            if (status == 404) {
                return BalanceInfo.fail("该服务商不支持余额查询，请手动配置余额查询地址");
            }
            return BalanceInfo.fail("查询失败: HTTP " + status);
        } catch (Exception e) {
            if (e.getClass().getSimpleName().contains("Timeout")
                    || (e.getCause() != null && e.getCause() instanceof java.util.concurrent.TimeoutException)) {
                return BalanceInfo.fail("查询超时，请稍后重试");
            }
            log.error("余额查询异常", e);
            return BalanceInfo.fail("查询失败，请稍后重试");
        }
    }

    @SuppressWarnings("unchecked")
    private BalanceInfo parseBalance(String provider, Map<String, Object> response) {
        try {
            return switch (provider) {
                case "deepseek" -> parseDeepSeek(response);
                case "kimi" -> parseKimi(response);
                case "dashscope" -> parseDashScope(response);
                default -> parseGeneric(response);
            };
        } catch (Exception e) {
            log.warn("解析余额响应失败: provider={}", provider, e);
            return BalanceInfo.fail("余额查询返回数据格式异常");
        }
    }

    @SuppressWarnings("unchecked")
    private BalanceInfo parseDeepSeek(Map<String, Object> response) {
        Map<String, Object> data = response;
        if (response.containsKey("data") && response.get("data") instanceof Map) {
            data = (Map<String, Object>) response.get("data");
        }

        if (response.containsKey("code") && !Integer.valueOf(0).equals(response.get("code"))) {
            String msg = response.containsKey("message") ? String.valueOf(response.get("message")) : "查询失败";
            return BalanceInfo.fail(msg);
        }

        boolean isAvailable = !data.containsKey("is_available") || Boolean.TRUE.equals(data.get("is_available"));

        if (data.containsKey("balance_infos") && data.get("balance_infos") instanceof List) {
            List<Map<String, Object>> infos = (List<Map<String, Object>>) data.get("balance_infos");
            if (!infos.isEmpty()) {
                Map<String, Object> info = infos.get(0);
                String balance = String.valueOf(info.getOrDefault("total_balance", "0"));
                String currency = info.containsKey("currency") ? String.valueOf(info.get("currency")) : "CNY";
                return BalanceInfo.ok(balance, currency, isAvailable);
            }
        }

        Object balanceObj = data.get("balance");
        if (balanceObj != null) {
            String balance = String.valueOf(balanceObj);
            String currency = data.containsKey("currency") ? String.valueOf(data.get("currency")) : "CNY";
            return BalanceInfo.ok(balance, currency, isAvailable);
        }

        return BalanceInfo.fail("余额数据为空");
    }

    @SuppressWarnings("unchecked")
    private BalanceInfo parseKimi(Map<String, Object> response) {
        if (response.containsKey("code") && !Integer.valueOf(0).equals(response.get("code"))) {
            String msg = response.containsKey("msg") ? String.valueOf(response.get("msg"))
                    : response.containsKey("message") ? String.valueOf(response.get("message")) : "查询失败";
            return BalanceInfo.fail(msg);
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) data = response;

        Object balanceObj = data.get("available_balance");
        if (balanceObj == null) balanceObj = data.get("balance");
        if (balanceObj == null) return BalanceInfo.fail("余额数据为空");

        return BalanceInfo.ok(String.valueOf(balanceObj), "CNY", true);
    }

    @SuppressWarnings("unchecked")
    private BalanceInfo parseDashScope(Map<String, Object> response) {
        if (response.containsKey("code") && !"200".equals(String.valueOf(response.get("code")))
                && !Integer.valueOf(0).equals(response.get("code"))) {
            String msg = response.containsKey("message") ? String.valueOf(response.get("message")) : "查询失败";
            return BalanceInfo.fail(msg);
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) data = response;

        Object balanceObj = data.get("available_balance");
        if (balanceObj == null) balanceObj = data.get("balance");
        String balance = balanceObj != null ? String.valueOf(balanceObj) : null;
        String currency = data.containsKey("currency") ? String.valueOf(data.get("currency")) : "CNY";

        if (data.containsKey("plan_type") || data.containsKey("plan_name")) {
            String planName = data.containsKey("plan_name") ? String.valueOf(data.get("plan_name")) : String.valueOf(data.get("plan_type"));
            return BalanceInfo.plan(planName, true);
        }

        if (balance != null) {
            return BalanceInfo.ok(balance, currency, true);
        }
        return BalanceInfo.fail("无法解析余额信息");
    }

    @SuppressWarnings("unchecked")
    private BalanceInfo parseGeneric(Map<String, Object> response) {
        if (response.containsKey("code") && !Integer.valueOf(0).equals(response.get("code"))) {
            String msg = response.containsKey("message") ? String.valueOf(response.get("message")) : "查询失败";
            return BalanceInfo.fail(msg);
        }

        Map<String, Object> data = response;
        if (response.containsKey("data") && response.get("data") instanceof Map) {
            data = (Map<String, Object>) response.get("data");
        }

        if (data.containsKey("balance")) {
            Object balanceObj = data.get("balance");
            if (balanceObj == null) return BalanceInfo.fail("余额数据为空");
            String balance = String.valueOf(balanceObj);
            String currency = data.containsKey("currency") ? String.valueOf(data.get("currency")) : null;
            boolean isAvailable = !data.containsKey("is_available") || Boolean.TRUE.equals(data.get("is_available"));
            return BalanceInfo.ok(balance, currency, isAvailable);
        }
        if (data.containsKey("plan_name") || data.containsKey("plan_type")) {
            String planName = data.containsKey("plan_name") ? String.valueOf(data.get("plan_name")) : String.valueOf(data.get("plan_type"));
            return BalanceInfo.plan(planName, true);
        }
        return BalanceInfo.fail("无法识别余额响应格式");
    }

    private String appendPath(String baseUrl, String path) {
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        return baseUrl + path;
    }

    private String buildAuthHeader(String authType) {
        if (authType == null) return HttpHeaders.AUTHORIZATION;
        return switch (authType) {
            case "X_API_KEY" -> "x-api-key";
            case "RAW_TOKEN" -> HttpHeaders.AUTHORIZATION;
            case "API_KEY" -> "api-key";
            default -> HttpHeaders.AUTHORIZATION;
        };
    }

    private String buildAuthValue(String authType, String apiKey) {
        if (authType == null) return "Bearer " + apiKey;
        return switch (authType) {
            case "BEARER" -> "Bearer " + apiKey;
            default -> apiKey;
        };
    }
}
