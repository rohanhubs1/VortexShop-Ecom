package com.ecom.platform.service;

import com.ecom.platform.dto.CategoryDto;
import com.ecom.platform.entity.Category;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.CategoryRepository;
import com.ecom.platform.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.ecom.platform.support.TestFixtures.category;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryDto dto(String name) {
        return CategoryDto.builder().name(name).description("desc").build();
    }

    @Test
    void listsAndFetchesCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(category(1L, "Books"), category(2L, "Toys")));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Books")));
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(categoryService.getAllCategories()).extracting(CategoryDto::getName).containsExactly("Books", "Toys");
        assertThat(categoryService.getCategoryById(1L).getSlug()).isEqualTo("books");
        assertThatThrownBy(() -> categoryService.getCategoryById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createBuildsSlugFromName() {
        when(categoryRepository.existsByName("Home & Kitchen")).thenReturn(false);
        when(categoryRepository.existsBySlug("home-kitchen")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(categoryService.createCategory(dto("Home & Kitchen")).getSlug()).isEqualTo("home-kitchen");
    }

    @Test
    void createRejectsDuplicateNameOrSlug() {
        when(categoryRepository.existsByName("Books")).thenReturn(true);
        when(categoryRepository.existsByName("Books!")).thenReturn(false);
        when(categoryRepository.existsBySlug("books")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(dto("Books"))).hasMessage("Category name already exists");
        assertThatThrownBy(() -> categoryService.createCategory(dto("Books!"))).hasMessage("Category slug already exists");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateRenamesCategoryAndSlug() {
        Category books = category(1L, "Books");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(books));
        when(categoryRepository.existsByName("Comics")).thenReturn(false);
        when(categoryRepository.save(books)).thenReturn(books);

        CategoryDto updated = categoryService.updateCategory(1L, dto("Comics"));

        assertThat(updated.getName()).isEqualTo("Comics");
        assertThat(updated.getSlug()).isEqualTo("comics");
    }

    @Test
    void updateRejectsNameOwnedByAnotherCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Books")));
        when(categoryRepository.existsByName("Toys")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, dto("Toys"))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteRemovesEmptyCategory() {
        Category books = category(1L, "Books");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(books));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(books);
    }

    @Test
    void deleteRefusesCategoryThatStillHasProducts() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Books")));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("associated products");
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
