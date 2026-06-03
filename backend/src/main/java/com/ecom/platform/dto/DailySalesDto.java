package com.ecom.platform.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySalesDto {
    private String date;
    private BigDecimal sales;
}
