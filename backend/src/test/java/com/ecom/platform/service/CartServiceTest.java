package com.ecom.platform.service;

import com.ecom.platform.dto.AddToCartRequest;
import com.ecom.platform.dto.CartDto;
import com.ecom.platform.dto.UpdateCartItemRequest;
import com.ecom.platform.entity.Cart;
import com.ecom.platform.entity.CartItem;
import com.ecom.platform.entity.Product;
import com.ecom.platform.entity.User;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.CartItemRepository;
import com.ecom.platform.repository.CartRepository;
import com.ecom.platform.repository.ProductRepository;
import com.ecom.platform.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User customer;
    private Cart cart;
    private Product phone;

    @BeforeEach
    void setUp() {
        customer = customer(1L, "rohan@example.com");
        cart = Cart.builder().id(20L).user(customer).build();
        phone = product(7L, "Phone", "100.00", 5);
        authenticateAs(customer);
        lenient().when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private CartItem itemInCart(Product product, int quantity) {
        CartItem item = CartItem.builder().id(100L + product.getId()).cart(cart).product(product).quantity(quantity).build();
        cart.getItems().add(item);
        return item;
    }

    @Test
    void getCartCreatesEmptyCartOnFirstVisit() {
        when(cartRepository.findByUser(customer)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart created = inv.getArgument(0);
            created.setId(21L);
            return created;
        });

        CartDto dto = cartService.getCart();

        assertThat(dto.getId()).isEqualTo(21L);
        assertThat(dto.getItems()).isEmpty();
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void getCartTotalsUseDiscountedPrices() {
        Product deal = product(8L, "Headphones", "200.00", 5);
        deal.setTopDeal(true);
        deal.setDiscountPercentage(50);
        itemInCart(phone, 2);
        itemInCart(deal, 1);
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));

        CartDto dto = cartService.getCart();

        // 2 x 100 + 1 x (200 - 50%)
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(dto.getItems()).extracting(i -> i.getProductName()).containsExactly("Phone", "Headphones");
        assertThat(dto.getItems().get(1).getProductPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void addItemCreatesNewCartLine() {
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(phone));
        when(cartItemRepository.findByCartIdAndProductId(20L, 7L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem saved = inv.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        CartDto dto = cartService.addItemToCart(new AddToCartRequest(7L, 2));

        assertThat(dto.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getSubTotal()).isEqualByComparingTo("200.00");
        });
    }

    @Test
    void addItemIncrementsExistingLine() {
        CartItem existing = itemInCart(phone, 2);
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(phone));
        when(cartItemRepository.findByCartIdAndProductId(20L, 7L)).thenReturn(Optional.of(existing));

        CartDto dto = cartService.addItemToCart(new AddToCartRequest(7L, 3));

        assertThat(existing.getQuantity()).isEqualTo(5);
        assertThat(dto.getItems()).hasSize(1);
        verify(cartItemRepository).save(existing);
    }

    @Test
    void addItemRejectsQuantityAboveStock() {
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(phone));

        assertThatThrownBy(() -> cartService.addItemToCart(new AddToCartRequest(7L, 6)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only 5 units available in stock.");
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItemRejectsWhenCombinedQuantityWouldExceedStock() {
        CartItem existing = itemInCart(phone, 4);
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(phone));
        when(cartItemRepository.findByCartIdAndProductId(20L, 7L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cartService.addItemToCart(new AddToCartRequest(7L, 2)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceed available stock");
        assertThat(existing.getQuantity()).isEqualTo(4);
    }

    @Test
    void addItemRejectsUnknownProduct() {
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(new AddToCartRequest(7L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateQuantitySetsAbsoluteValue() {
        CartItem existing = itemInCart(phone, 1);
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(phone));
        when(cartItemRepository.findByCartIdAndProductId(20L, 7L)).thenReturn(Optional.of(existing));
        when(cartRepository.findById(20L)).thenReturn(Optional.of(cart));

        CartDto dto = cartService.updateItemQuantity(7L, new UpdateCartItemRequest(4));

        assertThat(existing.getQuantity()).isEqualTo(4);
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    void updateQuantityRejectsMoreThanStock() {
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(phone));

        assertThatThrownBy(() -> cartService.updateItemQuantity(7L, new UpdateCartItemRequest(9)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateQuantityRejectsProductNotInCart() {
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(phone));
        when(cartItemRepository.findByCartIdAndProductId(20L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(7L, new UpdateCartItemRequest(1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product is not in your cart");
    }

    @Test
    void updateQuantityRequiresExistingCart() {
        when(cartRepository.findByUser(customer)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(7L, new UpdateCartItemRequest(1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart not found");
    }

    @Test
    void removeItemDeletesLine() {
        CartItem existing = itemInCart(phone, 1);
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(20L, 7L)).thenReturn(Optional.of(existing));
        when(cartRepository.findById(20L)).thenReturn(Optional.of(cart));

        CartDto dto = cartService.removeItemFromCart(7L);

        assertThat(dto.getItems()).isEmpty();
        verify(cartItemRepository).delete(existing);
    }

    @Test
    void removeItemRejectsProductNotInCart() {
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(20L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItemFromCart(7L)).isInstanceOf(ResourceNotFoundException.class);
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void clearCartDeletesAllLines() {
        itemInCart(phone, 1);

        cartService.clearCart(cart);

        assertThat(cart.getItems()).isEmpty();
        verify(cartItemRepository).deleteByCartId(20L);
        verify(cartRepository).save(cart);
    }
}
