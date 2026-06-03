package com.ecom.platform.controller;

import com.ecom.platform.dto.WishlistDto;
import com.ecom.platform.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Endpoints for customer wishlist management")
@PreAuthorize("hasRole('CUSTOMER')")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get current customer's wishlist")
    public ResponseEntity<WishlistDto> getWishlist() {
        return ResponseEntity.ok(wishlistService.getWishlist());
    }

    @PostMapping("/products/{productId}")
    @Operation(summary = "Add a product to wishlist")
    public ResponseEntity<WishlistDto> addProductToWishlist(@PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.addProductToWishlist(productId));
    }

    @DeleteMapping("/products/{productId}")
    @Operation(summary = "Remove a product from wishlist")
    public ResponseEntity<WishlistDto> removeProductFromWishlist(@PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.removeProductFromWishlist(productId));
    }
}
