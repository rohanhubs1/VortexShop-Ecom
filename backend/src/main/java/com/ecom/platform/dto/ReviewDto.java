package com.ecom.platform.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {
    private Long id;
    private Long userId;
    private String reviewerName;
    private Long productId;
    private int rating;
    private String comment;
    private String imageUrl;
    private LocalDateTime createdAt;
}
