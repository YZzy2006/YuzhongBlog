package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.dto.site.SiteInfoResponse;
import com.ticketingsystem.yuzhonblog.dto.site.SiteSettingRequest;
import com.ticketingsystem.yuzhonblog.entity.SiteSetting;
import com.ticketingsystem.yuzhonblog.repository.SiteSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteSettingServiceTest {

    @Mock
    private SiteSettingRepository siteSettingRepository;
    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    @InjectMocks
    private SiteSettingService siteSettingService;

    // --- getSiteInfo ---

    @Test
    void getSiteInfo_WithSettings_ReturnsMappedResponse() {
        // given
        SiteSetting s1 = buildSiteSetting(1L, "site_name", "My Blog");
        SiteSetting s2 = buildSiteSetting(2L, "site_description", "A tech blog");
        SiteSetting s3 = buildSiteSetting(3L, "icp_number", "京ICP备12345678号");
        when(siteSettingRepository.findAll()).thenReturn(List.of(s1, s2, s3));

        // when
        SiteInfoResponse response = siteSettingService.getSiteInfo();

        // then
        assertThat(response.getSiteName()).isEqualTo("My Blog");
        assertThat(response.getSiteDescription()).isEqualTo("A tech blog");
        assertThat(response.getExtraSettings()).containsEntry("icp_number", "京ICP备12345678号");
        assertThat(response.getExtraSettings()).doesNotContainKey("site_name");
        assertThat(response.getExtraSettings()).doesNotContainKey("site_description");
    }

    @Test
    void getSiteInfo_NoSettings_ReturnsDefaults() {
        // given
        when(siteSettingRepository.findAll()).thenReturn(Collections.emptyList());

        // when
        SiteInfoResponse response = siteSettingService.getSiteInfo();

        // then
        assertThat(response.getSiteName()).isEqualTo("雨中的研发日志");
        assertThat(response.getSiteDescription()).isEmpty();
        assertThat(response.getExtraSettings()).isEmpty();
    }

    @Test
    void getSiteInfo_MissingSiteName_UsesDefault() {
        // given
        SiteSetting s1 = buildSiteSetting(1L, "site_description", "A blog");
        when(siteSettingRepository.findAll()).thenReturn(List.of(s1));

        // when
        SiteInfoResponse response = siteSettingService.getSiteInfo();

        // then
        assertThat(response.getSiteName()).isEqualTo("雨中的研发日志");
        assertThat(response.getSiteDescription()).isEqualTo("A blog");
    }

    // --- getAllSettings ---

    @Test
    void getAllSettings_WithSettings_ReturnsMap() {
        // given
        SiteSetting s1 = buildSiteSetting(1L, "site_name", "My Blog");
        SiteSetting s2 = buildSiteSetting(2L, "site_description", "A blog");
        when(siteSettingRepository.findAll()).thenReturn(List.of(s1, s2));

        // when
        Map<String, String> result = siteSettingService.getAllSettings();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("site_name", "My Blog");
        assertThat(result).containsEntry("site_description", "A blog");
    }

    @Test
    void getAllSettings_NoSettings_ReturnsEmptyMap() {
        // given
        when(siteSettingRepository.findAll()).thenReturn(Collections.emptyList());

        // when
        Map<String, String> result = siteSettingService.getAllSettings();

        // then
        assertThat(result).isEmpty();
    }

    // --- updateSettings ---

    @Test
    void updateSettings_NewKeys_CreatesAndSaves() {
        // given
        SiteSettingRequest request = new SiteSettingRequest();
        Map<String, String> settings = new HashMap<>();
        settings.put("site_name", "New Blog");
        settings.put("site_description", "New desc");
        request.setSettings(settings);

        when(siteSettingRepository.findBySettingKey("site_name")).thenReturn(Optional.empty());
        when(siteSettingRepository.findBySettingKey("site_description")).thenReturn(Optional.empty());
        when(siteSettingRepository.save(any(SiteSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        siteSettingService.updateSettings(request);

        // then
        verify(siteSettingRepository, times(2)).save(any(SiteSetting.class));
        verify(siteSettingRepository).save(argThat(s -> s.getSettingKey().equals("site_name") && s.getSettingValue().equals("New Blog")));
        verify(siteSettingRepository).save(argThat(s -> s.getSettingKey().equals("site_description") && s.getSettingValue().equals("New desc")));
    }

    @Test
    void updateSettings_ExistingKeys_UpdatesValue() {
        // given
        SiteSetting existing = buildSiteSetting(1L, "site_name", "Old Blog");
        SiteSettingRequest request = new SiteSettingRequest();
        Map<String, String> settings = new HashMap<>();
        settings.put("site_name", "Updated Blog");
        request.setSettings(settings);

        when(siteSettingRepository.findBySettingKey("site_name")).thenReturn(Optional.of(existing));
        when(siteSettingRepository.save(any(SiteSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        siteSettingService.updateSettings(request);

        // then
        verify(siteSettingRepository).save(argThat(s -> s.getSettingKey().equals("site_name") && s.getSettingValue().equals("Updated Blog")));
    }

    @Test
    void updateSettings_NullSettings_DoesNothing() {
        // given
        SiteSettingRequest request = new SiteSettingRequest();
        request.setSettings(null);

        // when
        siteSettingService.updateSettings(request);

        // then
        verify(siteSettingRepository, never()).findBySettingKey(anyString());
        verify(siteSettingRepository, never()).save(any());
    }

    // --- helpers ---

    private SiteSetting buildSiteSetting(Long id, String key, String value) {
        SiteSetting setting = new SiteSetting();
        setting.setId(id);
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        return setting;
    }
}
