package com.ecom.platform.controller;

import com.ecom.platform.dto.CreateReviewRequest;
import com.ecom.platform.dto.ProductDto;
import com.ecom.platform.dto.ReviewDto;
import com.ecom.platform.service.ImageStorageService;
import com.ecom.platform.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Catalog", description = "Public endpoints for catalog browsing, searches, and reviews")
public class ProductController {

    private final ProductService productService;
    private final ImageStorageService imageStorageService;

    @GetMapping
    @Operation(summary = "Search, filter, sort and list products with pagination")
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
        
        return ResponseEntity.ok(productService.searchProducts(keyword, categoryId, minPrice, maxPrice, minRating, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information of a product by ID")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product information by slug")
    public ResponseEntity<ProductDto> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getProductBySlug(slug));
    }

    // Reviews
    @PostMapping("/reviews")
    @Operation(summary = "Add a product rating and review")
    public ResponseEntity<ReviewDto> addReview(@Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.ok(productService.addReview(request));
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "List all reviews for a product")
    public ResponseEntity<List<ReviewDto>> getProductReviews(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductReviews(id));
    }

    // Serving images locally
    @GetMapping("/images/{filename:.+}")
    @Operation(summary = "Retrieve uploaded product images")
    public ResponseEntity<byte[]> getProductImage(@PathVariable String filename) {
        byte[] imageBytes = imageStorageService.loadFile(filename);
        
        MediaType contentType = MediaType.IMAGE_JPEG;
        if (filename.toLowerCase().endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG;
        } else if (filename.toLowerCase().endsWith(".gif")) {
            contentType = MediaType.IMAGE_GIF;
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(imageBytes);
    }
}
