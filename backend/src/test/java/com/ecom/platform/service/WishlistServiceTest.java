package com.ecom.platform.service;

import com.ecom.platform.dto.WishlistDto;
import com.ecom.platform.entity.Product;
import com.ecom.platform.entity.User;
import com.ecom.platform.entity.Wishlist;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.CategoryRepository;
import com.ecom.platform.repository.ProductRepository;
import com.ecom.platform.repository.ReviewRepository;
import com.ecom.platform.repository.UserRepository;
import com.ecom.platform.repository.WishlistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static com.ecom.platform.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock private WishlistRepository wishlistRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    private WishlistService wishlistService;
    private User customer;
    private Wishlist wishlist;

    @BeforeEach
    void setUp() {
        // ProductService is only used for its DTO mapping, so use the real one
        ProductService productService = new ProductService(productRepository, mock(CategoryRepository.class),
                mock(ReviewRepository.class), userRepository);
        wishlistService = new WishlistService(wishlistRepository, productRepository, userRepository, productService);

        customer = customer(1L, "rohan@example.com");
        wishlist = Wishlist.builder().id(30L).user(customer).build();
        authenticateAs(customer);
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        lenient().when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getWishlistCreatesOneWhenMissing() {
        when(wishlistRepository.findByUser(customer)).thenReturn(Optional.empty());

        WishlistDto dto = wishlistService.getWishlist();

        assertThat(dto.getProducts()).isEmpty();
        verify(wishlistRepository).save(any(Wishlist.class));
    }

    @Test
    void addProductIsIdempotent() {
        Product phone = product(7L, "Phone", "100.00", 5);
        when(wishlistRepository.findByUser(customer)).thenReturn(Optional.of(wishlist));
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(phone));

        wishlistService.addProductToWishlist(7L);
        WishlistDto dto = wishlistService.addProductToWishlist(7L);

        assertThat(dto.getProducts()).singleElement().satisfies(p -> assertThat(p.getName()).isEqualTo("Phone"));
    }

    @Test
    void addProductRejectsUnknownProduct() {
        when(wishlistRepository.findByUser(customer)).thenReturn(Optional.of(wishlist));
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.addProductToWishlist(7L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeProduct() {
        Product phone = product(7L, "Phone", "100.00", 5);
        wishlist.getProducts().add(phone);
        when(wishlistRepository.findByUser(customer)).thenReturn(Optional.of(wishlist));
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(phone));

        assertThat(wishlistService.removeProductFromWishlist(7L).getProducts()).isEmpty();
    }

    @Test
    void removeProductRequiresExistingWishlist() {
        when(wishlistRepository.findByUser(customer)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.removeProductFromWishlist(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Wishlist not found");
    }
}
