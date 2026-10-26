package com.ecom.platform.controller;

import com.ecom.platform.config.JwtTokenProvider;
import com.ecom.platform.config.SecurityConfig;
import com.ecom.platform.dto.OrderDto;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.service.CustomUserDetailsService;
import com.ecom.platform.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "CUSTOMER")
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private OrderService orderService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private OrderDto order(String status) {
        return OrderDto.builder().id(40L).orderNumber("ORD-ABCD1234").status(status)
                .finalAmount(new BigDecimal("180.00")).items(List.of()).build();
    }

    @Test
    void placeOrderReturns201() throws Exception {
        when(orderService.placeOrder(any())).thenReturn(order("PENDING"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddressId\":5,\"couponCode\":\"SAVE10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-ABCD1234"))
                .andExpect(jsonPath("$.finalAmount").value(180.00));

        verify(orderService).placeOrder(argThat(r -> r.getShippingAddressId() == 5L && "SAVE10".equals(r.getCouponCode())));
    }

    @Test
    void placeOrderRequiresShippingAddress() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.shippingAddressId").value("Shipping address ID is required"));
        verifyNoInteractions(orderService);
    }

    @Test
    void businessRuleViolationIsReturnedAs400() throws Exception {
        when(orderService.placeOrder(any())).thenThrow(new BadRequestException("Coupon already claimed"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddressId\":5,\"couponCode\":\"SAVE10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Coupon already claimed"));
    }

    @Test
    void orderHistoryIsNewestFirst() throws Exception {
        when(orderService.getMyOrderHistory(any())).thenReturn(new PageImpl<>(List.of(order("PENDING"))));

        mockMvc.perform(get("/api/orders").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).getMyOrderHistory(pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void orderDetailsAndNotFound() throws Exception {
        when(orderService.getOrderDetails(40L)).thenReturn(order("PENDING"));
        when(orderService.getOrderDetails(41L)).thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/40")).andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/41"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void cancelOrder() throws Exception {
        when(orderService.cancelOrder(40L)).thenReturn(order("CANCELLED"));

        mockMvc.perform(post("/api/orders/40/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotPlaceCustomerOrders() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddressId\":5}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orderService);
    }
}
