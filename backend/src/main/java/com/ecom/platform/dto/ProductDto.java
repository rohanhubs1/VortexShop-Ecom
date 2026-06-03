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
    private LocalDateTime createdAt;
}
