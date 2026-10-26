package com.ecom.platform.service;

import com.ecom.platform.dto.CreateReviewRequest;
import com.ecom.platform.dto.ProductCreateRequest;
import com.ecom.platform.dto.ProductDto;
import com.ecom.platform.dto.ReviewDto;
import com.ecom.platform.entity.Category;
import com.ecom.platform.entity.Product;
import com.ecom.platform.entity.Review;
import com.ecom.platform.entity.User;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.CategoryRepository;
import com.ecom.platform.repository.ProductRepository;
import com.ecom.platform.repository.ReviewRepository;
import com.ecom.platform.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.ecom.platform.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    private final Pageable pageable = PageRequest.of(0, 12);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private ProductCreateRequest request(String name) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryId(1L);
        request.setName(name);
        request.setDescription("desc");
        request.setPrice(new BigDecimal("499.99"));
        request.setStockQuantity(20);
        return request;
    }

    @Test
    void searchTrimsKeywordAndPassesFiltersThrough() {
        when(productRepository.searchProducts(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(product(1L, "Phone", "100.00", 5))));

        Page<ProductDto> page = productService.searchProducts("  phone  ", 3L, new BigDecimal("10"), null, 4.0, true, pageable);

        assertThat(page.getContent()).extracting(ProductDto::getName).containsExactly("Phone");
        verify(productRepository).searchProducts(eq("phone"), eq(3L), eq(new BigDecimal("10")), isNull(), eq(4.0), eq(true),
                any(LocalDateTime.class), eq(pageable));
    }

    @Test
    void searchTreatsBlankKeywordAsNoKeyword() {
        when(productRepository.searchProducts(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        productService.searchProducts("   ", null, null, null, null, null, pageable);

        verify(productRepository).searchProducts(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                any(LocalDateTime.class), eq(pageable));
    }

    @Test
    void topDealsComeFromActiveDealQuery() {
        Product deal = product(1L, "Phone", "100.00", 5);
        deal.setTopDeal(true);
        deal.setDiscountPercentage(10);
        when(productRepository.findActiveTopDeals(any(LocalDateTime.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(deal)));

        ProductDto dto = productService.getTopDeals(pageable).getContent().get(0);

        assertThat(dto.isTopDeal()).isTrue();
        assertThat(dto.getDiscountPercentage()).isEqualTo(10);
    }

    @Test
    void getProductByIdMapsCategory() {
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product(1L, "Phone", "100.00", 5)));

        ProductDto dto = productService.getProductById(1L);

        assertThat(dto.getCategoryName()).isEqualTo("Electronics");
        assertThat(dto.getPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void getProductByIdThrowsForMissingOrDeletedProduct() {
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProductBySlug() {
        when(productRepository.findBySlugAndDeletedFalse("phone")).thenReturn(Optional.of(product(1L, "Phone", "100.00", 5)));
        when(productRepository.findBySlugAndDeletedFalse("ghost")).thenReturn(Optional.empty());

        assertThat(productService.getProductBySlug("phone").getId()).isEqualTo(1L);
        assertThatThrownBy(() -> productService.getProductBySlug("ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void createProductGeneratesSlugFromName() {
        Category category = category(1L, "Electronics");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug("gaming-laptop-pro-15")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDto dto = productService.createProduct(request("Gaming Laptop Pro 15!"));

        assertThat(dto.getSlug()).isEqualTo("gaming-laptop-pro-15");
        assertThat(dto.getStockQuantity()).isEqualTo(20);
        assertThat(dto.getAverageRating()).isEqualTo(0.0);
        assertThat(dto.isDeleted()).isFalse();
    }

    @Test
    void createProductMakesSlugUniqueOnCollision() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Electronics")));
        when(productRepository.existsBySlug("phone")).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDto dto = productService.createProduct(request("Phone"));

        assertThat(dto.getSlug()).matches("phone-\\d{1,3}");
    }

    @Test
    void createProductRequiresExistingCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request("Phone")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProductKeepsExistingImageWhenNoneProvided() {
        Product existing = product(1L, "Phone", "100.00", 5);
        existing.setImageUrl("/api/products/images/old.png");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Electronics")));
        when(productRepository.existsBySlugAndIdNot("phone-x", 1L)).thenReturn(false);
        when(productRepository.save(existing)).thenReturn(existing);

        ProductCreateRequest update = request("Phone X");
        update.setTopDeal(true);
        update.setDiscountPercentage(15);
        ProductDto dto = productService.updateProduct(1L, update);

        assertThat(dto.getName()).isEqualTo("Phone X");
        assertThat(dto.getSlug()).isEqualTo("phone-x");
        assertThat(dto.getPrice()).isEqualByComparingTo("499.99");
        assertThat(dto.getImageUrl()).isEqualTo("/api/products/images/old.png");
        assertThat(dto.isTopDeal()).isTrue();
        assertThat(dto.getDiscountPercentage()).isEqualTo(15);
    }

    @Test
    void updateProductReplacesImageAndDedupesSlug() {
        Product existing = product(1L, "Phone", "100.00", 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Electronics")));
        when(productRepository.existsBySlugAndIdNot("tablet", 1L)).thenReturn(true);
        when(productRepository.save(existing)).thenReturn(existing);

        ProductCreateRequest update = request("Tablet");
        update.setImageUrl("/api/products/images/new.png");
        ProductDto dto = productService.updateProduct(1L, update);

        assertThat(dto.getImageUrl()).isEqualTo("/api/products/images/new.png");
        assertThat(dto.getSlug()).startsWith("tablet-");
    }

    @Test
    void updateProductThrowsWhenProductMissing() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(1L, request("Phone")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void softDeleteMarksProductDeletedInsteadOfRemovingIt() {
        Product existing = product(1L, "Phone", "100.00", 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.softDeleteProduct(1L);

        assertThat(existing.isDeleted()).isTrue();
        verify(productRepository).save(existing);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void softDeleteThrowsWhenProductMissing() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.softDeleteProduct(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addReviewRecalculatesAverageRatingToOneDecimal() {
        User reviewer = customer(2L, "reviewer@example.com");
        authenticateAs(reviewer);
        Product phone = product(1L, "Phone", "100.00", 5);
        when(userRepository.findByEmail("reviewer@example.com")).thenReturn(Optional.of(reviewer));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(phone));
        when(reviewRepository.existsByUserIdAndProductId(2L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.findByProductId(1L)).thenReturn(List.of(
                Review.builder().rating(4).build(),
                Review.builder().rating(5).build(),
                Review.builder().rating(5).build()));

        ReviewDto dto = productService.addReview(new CreateReviewRequest(1L, 5, "Great phone", null));

        assertThat(phone.getAverageRating()).isEqualTo(4.7);
        assertThat(dto.getReviewerName()).isEqualTo("Test User");
        assertThat(dto.getRating()).isEqualTo(5);
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(reviewer);
    }

    @Test
    void addReviewRejectsSecondReviewFromSameUser() {
        User reviewer = customer(2L, "reviewer@example.com");
        authenticateAs(reviewer);
        when(userRepository.findByEmail("reviewer@example.com")).thenReturn(Optional.of(reviewer));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product(1L, "Phone", "100.00", 5)));
        when(reviewRepository.existsByUserIdAndProductId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.addReview(new CreateReviewRequest(1L, 5, "Again", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already reviewed");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void getProductReviewsMapsReviewer() {
        User reviewer = customer(2L, "reviewer@example.com");
        Product phone = product(1L, "Phone", "100.00", 5);
        when(reviewRepository.findByProductId(1L)).thenReturn(List.of(
                Review.builder().id(9L).user(reviewer).product(phone).rating(3).comment("ok").build()));

        List<ReviewDto> reviews = productService.getProductReviews(1L);

        assertThat(reviews).singleElement().satisfies(r -> {
            assertThat(r.getUserId()).isEqualTo(2L);
            assertThat(r.getProductId()).isEqualTo(1L);
            assertThat(r.getComment()).isEqualTo("ok");
        });
    }
}
