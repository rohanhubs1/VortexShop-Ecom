package com.ecom.platform.controller;

import com.ecom.platform.dto.AddToCartRequest;
import com.ecom.platform.dto.CartDto;
import com.ecom.platform.dto.UpdateCartItemRequest;
import com.ecom.platform.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Shopping Cart", description = "Endpoints for customer shopping cart items management")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current authenticated customer's shopping cart")
    public ResponseEntity<CartDto> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @PostMapping
    @Operation(summary = "Add an item to the shopping cart")
    public ResponseEntity<CartDto> addItemToCart(@Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItemToCart(request));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<CartDto> updateCartItemQuantity(@PathVariable Long productId, @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(productId, request));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Delete an item from shopping cart")
    public ResponseEntity<CartDto> removeItemFromCart(@PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItemFromCart(productId));
    }
}
