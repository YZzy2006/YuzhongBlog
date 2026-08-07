package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.dto.category.CategoryRequest;
import com.ticketingsystem.yuzhonblog.dto.category.CategoryResponse;
import com.ticketingsystem.yuzhonblog.entity.Category;
import com.ticketingsystem.yuzhonblog.repository.ArticleRepository;
import com.ticketingsystem.yuzhonblog.repository.CategoryRepository;
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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    @InjectMocks
    private CategoryService categoryService;

    // --- list ---

    @Test
    void list_HasCategories_ReturnsSortedList() {
        // given
        Category cat1 = buildCategory(1L, "Tech", 10);
        Category cat2 = buildCategory(2L, "Life", 5);
        when(categoryRepository.findAllByOrderBySortOrderDesc()).thenReturn(List.of(cat1, cat2));

        // when
        List<CategoryResponse> result = categoryService.list();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Tech");
        assertThat(result.get(0).getSortOrder()).isEqualTo(10);
        assertThat(result.get(1).getName()).isEqualTo("Life");
        verify(categoryRepository).findAllByOrderBySortOrderDesc();
    }

    @Test
    void list_NoCategories_ReturnsEmptyList() {
        // given
        when(categoryRepository.findAllByOrderBySortOrderDesc()).thenReturn(Collections.emptyList());

        // when
        List<CategoryResponse> result = categoryService.list();

        // then
        assertThat(result).isEmpty();
    }

    // --- getById ---

    @Test
    void getById_ExistingId_ReturnsResponse() {
        // given
        Category category = buildCategory(1L, "Tech", 5);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // when
        CategoryResponse response = categoryService.getById(1L);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Tech");
        assertThat(response.getSortOrder()).isEqualTo(5);
    }

    @Test
    void getById_NonExistentId_ThrowsBusinessException() {
        // given
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> categoryService.getById(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(3001));
    }

    // --- create ---

    @Test
    void create_ValidRequest_CreatesAndReturnsResponse() {
        // given
        CategoryRequest request = new CategoryRequest();
        request.setName("New Category");
        request.setSortOrder(1);

        when(categoryRepository.existsByName("New Category")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        // when
        CategoryResponse response = categoryService.create(request);

        // then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getName()).isEqualTo("New Category");
        assertThat(response.getSortOrder()).isEqualTo(1);
        verify(categoryRepository).save(argThat(c -> c.getName().equals("New Category")));
    }

    @Test
    void create_DuplicateName_ThrowsBusinessException() {
        // given
        CategoryRequest request = new CategoryRequest();
        request.setName("Existing");
        when(categoryRepository.existsByName("Existing")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类名称已存在");
    }

    // --- update ---

    @Test
    void update_ExistingId_UpdatesAndReturnsResponse() {
        // given
        Category existing = buildCategory(1L, "Old Name", 0);
        CategoryRequest request = new CategoryRequest();
        request.setName("New Name");
        request.setSortOrder(5);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        CategoryResponse response = categoryService.update(1L, request);

        // then
        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getSortOrder()).isEqualTo(5);
        verify(categoryRepository).save(argThat(c -> c.getName().equals("New Name")));
    }

    @Test
    void update_NonExistentId_ThrowsBusinessException() {
        // given
        CategoryRequest request = new CategoryRequest();
        request.setName("Name");
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> categoryService.update(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(3001));
    }

    @Test
    void update_NameChangedToDuplicate_ThrowsBusinessException() {
        // given
        Category existing = buildCategory(1L, "Old Name", 0);
        CategoryRequest request = new CategoryRequest();
        request.setName("Duplicate Name");
        request.setSortOrder(0);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Duplicate Name")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类名称已存在");
    }

    @Test
    void update_SameName_NoDuplicateCheck() {
        // given
        Category existing = buildCategory(1L, "Same Name", 0);
        CategoryRequest request = new CategoryRequest();
        request.setName("Same Name");
        request.setSortOrder(3);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        CategoryResponse response = categoryService.update(1L, request);

        // then
        assertThat(response.getName()).isEqualTo("Same Name");
        assertThat(response.getSortOrder()).isEqualTo(3);
        verify(categoryRepository, never()).existsByName(anyString());
    }

    // --- delete ---

    @Test
    void delete_ExistingId_DeletesSuccessfully() {
        // given
        Category category = buildCategory(1L, "Tech", 0);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // when
        categoryService.delete(1L);

        // then
        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_NonExistentId_ThrowsBusinessException() {
        // given
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> categoryService.delete(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(3001));
    }

    // --- helpers ---

    private Category buildCategory(Long id, String name, Integer sortOrder) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSortOrder(sortOrder);
        return category;
    }
}
