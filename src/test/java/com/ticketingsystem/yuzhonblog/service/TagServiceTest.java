package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.dto.tag.TagRequest;
import com.ticketingsystem.yuzhonblog.dto.tag.TagResponse;
import com.ticketingsystem.yuzhonblog.entity.Tag;
import com.ticketingsystem.yuzhonblog.repository.ArticleTagRepository;
import com.ticketingsystem.yuzhonblog.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ArticleTagRepository articleTagRepository;

    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    @InjectMocks
    private TagService tagService;

    @BeforeEach
    void setUp() {
        lenient().when(articleTagRepository.countPublishedByTagId(anyLong())).thenReturn(0L);
    }

    // --- list ---

    @Test
    void list_HasTags_ReturnsSortedList() {
        // given
        Tag tag1 = buildTag(1L, "Java");
        Tag tag2 = buildTag(2L, "Spring");
        when(tagRepository.findAllByOrderByNameAsc()).thenReturn(List.of(tag1, tag2));

        // when
        List<TagResponse> result = tagService.list();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Java");
        assertThat(result.get(1).getName()).isEqualTo("Spring");
        verify(tagRepository).findAllByOrderByNameAsc();
    }

    @Test
    void list_NoTags_ReturnsEmptyList() {
        // given
        when(tagRepository.findAllByOrderByNameAsc()).thenReturn(Collections.emptyList());

        // when
        List<TagResponse> result = tagService.list();

        // then
        assertThat(result).isEmpty();
    }

    // --- getById ---

    @Test
    void getById_ExistingId_ReturnsResponse() {
        // given
        Tag tag = buildTag(1L, "Java");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        // when
        TagResponse response = tagService.getById(1L);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Java");
    }

    @Test
    void getById_NonExistentId_ThrowsBusinessException() {
        // given
        when(tagRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> tagService.getById(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(3003));
    }

    // --- create ---

    @Test
    void create_ValidRequest_CreatesAndReturnsResponse() {
        // given
        TagRequest request = new TagRequest();
        request.setName("New Tag");

        when(tagRepository.existsByName("New Tag")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });

        // when
        TagResponse response = tagService.create(request);

        // then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getName()).isEqualTo("New Tag");
        verify(tagRepository).save(argThat(t -> t.getName().equals("New Tag")));
    }

    @Test
    void create_DuplicateName_ThrowsBusinessException() {
        // given
        TagRequest request = new TagRequest();
        request.setName("Existing");
        when(tagRepository.existsByName("Existing")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> tagService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签名称已存在");
    }

    // --- update ---

    @Test
    void update_ExistingId_UpdatesAndReturnsResponse() {
        // given
        Tag existing = buildTag(1L, "Old Name");
        TagRequest request = new TagRequest();
        request.setName("New Name");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        TagResponse response = tagService.update(1L, request);

        // then
        assertThat(response.getName()).isEqualTo("New Name");
        verify(tagRepository).save(argThat(t -> t.getName().equals("New Name")));
    }

    @Test
    void update_NonExistentId_ThrowsBusinessException() {
        // given
        TagRequest request = new TagRequest();
        request.setName("Name");
        when(tagRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> tagService.update(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(3003));
    }

    @Test
    void update_NameChangedToDuplicate_ThrowsBusinessException() {
        // given
        Tag existing = buildTag(1L, "Old Name");
        TagRequest request = new TagRequest();
        request.setName("Duplicate Name");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tagRepository.existsByName("Duplicate Name")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> tagService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签名称已存在");
    }

    @Test
    void update_SameName_NoDuplicateCheck() {
        // given
        Tag existing = buildTag(1L, "Same Name");
        TagRequest request = new TagRequest();
        request.setName("Same Name");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        TagResponse response = tagService.update(1L, request);

        // then
        assertThat(response.getName()).isEqualTo("Same Name");
        verify(tagRepository, never()).existsByName(anyString());
    }

    // --- delete ---

    @Test
    void delete_ExistingId_DeletesSuccessfully() {
        // given
        Tag tag = buildTag(1L, "Java");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        // when
        tagService.delete(1L);

        // then
        verify(tagRepository).delete(tag);
    }

    @Test
    void delete_NonExistentId_ThrowsBusinessException() {
        // given
        when(tagRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> tagService.delete(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(3003));
    }

    // --- helpers ---

    private Tag buildTag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }
}
