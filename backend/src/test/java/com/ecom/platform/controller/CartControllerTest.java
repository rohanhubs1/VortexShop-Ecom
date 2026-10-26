package com.ecom.platform.controller;

import com.ecom.platform.config.JwtTokenProvider;
import com.ecom.platform.config.SecurityConfig;
import com.ecom.platform.dto.AddToCartRequest;
import com.ecom.platform.dto.CartDto;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.service.CartService;
import com.ecom.platform.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import(SecurityConfig.class)
class CartControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CartService cartService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private CartDto emptyCart() {
        return CartDto.builder().id(20L).items(List.of()).totalAmount(BigDecimal.ZERO).build();
    }

    @Test
    void anonymousUserCannotSeeCart() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isForbidden());
        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminIsNotACustomer() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isForbidden());
        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerGetsCart() throws Exception {
        when(cartService.getCart()).thenReturn(emptyCart());

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void addItem() throws Exception {
        when(cartService.addItemToCart(any(AddToCartRequest.class))).thenReturn(emptyCart());

        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":7,\"quantity\":2}"))
                .andExpect(status().isOk());

        verify(cartService).addItemToCart(argThat(r -> r.getProductId() == 7L && r.getQuantity() == 2));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void addItemValidatesQuantityAndProductId() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.quantity").value("Quantity must be at least 1"))
                .andExpect(jsonPath("$.details.productId").value("Product ID is required"));
        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void stockErrorIsReturnedAs400() throws Exception {
        when(cartService.addItemToCart(any())).thenThrow(new BadRequestException("Only 5 units available in stock."));

        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":7,\"quantity\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only 5 units available in stock."));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateAndRemoveItem() throws Exception {
        when(cartService.updateItemQuantity(eq(7L), any())).thenReturn(emptyCart());
        when(cartService.removeItemFromCart(7L)).thenReturn(emptyCart());

        mockMvc.perform(put("/api/cart/items/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/cart/items/7"))
                .andExpect(status().isOk());

        verify(cartService).updateItemQuantity(eq(7L), argThat(r -> r.getQuantity() == 3));
        verify(cartService).removeItemFromCart(7L);
    }
}
