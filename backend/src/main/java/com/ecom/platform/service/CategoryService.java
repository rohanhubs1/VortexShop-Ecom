package com.ecom.platform.service;

import com.ecom.platform.dto.CategoryDto;
import com.ecom.platform.entity.Category;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return convertToDto(category);
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new BadRequestException("Category name already exists");
        }

        String slug = generateSlug(dto.getName());
        if (categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("Category slug already exists");
        }

        Category category = Category.builder()
                .name(dto.getName())
                .slug(slug)
                .description(dto.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        return convertToDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getName().equalsIgnoreCase(dto.getName()) && categoryRepository.existsByName(dto.getName())) {
            throw new BadRequestException("Category name already exists");
        }

        category.setName(dto.getName());
        category.setSlug(generateSlug(dto.getName()));
        category.setDescription(dto.getDescription());

        Category updated = categoryRepository.save(category);
        return convertToDto(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        categoryRepository.delete(category);
    }

    private String generateSlug(String input) {
        if (input == null) return "";
        return input.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
    }

    private CategoryDto convertToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .build();
    }
}
