package com.ticketingsystem.yuzhonblog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.entity.WeatherConfigEntity;
import com.ticketingsystem.yuzhonblog.repository.WeatherConfigRepository;
import com.ticketingsystem.yuzhonblog.util.AesUtil;
import com.ticketingsystem.yuzhonblog.util.SsrfUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherConfigRepository weatherConfigRepository;
    private final AesUtil aesUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate = createRestTemplate();

    private String safeGet(String url) {
        SsrfUtil.validateUrl(url);
        return restTemplate.getForObject(url, String.class);
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("天气 API 返回空响应");
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException("JSON 解析失败", e);
        }
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    /**
     * 获取天气数据（统一封装）
     * 有激活配置 → 用配置的供应商
     * 无激活配置 → 回退到 Open-Meteo（免费）
     */
    public Map<String, Object> fetchWeather(double lat, double lon) {
        Optional<WeatherConfigEntity> activeOpt = weatherConfigRepository.findFirstByIsActiveTrue();

        if (activeOpt.isPresent()) {
            WeatherConfigEntity config = activeOpt.get();
            try {
                return fetchFromProvider(config, lat, lon);
            } catch (Exception e) {
                log.warn("配置的天气供应商 {} 失败，回退到 Open-Meteo: {}", config.getProvider(), e.getMessage());
                return fetchOpenMeteo(lat, lon);
            }
        }

        return fetchOpenMeteo(lat, lon);
    }

    private Map<String, Object> fetchFromProvider(WeatherConfigEntity config, double lat, double lon) {
        String apiKey;
        try {
            apiKey = aesUtil.decrypt(config.getApiKey());
        } catch (Exception e) {
            throw new RuntimeException("API Key 解密失败", e);
        }

        return switch (config.getProvider()) {
            case "qweather" -> fetchQWeather(config, apiKey, lat, lon);
            case "openweathermap" -> fetchOpenWeatherMap(config, apiKey, lat, lon);
            case "seniverse" -> fetchSeniverse(config, apiKey, lat, lon);
            case "custom" -> fetchCustom(config, apiKey, lat, lon);
            default -> throw new RuntimeException("不支持的供应商: " + config.getProvider());
        };
    }

    // ==================== Open-Meteo（默认回退）====================

    private Map<String, Object> fetchOpenMeteo(double lat, double lon) {
        String url = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s" +
                "&current=temperature_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min,weather_code" +
                "&timezone=Asia/Shanghai&forecast_days=5",
                lat, lon);

        String raw = safeGet(url);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "open-meteo");
        result.put("providerName", "Open-Meteo");
        result.put("providerUrl", "https://open-meteo.com");
        result.put("raw", raw);
        return result;
    }

    // ==================== 和风天气 ====================

    private Map<String, Object> fetchQWeather(WeatherConfigEntity config, String apiKey, double lat, double lon) {
        String baseUrl = config.getBaseUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        String lang = config.getLanguage() != null ? config.getLanguage() : "zh";

        // 当前天气
        String currentUrl = baseUrl + "weather/now?location=" + lon + "," + lat + "&key=" + apiKey + "&lang=" + lang;
        String currentRaw = safeGet(currentUrl);
        JsonNode currentJson = parseJson(currentRaw);

        if (!"200".equals(currentJson.path("code").asText())) {
            throw new RuntimeException("和风天气返回错误: " + currentJson.path("code").asText());
        }
        JsonNode now = currentJson.path("now");

        // 3天预报
        String dailyUrl = baseUrl + "weather/3d?location=" + lon + "," + lat + "&key=" + apiKey + "&lang=" + lang;
        String dailyRaw = safeGet(dailyUrl);
        JsonNode dailyJson = parseJson(dailyRaw);

        // 构建统一格式
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("temperature_2m", now.path("temp").asDouble());
        current.put("weather_code", mapQWeatherCode(now.path("text").asText(), now.path("code").asText()));

        List<String> times = new ArrayList<>();
        List<Double> maxTemps = new ArrayList<>();
        List<Double> minTemps = new ArrayList<>();
        List<Integer> codes = new ArrayList<>();

        if (dailyJson.has("daily")) {
            for (JsonNode day : dailyJson.path("daily")) {
                times.add(day.path("fxDate").asText());
                maxTemps.add(day.path("tempMax").asDouble());
                minTemps.add(day.path("tempMin").asDouble());
                codes.add(mapQWeatherCode(day.path("textDay").asText(), day.path("codeDay").asText()));
            }
        }

        Map<String, Object> daily = new LinkedHashMap<>();
        daily.put("time", times);
        daily.put("temperature_2m_max", maxTemps);
        daily.put("temperature_2m_min", minTemps);
        daily.put("weather_code", codes);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "qweather");
        result.put("providerName", "和风天气");
        result.put("providerUrl", "https://www.qweather.com");
        result.put("current", current);
        result.put("daily", daily);
        return result;
    }

    private int mapQWeatherCode(String text, String code) {
        // 和风天气 code 映射到 WMO code
        int c;
        try { c = Integer.parseInt(code); } catch (Exception e) { return 0; }
        if (c == 100) return 0;        // 晴
        if (c == 101 || c == 102) return 1; // 多云
        if (c == 103 || c == 104) return 2; // 阴
        if (c >= 300 && c <= 304) return 61; // 雨
        if (c >= 305 && c <= 312) return 63; // 中雨
        if (c >= 313 && c <= 399) return 65; // 大雨/冻雨
        if (c >= 400 && c <= 499) return 71; // 雪
        if (c == 500 || c == 501) return 45; // 雾
        if (c == 502 || c == 503) return 48; // 霾
        if (c == 504 || c == 507 || c == 508) return 0; // 浮尘/沙尘
        if (c >= 300) return 61;
        return 0;
    }

    // ==================== OpenWeatherMap ====================

    private Map<String, Object> fetchOpenWeatherMap(WeatherConfigEntity config, String apiKey, double lat, double lon) {
        String baseUrl = config.getBaseUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        String lang = config.getLanguage() != null ? config.getLanguage() : "en";
        String units = "metric";

        // 当前天气
        String currentUrl = baseUrl + "weather?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey + "&units=" + units + "&lang=" + lang;
        String currentRaw = safeGet(currentUrl);
        JsonNode currentJson = parseJson(currentRaw);

        JsonNode main = currentJson.path("main");
        JsonNode weather = currentJson.path("weather").path(0);

        Map<String, Object> current = new LinkedHashMap<>();
        current.put("temperature_2m", main.path("temp").asDouble());
        current.put("weather_code", mapOWMCode(weather.path("id").asInt()));

        // 5天预报（每3小时）
        String forecastUrl = baseUrl + "forecast?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey + "&units=" + units + "&lang=" + lang;
        String forecastRaw = safeGet(forecastUrl);
        JsonNode forecastJson = parseJson(forecastRaw);

        // 按天聚合
        Map<String, List<Double>> dayMax = new LinkedHashMap<>();
        Map<String, List<Double>> dayMin = new LinkedHashMap<>();
        Map<String, Integer> dayCodes = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        for (int i = 1; i <= 4; i++) {
            String date = today.plusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            dayMax.put(date, new ArrayList<>());
            dayMin.put(date, new ArrayList<>());
        }

        for (JsonNode item : forecastJson.path("list")) {
            String dtTxt = item.path("dt_txt").asText();
            if (dtTxt.length() < 10) continue;
            String date = dtTxt.substring(0, 10);
            if (!dayMax.containsKey(date)) continue;

            double temp = item.path("main").path("temp").asDouble();
            dayMax.get(date).add(temp);
            dayMin.get(date).add(temp);
            int weatherId = item.path("weather").path(0).path("id").asInt();
            dayCodes.put(date, mapOWMCode(weatherId));
        }

        List<String> times = new ArrayList<>();
        List<Double> maxTemps = new ArrayList<>();
        List<Double> minTemps = new ArrayList<>();
        List<Integer> codes = new ArrayList<>();

        for (String date : dayMax.keySet()) {
            times.add(date);
            maxTemps.add(dayMax.get(date).stream().mapToDouble(d -> d).max().orElse(0));
            minTemps.add(dayMin.get(date).stream().mapToDouble(d -> d).min().orElse(0));
            codes.add(dayCodes.getOrDefault(date, 0));
        }

        Map<String, Object> daily = new LinkedHashMap<>();
        daily.put("time", times);
        daily.put("temperature_2m_max", maxTemps);
        daily.put("temperature_2m_min", minTemps);
        daily.put("weather_code", codes);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "openweathermap");
        result.put("providerName", "OpenWeatherMap");
        result.put("providerUrl", "https://openweathermap.org");
        result.put("current", current);
        result.put("daily", daily);
        return result;
    }

    private int mapOWMCode(int owmId) {
        if (owmId >= 200 && owmId < 300) return 95;   // 雷暴
        if (owmId >= 300 && owmId < 400) return 51;   // 毛毛雨
        if (owmId >= 500 && owmId < 505) return 61;   // 雨
        if (owmId >= 505 && owmId < 600) return 65;   // 冻雨/暴雨
        if (owmId >= 600 && owmId < 613) return 71;   // 雪
        if (owmId >= 613 && owmId < 700) return 67;   // 雨夹雪
        if (owmId >= 700 && owmId < 800) return 45;   // 雾/霾
        if (owmId == 800) return 0;                    // 晴
        if (owmId == 801) return 1;                    // 少云
        if (owmId == 802) return 2;                    // 多云
        if (owmId >= 803) return 3;                    // 阴
        return 0;
    }

    // ==================== 心知天气 ====================

    private Map<String, Object> fetchSeniverse(WeatherConfigEntity config, String apiKey, double lat, double lon) {
        String baseUrl = config.getBaseUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        String lang = config.getLanguage() != null ? config.getLanguage() : "zh";
        String location = lat + ":" + lon;

        // 当前天气
        String currentUrl = baseUrl + "weather/now.json?key=" + apiKey + "&location=" + location + "&language=" + lang;
        String currentRaw = safeGet(currentUrl);
        JsonNode currentJson = parseJson(currentRaw);
        JsonNode now = currentJson.path("results").path(0).path("now");

        Map<String, Object> current = new LinkedHashMap<>();
        current.put("temperature_2m", Double.parseDouble(now.path("temperature").asText("0")));
        current.put("weather_code", mapSeniverseCode(now.path("code").asText()));

        // 3天预报
        String dailyUrl = baseUrl + "weather/daily.json?key=" + apiKey + "&location=" + location + "&language=" + lang + "&start=0&days=5";
        String dailyRaw = safeGet(dailyUrl);
        JsonNode dailyJson = parseJson(dailyRaw);

        List<String> times = new ArrayList<>();
        List<Double> maxTemps = new ArrayList<>();
        List<Double> minTemps = new ArrayList<>();
        List<Integer> codes = new ArrayList<>();

        for (JsonNode day : dailyJson.path("results").path(0).path("daily")) {
            times.add(day.path("date").asText());
            maxTemps.add(Double.parseDouble(day.path("high").asText("0")));
            minTemps.add(Double.parseDouble(day.path("low").asText("0")));
            codes.add(mapSeniverseCode(day.path("code_day").asText()));
        }

        Map<String, Object> daily = new LinkedHashMap<>();
        daily.put("time", times);
        daily.put("temperature_2m_max", maxTemps);
        daily.put("temperature_2m_min", minTemps);
        daily.put("weather_code", codes);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "seniverse");
        result.put("providerName", "心知天气");
        result.put("providerUrl", "https://seniverse.com");
        result.put("current", current);
        result.put("daily", daily);
        return result;
    }

    private int mapSeniverseCode(String code) {
        int c;
        try { c = Integer.parseInt(code); } catch (Exception e) { return 0; }
        if (c == 0) return 0;          // 晴
        if (c == 1 || c == 2) return 1; // 多云
        if (c == 3) return 3;           // 阴
        if (c >= 4 && c <= 9) return 61; // 雨
        if (c >= 10 && c <= 12) return 71; // 雪
        if (c == 13 || c == 14) return 51; // 雾
        if (c >= 15 && c <= 18) return 45; // 霾/沙尘
        if (c >= 19 && c <= 25) return 61; // 雨
        if (c >= 26 && c <= 28) return 71; // 雪
        if (c >= 29 && c <= 30) return 95; // 沙尘暴
        if (c >= 31 && c <= 35) return 45; // 雾霾
        return 0;
    }

    // ==================== 自定义供应商 ====================

    private Map<String, Object> fetchCustom(WeatherConfigEntity config, String apiKey, double lat, double lon) {
        String baseUrl = config.getBaseUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        String lang = config.getLanguage() != null ? config.getLanguage() : "zh";

        String url = baseUrl;
        if ("query_param".equals(config.getAuthType()) || config.getAuthType() == null) {
            url += (url.contains("?") ? "&" : "?") + "key=" + apiKey;
        }

        String raw = safeGet(url);

        // 自定义供应商返回原始数据，前端需要自行解析
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "custom");
        result.put("providerName", config.getName());
        result.put("providerUrl", config.getBaseUrl());
        result.put("raw", raw);
        return result;
    }
}
