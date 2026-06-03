package com.ecom.platform.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardDto {
    private BigDecimal totalSales;
    private long totalOrders;
    private long totalCustomers;
    private long totalProducts;
    private Map<String, Long> orderStatusCounts;
    private List<DailySalesDto> dailySales;
    private List<ProductDto> topSellingProducts;
}
