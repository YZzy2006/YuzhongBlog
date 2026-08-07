package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.ai.AiConfigRequest;
import com.ticketingsystem.yuzhonblog.entity.AiConfigEntity;
import com.ticketingsystem.yuzhonblog.repository.AiConfigRepository;
import com.ticketingsystem.yuzhonblog.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiConfigService {

    private final AiConfigRepository aiConfigRepository;
    private final AesUtil aesUtil;

    @Transactional(readOnly = true)
    public List<AiConfigEntity> list() {
        return aiConfigRepository.findAllByOrderBySortOrderDesc();
    }

    @Transactional(readOnly = true)
    public AiConfigEntity get(Long id) {
        return aiConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND.getCode(), "AI配置不存在"));
    }

    @Transactional(readOnly = true)
    public AiConfigEntity getActive() {
        return aiConfigRepository.findFirstActive().orElse(null);
    }

    @Transactional
    public AiConfigEntity create(AiConfigRequest dto) {
        AiConfigEntity entity = new AiConfigEntity();
        entity.setName(dto.getName());
        entity.setApiKey(aesUtil.encrypt(dto.getApiKey()));
        entity.setBaseUrl(dto.getBaseUrl());
        entity.setModel(dto.getModel());
        entity.setMaxTokens(dto.getMaxTokens() != null ? dto.getMaxTokens() : 4096);
        entity.setTemperature(dto.getTemperature() != null ? dto.getTemperature() : 0.7);
        entity.setApiFormat(dto.getApiFormat() != null ? dto.getApiFormat() : "OPENAI");
        entity.setAuthType(dto.getAuthType() != null ? dto.getAuthType() : "BEARER");
        entity.setWebsiteUrl(dto.getWebsiteUrl());
        entity.setBalanceUrl(dto.getBalanceUrl());
        entity.setBalanceScript(dto.getBalanceScript());
        entity.setDescription(dto.getDescription());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);

        // Auto-activate if this is the first config
        boolean isFirst = aiConfigRepository.count() == 0;
        entity.setIsActive(isFirst);

        entity = aiConfigRepository.save(entity);
        log.info("AI配置已创建: id={}, name={}, autoActivated={}", entity.getId(), entity.getName(), isFirst);
        return entity;
    }

    @Transactional
    public AiConfigEntity update(Long id, AiConfigRequest dto) {
        AiConfigEntity entity = get(id);
        entity.setName(dto.getName());
        // Only update key if a new one is provided (not masked)
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank() && !dto.getApiKey().startsWith("****")) {
            entity.setApiKey(aesUtil.encrypt(dto.getApiKey()));
        }
        entity.setBaseUrl(dto.getBaseUrl());
        entity.setModel(dto.getModel());
        if (dto.getMaxTokens() != null) entity.setMaxTokens(dto.getMaxTokens());
        if (dto.getTemperature() != null) entity.setTemperature(dto.getTemperature());
        if (dto.getApiFormat() != null) entity.setApiFormat(dto.getApiFormat());
        if (dto.getAuthType() != null) entity.setAuthType(dto.getAuthType());
        entity.setWebsiteUrl(dto.getWebsiteUrl());
        entity.setBalanceUrl(dto.getBalanceUrl());
        entity.setBalanceScript(dto.getBalanceScript());
        entity.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());

        entity = aiConfigRepository.save(entity);
        log.info("AI配置已更新: id={}, name={}", entity.getId(), entity.getName());
        return entity;
    }

    @Transactional
    public void delete(Long id) {
        AiConfigEntity entity = get(id);
        boolean wasActive = Boolean.TRUE.equals(entity.getIsActive());
        aiConfigRepository.delete(entity);
        log.info("AI配置已删除: id={}, name={}, wasActive={}", id, entity.getName(), wasActive);
    }

    @Transactional
    public void activate(Long id) {
        // Bulk deactivate all active configs in a single UPDATE
        aiConfigRepository.deactivateAll();
        // Activate target
        AiConfigEntity target = get(id);
        target.setIsActive(true);
        log.info("AI配置已激活: id={}, name={}", target.getId(), target.getName());
    }
}
