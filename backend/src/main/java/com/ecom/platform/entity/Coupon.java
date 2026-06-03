package com.ecom.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_percentage", nullable = false)
    private int discountPercentage;

    @Column(name = "max_discount", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "minimum_order_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public boolean isValid() {
        return active && expiryDate.isAfter(LocalDateTime.now());
    }
}
