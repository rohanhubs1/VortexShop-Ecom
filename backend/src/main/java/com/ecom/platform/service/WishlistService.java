package com.ecom.platform.service;

import com.ecom.platform.dto.ProductDto;
import com.ecom.platform.dto.WishlistDto;
import com.ecom.platform.entity.*;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.ProductRepository;
import com.ecom.platform.repository.UserRepository;
import com.ecom.platform.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

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
    public WishlistDto getWishlist() {
        User user = getAuthenticatedUser();
        Wishlist wishlist = wishlistRepository.findByUser(user)
                .orElseGet(() -> {
                    Wishlist newWishlist = Wishlist.builder().user(user).build();
                    return wishlistRepository.save(newWishlist);
                });
        return convertToDto(wishlist);
    }

    @Transactional
    public WishlistDto addProductToWishlist(Long productId) {
        User user = getAuthenticatedUser();
        Wishlist wishlist = wishlistRepository.findByUser(user)
                .orElseGet(() -> {
                    Wishlist newWishlist = Wishlist.builder().user(user).build();
                    return wishlistRepository.save(newWishlist);
                });

        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        wishlist.getProducts().add(product);
        Wishlist saved = wishlistRepository.save(wishlist);
        return convertToDto(saved);
    }

    @Transactional
    public WishlistDto removeProductFromWishlist(Long productId) {
        User user = getAuthenticatedUser();
        Wishlist wishlist = wishlistRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found"));

        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        wishlist.getProducts().remove(product);
        Wishlist saved = wishlistRepository.save(wishlist);
        return convertToDto(saved);
    }

    private WishlistDto convertToDto(Wishlist wishlist) {
        List<ProductDto> products = wishlist.getProducts().stream()
                .map(productService::convertToDto)
                .collect(Collectors.toList());

        return WishlistDto.builder()
                .id(wishlist.getId())
                .products(products)
                .build();
    }
}
