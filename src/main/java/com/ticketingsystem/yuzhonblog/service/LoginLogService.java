package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.log.HeatmapEntry;
import com.ticketingsystem.yuzhonblog.dto.log.LoginLogResponse;
import com.ticketingsystem.yuzhonblog.dto.log.SecurityAlert;
import com.ticketingsystem.yuzhonblog.entity.LoginLog;
import com.ticketingsystem.yuzhonblog.repository.LoginLogRepository;
import com.ticketingsystem.yuzhonblog.security.SessionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;
    private final SessionStore sessionStore;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PageResult<LoginLogResponse> search(String keyword, Integer status,
                                                String startDate, String endDate,
                                                int page, int size) {
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<LoginLog> result = loginLogRepository.search(kw, status, start, end, pageable);
        return PageResult.of(result.map(LoginLogResponse::from));
    }

    public List<LoginLogResponse> exportCsv(String keyword, Integer status,
                                             String startDate, String endDate,
                                             Integer page, Integer size) {
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Pageable pageable = (page != null && size != null)
                ? PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 10000))
                : PageRequest.of(0, 10000);
        List<LoginLog> logs = loginLogRepository.searchAll(kw, status, start, end, pageable);
        return logs.stream().map(LoginLogResponse::from).collect(Collectors.toList());
    }

    public List<HeatmapEntry> getLoginHeatmap(Long userId) {
        // Go back ~1 year and align to Sunday (GitHub style)
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(364);
        // Align to the previous Sunday (or today if it's Sunday)
        while (startDate.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
            startDate = startDate.minusDays(1);
        }
        LocalDateTime since = startDate.atStartOfDay();

        List<Object[]> rows = loginLogRepository.countSuccessfulLoginsByDay(userId, since);
        Map<String, Long> countMap = rows.stream()
                .collect(Collectors.toMap(
                        r -> r[0].toString(),
                        r -> (Long) r[1]
                ));

        List<HeatmapEntry> entries = new ArrayList<>();
        LocalDate date = startDate;
        while (!date.isAfter(today)) {
            String key = date.toString();
            entries.add(new HeatmapEntry(key, countMap.getOrDefault(key, 0L)));
            date = date.plusDays(1);
        }
        return entries;
    }

    public List<SecurityAlert> getSecurityAlerts() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Object[]> rows = loginLogRepository.findUsersWithExcessiveFailures(since, 5);
        return rows.stream()
                .map(r -> new SecurityAlert(
                        (String) r[0],
                        (Long) r[1],
                        r[2] != null ? r[2].toString() : null
                ))
                .collect(Collectors.toList());
    }

    public SessionStore.SessionData getCurrentSession(Long userId) {
        return sessionStore.getTokenInfo(userId);
    }

    public Map<String, Long> getHeatmapAsMap(Long userId) {
        return getLoginHeatmap(userId).stream()
                .collect(Collectors.toMap(HeatmapEntry::getDate, HeatmapEntry::getCount));
    }

    private LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr, DATE_FMT).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr, DATE_FMT).atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
