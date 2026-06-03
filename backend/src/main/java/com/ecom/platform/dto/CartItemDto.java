package com.ecom.platform.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productImageUrl;
    private BigDecimal productPrice;
    private int quantity;
    private int stockQuantity;
    private BigDecimal subTotal;
}
