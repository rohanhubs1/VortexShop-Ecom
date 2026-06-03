package com.ecom.platform.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productImageUrl;
    private int quantity;
    private BigDecimal price;
}
