package com.ecom.platform.service;

import com.ecom.platform.dto.CreateReviewRequest;
import com.ecom.platform.dto.ProductCreateRequest;
import com.ecom.platform.dto.ProductDto;
import com.ecom.platform.dto.ReviewDto;
import com.ecom.platform.entity.*;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.CategoryRepository;
import com.ecom.platform.repository.ProductRepository;
import com.ecom.platform.repository.ReviewRepository;
import com.ecom.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        throw new BadRequestException("Not authenticated");
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product-search", key = "#keyword + '-' + #categoryId + '-' + #minPrice + '-' + #maxPrice + '-' + #minRating + '-' + (#isTopDeal != null ? #isTopDeal.toString() : 'null') + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<ProductDto> searchProducts(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Double minRating, Boolean isTopDeal, Pageable pageable) {
        log.info("Fetching products from DB for keyword: {}, categoryId: {}, isTopDeal: {}", keyword, categoryId, isTopDeal);
        Page<Product> productPage = productRepository.searchProducts(
                keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null,
                categoryId, minPrice, maxPrice, minRating, isTopDeal, pageable
        );
        return productPage.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getTopDeals(Pageable pageable) {
        return productRepository.findByIsTopDealTrueAndDeletedFalse(pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductDto getProductById(Long id) {
        log.info("Fetching product ID: {} from DB", id);
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or soft-deleted"));
        return convertToDto(product);
    }

    @Transactional(readOnly = true)
    public ProductDto getProductBySlug(String slug) {
        Product product = productRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        return convertToDto(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "product-search"}, allEntries = true)
    public ProductDto createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String slug = generateSlug(request.getName());
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 1000;
        }

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .averageRating(0.0)
                .deleted(false)
                .build();

        Product saved = productRepository.save(product);
        return convertToDto(saved);
    }

    @Transactional
    @CacheEvict(value = {"products", "product-search"}, allEntries = true)
    public ProductDto updateProduct(Long id, ProductCreateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setCategory(category);
        product.setName(request.getName());
        product.setSlug(generateSlug(request.getName()));
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }

        Product updated = productRepository.save(product);
        return convertToDto(updated);
    }

    @Transactional
    @CacheEvict(value = {"products", "product-search"}, allEntries = true)
    public void softDeleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setDeleted(true);
        productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "product-search"}, allEntries = true)
    public ReviewDto addReview(CreateReviewRequest request) {
        User user = getAuthenticatedUser();
        Product product = productRepository.findByIdAndDeletedFalse(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (reviewRepository.existsByUserIdAndProductId(user.getId(), product.getId())) {
            throw new BadRequestException("You have already reviewed this product.");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .imageUrl(request.getImageUrl())
                .build();

        Review savedReview = reviewRepository.save(review);

        // Recalculate average rating for product
        List<Review> reviews = reviewRepository.findByProductId(product.getId());
        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        product.setAverageRating(Math.round(average * 10.0) / 10.0);
        productRepository.save(product);

        return convertToReviewDto(savedReview);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getProductReviews(Long productId) {
        return reviewRepository.findByProductId(productId).stream()
                .map(this::convertToReviewDto)
                .collect(Collectors.toList());
    }

    private String generateSlug(String input) {
        if (input == null) return "";
        return input.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
    }

    public ProductDto convertToDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .salesCount(product.getSalesCount())
                .imageUrl(product.getImageUrl())
                .averageRating(product.getAverageRating())
                .isTopDeal(product.isTopDeal())
                .discountPercentage(product.getDiscountPercentage())
                .offerExpiryDate(product.getOfferExpiryDate())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private ReviewDto convertToReviewDto(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .reviewerName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .productId(review.getProduct().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
