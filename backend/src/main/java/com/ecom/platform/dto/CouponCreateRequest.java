package com.ecom.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponCreateRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @Min(value = 1, message = "Discount percentage must be at least 1")
    @Max(value = 100, message = "Discount percentage cannot exceed 100")
    private int discountPercentage;

    @NotNull(message = "Maximum discount amount is required")
    private BigDecimal maxDiscount;

    @NotNull(message = "Minimum order amount is required")
    private BigDecimal minimumOrderAmount;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiryDate;

    @Builder.Default
    private boolean active = true;
}
