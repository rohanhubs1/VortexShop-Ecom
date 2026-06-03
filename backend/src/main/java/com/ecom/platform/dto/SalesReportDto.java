package com.ecom.platform.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportDto {
    private BigDecimal totalRevenue;
    private long totalOrdersPlaced;
    private long totalItemsSold;
    private BigDecimal averageOrderValue;
    private List<OrderDto> orders;
}
