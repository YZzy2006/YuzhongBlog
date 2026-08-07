package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.dto.project.ProjectRequest;
import com.ticketingsystem.yuzhonblog.dto.project.ProjectResponse;
import com.ticketingsystem.yuzhonblog.entity.Project;
import com.ticketingsystem.yuzhonblog.repository.ProjectRepository;
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
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private SensitiveWordFilter sensitiveWordFilter;
    @Mock
    private ContentReviewHelper contentReviewHelper;

    @InjectMocks
    private ProjectService projectService;

    // --- list ---

    @Test
    void list_HasProjects_ReturnsSortedList() {
        // given
        Project p1 = buildProject(1L, "Blog", "A blog", "Spring Boot", "cover1.png", "https://github.com/blog", "https://demo.blog", 10);
        Project p2 = buildProject(2L, "API", "An API", "Java", "cover2.png", "https://github.com/api", "https://demo.api", 5);
        when(projectRepository.findAllByOrderBySortOrderDescCreatedAtDesc()).thenReturn(List.of(p1, p2));

        // when
        List<ProjectResponse> result = projectService.list();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Blog");
        assertThat(result.get(0).getTechStack()).isEqualTo("Spring Boot");
        assertThat(result.get(1).getName()).isEqualTo("API");
        verify(projectRepository).findAllByOrderBySortOrderDescCreatedAtDesc();
    }

    @Test
    void list_NoProjects_ReturnsEmptyList() {
        // given
        when(projectRepository.findAllByOrderBySortOrderDescCreatedAtDesc()).thenReturn(Collections.emptyList());

        // when
        List<ProjectResponse> result = projectService.list();

        // then
        assertThat(result).isEmpty();
    }

    // --- getById ---

    @Test
    void getById_ExistingId_ReturnsResponse() {
        // given
        Project project = buildProject(1L, "Blog", "A blog", "Spring Boot", "cover.png", "https://github.com", "https://demo.com", 1);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // when
        ProjectResponse response = projectService.getById(1L);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Blog");
        assertThat(response.getDescription()).isEqualTo("A blog");
        assertThat(response.getTechStack()).isEqualTo("Spring Boot");
        assertThat(response.getCoverImage()).isEqualTo("cover.png");
        assertThat(response.getGithubUrl()).isEqualTo("https://github.com");
        assertThat(response.getDemoUrl()).isEqualTo("https://demo.com");
        assertThat(response.getSortOrder()).isEqualTo(1);
    }

    @Test
    void getById_NonExistentId_ThrowsBusinessException() {
        // given
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> projectService.getById(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(4001));
    }

    // --- create ---

    @Test
    void create_ValidRequest_CreatesAndReturnsResponse() {
        // given
        ProjectRequest request = buildProjectRequest("New Project", "Desc", "Vue", "cover.png", "https://github.com", "https://demo.com", 1);

        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        // when
        ProjectResponse response = projectService.create(request);

        // then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getName()).isEqualTo("New Project");
        assertThat(response.getDescription()).isEqualTo("Desc");
        assertThat(response.getTechStack()).isEqualTo("Vue");
        assertThat(response.getCoverImage()).isEqualTo("cover.png");
        assertThat(response.getGithubUrl()).isEqualTo("https://github.com");
        assertThat(response.getDemoUrl()).isEqualTo("https://demo.com");
        assertThat(response.getSortOrder()).isEqualTo(1);
        verify(projectRepository).save(argThat(p -> p.getName().equals("New Project")));
    }

    // --- update ---

    @Test
    void update_ExistingId_UpdatesAndReturnsResponse() {
        // given
        Project existing = buildProject(1L, "Old", "Old Desc", "Java", "old.png", "https://old.com", "https://old-demo.com", 0);
        ProjectRequest request = buildProjectRequest("Updated", "New Desc", "Kotlin", "new.png", "https://new.com", "https://new-demo.com", 5);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        ProjectResponse response = projectService.update(1L, request);

        // then
        assertThat(response.getName()).isEqualTo("Updated");
        assertThat(response.getDescription()).isEqualTo("New Desc");
        assertThat(response.getTechStack()).isEqualTo("Kotlin");
        assertThat(response.getCoverImage()).isEqualTo("new.png");
        assertThat(response.getGithubUrl()).isEqualTo("https://new.com");
        assertThat(response.getDemoUrl()).isEqualTo("https://new-demo.com");
        assertThat(response.getSortOrder()).isEqualTo(5);
        verify(projectRepository).save(argThat(p -> p.getName().equals("Updated")));
    }

    @Test
    void update_NonExistentId_ThrowsBusinessException() {
        // given
        ProjectRequest request = buildProjectRequest("Name", "Desc", "Java", null, null, null, 0);
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> projectService.update(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(4001));
    }

    // --- delete ---

    @Test
    void delete_ExistingId_DeletesSuccessfully() {
        // given
        when(projectRepository.existsById(1L)).thenReturn(true);

        // when
        projectService.delete(1L);

        // then
        verify(projectRepository).deleteById(1L);
    }

    @Test
    void delete_NonExistentId_ThrowsBusinessException() {
        // given
        when(projectRepository.existsById(999L)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> projectService.delete(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(4001));
    }

    // --- helpers ---

    private Project buildProject(Long id, String name, String description, String techStack,
                                  String coverImage, String githubUrl, String demoUrl, Integer sortOrder) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription(description);
        project.setTechStack(techStack);
        project.setCoverImage(coverImage);
        project.setGithubUrl(githubUrl);
        project.setDemoUrl(demoUrl);
        project.setSortOrder(sortOrder);
        project.setStatus("PUBLISHED");
        return project;
    }

    private ProjectRequest buildProjectRequest(String name, String description, String techStack,
                                                String coverImage, String githubUrl, String demoUrl, Integer sortOrder) {
        ProjectRequest request = new ProjectRequest();
        request.setName(name);
        request.setDescription(description);
        request.setTechStack(techStack);
        request.setCoverImage(coverImage);
        request.setGithubUrl(githubUrl);
        request.setDemoUrl(demoUrl);
        request.setSortOrder(sortOrder);
        return request;
    }
}
