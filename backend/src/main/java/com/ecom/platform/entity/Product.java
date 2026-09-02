package com.ecom.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private int stockQuantity = 0;

    @Column(name = "sales_count", nullable = false)
    @Builder.Default
    private int salesCount = 0;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "is_top_deal")
    @Builder.Default
    private boolean isTopDeal = false;

    @Column(name = "discount_percentage")
    @Builder.Default
    private int discountPercentage = 0;

    @Column(name = "offer_expiry_date")
    private LocalDateTime offerExpiryDate;


    public BigDecimal getFinalPrice() {
        if (isTopDeal && discountPercentage > 0 && (offerExpiryDate == null || offerExpiryDate.isAfter(LocalDateTime.now()))) {
            BigDecimal discountMultiplier = BigDecimal.valueOf(1).subtract(BigDecimal.valueOf(discountPercentage).divide(BigDecimal.valueOf(100)));
            return price.multiply(discountMultiplier);
        }
        return price;
    }
}
