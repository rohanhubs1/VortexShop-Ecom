package com.ecom.platform.service;

import com.ecom.platform.dto.AdminDashboardDto;
import com.ecom.platform.dto.DailySalesDto;
import com.ecom.platform.entity.Order;
import com.ecom.platform.entity.OrderItem;
import com.ecom.platform.entity.OrderStatus;
import com.ecom.platform.dto.ProductDto;
import com.ecom.platform.repository.OrderRepository;
import com.ecom.platform.repository.ProductRepository;
import com.ecom.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboardStats() {
        List<Order> orders = orderRepository.findAll();

        BigDecimal totalSales = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCustomers = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_CUSTOMER")))
                .count();

        long totalProducts = productRepository.count();

        Map<String, Long> statusCounts = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));

        // Ensure all statuses exist in the map
        for (OrderStatus status : OrderStatus.values()) {
            statusCounts.putIfAbsent(status.name(), 0L);
        }

        // Calculate daily sales for the last 7 days
        List<DailySalesDto> dailySales = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        LocalDate today = LocalDate.now();
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            BigDecimal dailyTotal = orders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .filter(o -> o.getCreatedAt().toLocalDate().equals(date))
                    .map(Order::getFinalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            dailySales.add(new DailySalesDto(date.format(formatter), dailyTotal));
        }

        // Compute top selling products
        Map<Long, Long> productSales = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(item -> item.getProduct().getId(), Collectors.summingLong(OrderItem::getQuantity)));

        List<ProductDto> topSellingProducts = productSales.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> productRepository.findById(entry.getKey()).orElse(null))
                .filter(p -> p != null)
                .map(p -> ProductDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .price(p.getPrice())
                        .stockQuantity(p.getStockQuantity())
                        .build())
                .collect(Collectors.toList());

        return AdminDashboardDto.builder()
                .totalSales(totalSales)
                .totalOrders(orders.size())
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .orderStatusCounts(statusCounts)
                .dailySales(dailySales)
                .topSellingProducts(topSellingProducts)
                .build();
    }
}
