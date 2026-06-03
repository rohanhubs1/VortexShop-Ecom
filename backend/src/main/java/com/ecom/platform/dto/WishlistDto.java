package com.ecom.platform.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistDto {
    private Long id;
    private List<ProductDto> products;
}
