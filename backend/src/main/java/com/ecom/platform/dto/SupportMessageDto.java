package com.ecom.platform.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportMessageDto {
    private Long id;
    private Long ticketId;
    private String senderRole; // "CUSTOMER" or "ADMIN"
    private String message;
    private String attachmentUrl;
    private LocalDateTime createdAt;
}
