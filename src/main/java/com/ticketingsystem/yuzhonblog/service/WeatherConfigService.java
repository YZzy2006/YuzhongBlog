package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.weather.WeatherConfigRequest;
import com.ticketingsystem.yuzhonblog.entity.WeatherConfigEntity;
import com.ticketingsystem.yuzhonblog.repository.WeatherConfigRepository;
import com.ticketingsystem.yuzhonblog.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherConfigService {

    private final WeatherConfigRepository weatherConfigRepository;
    private final AesUtil aesUtil;

    @Transactional(readOnly = true)
    public List<WeatherConfigEntity> list() {
        return weatherConfigRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public WeatherConfigEntity get(Long id) {
        return weatherConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND.getCode(), "天气配置不存在"));
    }

    @Transactional(readOnly = true)
    public WeatherConfigEntity getActive() {
        return weatherConfigRepository.findFirstByIsActiveTrue().orElse(null);
    }

    @Transactional
    public WeatherConfigEntity create(WeatherConfigRequest dto) {
        WeatherConfigEntity entity = new WeatherConfigEntity();
        entity.setName(dto.getName());
        entity.setProvider(dto.getProvider());
        entity.setApiKey(aesUtil.encrypt(dto.getApiKey()));
        entity.setBaseUrl(dto.getBaseUrl());
        entity.setApiFormat(dto.getApiFormat() != null ? dto.getApiFormat() : "json");
        entity.setAuthType(dto.getAuthType() != null ? dto.getAuthType() : "query_param");
        entity.setLanguage(dto.getLanguage() != null ? dto.getLanguage() : "zh");
        entity.setUnits(dto.getUnits() != null ? dto.getUnits() : "c");
        entity.setLocation(dto.getLocation());
        entity.setExtraParams(dto.getExtraParams());
        entity.setDescription(dto.getDescription());

        // Auto-activate if this is the first config
        boolean isFirst = weatherConfigRepository.count() == 0;
        entity.setIsActive(isFirst);

        entity = weatherConfigRepository.save(entity);
        log.info("天气配置已创建: id={}, name={}, provider={}, autoActivated={}", entity.getId(), entity.getName(), entity.getProvider(), isFirst);
        return entity;
    }

    @Transactional
    public WeatherConfigEntity update(Long id, WeatherConfigRequest dto) {
        WeatherConfigEntity entity = get(id);
        entity.setName(dto.getName());
        entity.setProvider(dto.getProvider());
        // Only update key if a new one is provided (not masked)
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank() && !dto.getApiKey().startsWith("****")) {
            entity.setApiKey(aesUtil.encrypt(dto.getApiKey()));
        }
        entity.setBaseUrl(dto.getBaseUrl());
        if (dto.getApiFormat() != null) entity.setApiFormat(dto.getApiFormat());
        if (dto.getAuthType() != null) entity.setAuthType(dto.getAuthType());
        if (dto.getLanguage() != null) entity.setLanguage(dto.getLanguage());
        if (dto.getUnits() != null) entity.setUnits(dto.getUnits());
        entity.setLocation(dto.getLocation());
        entity.setExtraParams(dto.getExtraParams());
        entity.setDescription(dto.getDescription());

        entity = weatherConfigRepository.save(entity);
        log.info("天气配置已更新: id={}, name={}", entity.getId(), entity.getName());
        return entity;
    }

    @Transactional
    public void delete(Long id) {
        WeatherConfigEntity entity = get(id);
        boolean wasActive = Boolean.TRUE.equals(entity.getIsActive());
        weatherConfigRepository.delete(entity);
        log.info("天气配置已删除: id={}, name={}, wasActive={}", id, entity.getName(), wasActive);
    }

    @Transactional
    public void activate(Long id) {
        weatherConfigRepository.deactivateAll();
        WeatherConfigEntity target = get(id);
        target.setIsActive(true);
        log.info("天气配置已激活: id={}, name={}", target.getId(), target.getName());
    }
}
