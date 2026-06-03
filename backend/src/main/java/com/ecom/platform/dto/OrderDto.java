package com.ecom.platform.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {
    private Long id;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private AddressDto shippingAddress;
    private String couponCode;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;
    private String customerName;
    private String customerEmail;
}
