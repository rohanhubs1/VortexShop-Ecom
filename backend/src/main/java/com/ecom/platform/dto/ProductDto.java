package com.ecom.platform.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private int salesCount;
    private String imageUrl;
    private Double averageRating;
    @com.fasterxml.jackson.annotation.JsonProperty("isTopDeal")
    private boolean isTopDeal;
    private int discountPercentage;
    private LocalDateTime offerExpiryDate;
    private LocalDateTime createdAt;
    private boolean deleted;
}
