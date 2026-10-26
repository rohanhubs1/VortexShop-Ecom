package com.ecom.platform.service;

import com.ecom.platform.dto.AdminDashboardDto;
import com.ecom.platform.dto.ProductDto;
import com.ecom.platform.entity.*;
import com.ecom.platform.repository.OrderRepository;
import com.ecom.platform.repository.ProductRepository;
import com.ecom.platform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.ecom.platform.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Order order(OrderStatus status, String amount, LocalDateTime createdAt, Product product, int qty) {
        Order order = Order.builder().status(status).finalAmount(new BigDecimal(amount)).createdAt(createdAt).build();
        order.getItems().add(OrderItem.builder().product(product).quantity(qty).build());
        return order;
    }

    @Test
    void aggregatesSalesStatusCountsAndTopSellers() {
        Product phone = product(7L, "Phone", "100.00", 5);
        Product case_ = product(8L, "Case", "10.00", 50);
        LocalDateTime now = LocalDateTime.now();
        when(orderRepository.findAll()).thenReturn(List.of(
                order(OrderStatus.DELIVERED, "200.00", now, phone, 2),
                order(OrderStatus.PENDING, "50.00", now.minusDays(1), case_, 5),
                order(OrderStatus.CANCELLED, "999.00", now, phone, 9),
                order(OrderStatus.SHIPPED, "70.00", now.minusDays(30), case_, 1)));
        when(userRepository.findAll()).thenReturn(List.of(
                customer(1L, "a@example.com"), customer(2L, "b@example.com"), user(3L, "admin@example.com", RoleName.ROLE_ADMIN)));
        when(productRepository.count()).thenReturn(12L);
        when(productRepository.findById(7L)).thenReturn(Optional.of(phone));
        when(productRepository.findById(8L)).thenReturn(Optional.of(case_));

        AdminDashboardDto stats = dashboardService.getDashboardStats();

        assertThat(stats.getTotalSales()).isEqualByComparingTo("320.00");
        assertThat(stats.getTotalOrders()).isEqualTo(4);
        assertThat(stats.getTotalCustomers()).isEqualTo(2);
        assertThat(stats.getTotalProducts()).isEqualTo(12);
        assertThat(stats.getOrderStatusCounts())
                .containsEntry("CANCELLED", 1L)
                .containsEntry("CONFIRMED", 0L)
                .hasSize(OrderStatus.values().length);

        // last 7 days only, cancelled orders excluded
        assertThat(stats.getDailySales()).hasSize(7);
        assertThat(stats.getDailySales().get(6).getSales()).isEqualByComparingTo("200.00");
        assertThat(stats.getDailySales().get(5).getSales()).isEqualByComparingTo("50.00");

        // case: 5 + 1 sold, phone: 2 sold (the 9 cancelled do not count)
        assertThat(stats.getTopSellingProducts()).extracting(ProductDto::getName).containsExactly("Case", "Phone");
    }
}
