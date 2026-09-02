package com.ecom.platform.service;

import com.ecom.platform.dto.AddToCartRequest;
import com.ecom.platform.dto.CartDto;
import com.ecom.platform.dto.CartItemDto;
import com.ecom.platform.dto.UpdateCartItemRequest;
import com.ecom.platform.entity.*;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
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

    @Transactional
    public CartDto getCart() {
        User user = getAuthenticatedUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
        return convertToDto(cart);
    }

    @Transactional
    public CartDto addItemToCart(AddToCartRequest request) {
        User user = getAuthenticatedUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });

        Product product = productRepository.findByIdAndDeletedFalse(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Only " + product.getStockQuantity() + " units available in stock.");
        }

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> CartItem.builder().cart(cart).product(product).quantity(0).build());

        int newQuantity = cartItem.getQuantity() + request.getQuantity();
        if (product.getStockQuantity() < newQuantity) {
            throw new BadRequestException("Cannot add " + request.getQuantity() + " more units. Total quantity in cart would exceed available stock (" + product.getStockQuantity() + ").");
        }

        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);

        // Ensure the cart's items collection is updated in memory
        if (!cart.getItems().contains(cartItem)) {
            cart.getItems().add(cartItem);
        }

        return convertToDto(cart);
    }

    @Transactional
    public CartDto updateItemQuantity(Long productId, UpdateCartItemRequest request) {
        User user = getAuthenticatedUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Only " + product.getStockQuantity() + " units available in stock.");
        }

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product is not in your cart"));

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        // Refresh cart
        Cart refreshedCart = cartRepository.findById(cart.getId()).orElse(cart);
        return convertToDto(refreshedCart);
    }

    @Transactional
    public CartDto removeItemFromCart(Long productId) {
        User user = getAuthenticatedUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product is not in your cart"));

        cartItemRepository.delete(cartItem);
        cart.getItems().remove(cartItem);
        cartRepository.save(cart);

        // Refresh cart
        Cart refreshedCart = cartRepository.findById(cart.getId()).orElse(cart);
        return convertToDto(refreshedCart);
    }

    @Transactional
    public void clearCart(Cart cart) {
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    public CartDto convertToDto(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(item -> {
            BigDecimal price = item.getProduct().getFinalPrice();
            BigDecimal subTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            return CartItemDto.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .productSlug(item.getProduct().getSlug())
                    .productImageUrl(item.getProduct().getImageUrl())
                    .productPrice(price)
                    .quantity(item.getQuantity())
                    .stockQuantity(item.getProduct().getStockQuantity())
                    .subTotal(subTotal)
                    .build();
        }).collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(CartItemDto::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartDto.builder()
                .id(cart.getId())
                .items(items)
                .totalAmount(total)
                .build();
    }
}
