package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.entity.WeatherConfigEntity;
import com.ticketingsystem.yuzhonblog.util.AesUtil;
import com.ticketingsystem.yuzhonblog.util.SsrfUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherTestService {

    private final AesUtil aesUtil;

    private final WebClient webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .build();

    public Map<String, Object> testConnection(WeatherConfigEntity entity) {
        String apiKey;
        try {
            apiKey = aesUtil.decrypt(entity.getApiKey());
        } catch (Exception e) {
            return Map.of("success", false, "message", "API Key 解密失败");
        }

        if (apiKey == null || apiKey.isBlank()) {
            return Map.of("success", false, "message", "API Key 未配置");
        }

        String provider = entity.getProvider();
        String baseUrl = entity.getBaseUrl();
        String language = entity.getLanguage() != null ? entity.getLanguage() : "zh";

        // Build test URL based on provider
        String testUrl = buildTestUrl(provider, baseUrl, apiKey, language, entity);
        if (testUrl == null) {
            return Map.of("success", false, "message", "不支持的供应商: " + provider);
        }

        // SSRF protection
        try {
            SsrfUtil.validateUrl(testUrl);
        } catch (Exception e) {
            return Map.of("success", false, "message", "URL 安全校验失败: " + e.getMessage());
        }

        long start = System.currentTimeMillis();
        try {
            String response = webClient.get()
                    .uri(testUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            long latency = System.currentTimeMillis() - start;
            boolean success = validateResponse(provider, response);

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("latency", latency);
            if (success) {
                result.put("message", "连接成功");
            } else {
                result.put("message", "API 返回异常，请检查配置");
            }
            return result;

        } catch (WebClientResponseException e) {
            long latency = System.currentTimeMillis() - start;
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("latency", latency);
            result.put("message", "HTTP " + e.getStatusCode().value() + ": " + e.getStatusText());
            return result;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("latency", latency);
            result.put("message", "连接失败: " + e.getMessage());
            return result;
        }
    }

    private String buildTestUrl(String provider, String baseUrl, String apiKey, String language, WeatherConfigEntity entity) {
        // Ensure baseUrl ends with /
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        return switch (provider) {
            case "qweather" -> baseUrl + "weather/now?location=101010100&key=" + apiKey + "&lang=" + language;
            case "openweathermap" -> baseUrl + "weather?q=Beijing&appid=" + apiKey + "&units=metric&lang=" + language;
            case "seniverse" -> baseUrl + "weather/now.json?key=" + apiKey + "&location=Beijing&language=" + language;
            case "custom" -> {
                String url = baseUrl;
                if ("query_param".equals(entity.getAuthType()) || entity.getAuthType() == null) {
                    url += (url.contains("?") ? "&" : "?") + "key=" + apiKey;
                }
                yield url;
            }
            default -> null;
        };
    }

    private boolean validateResponse(String provider, String response) {
        if (response == null || response.isBlank()) return false;
        return switch (provider) {
            case "qweather" -> response.contains("\"code\":\"200\"") || response.contains("\"code\": \"200\"");
            case "openweathermap" -> response.contains("\"cod\":200") || response.contains("\"cod\": 200");
            case "seniverse" -> response.contains("\"results\"");
            case "custom" -> true; // Any non-empty response is considered success
            default -> false;
        };
    }
}
