package com.ecom.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderRequest {

    @NotNull(message = "Shipping address ID is required")
    private Long shippingAddressId;

    private String couponCode;
}
