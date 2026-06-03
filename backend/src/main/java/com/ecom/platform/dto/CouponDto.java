package com.ecom.platform.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponDto {
    private Long id;
    private String code;
    private int discountPercentage;
    private BigDecimal maxDiscount;
    private BigDecimal minimumOrderAmount;
    private LocalDateTime expiryDate;
    private boolean active;
}
