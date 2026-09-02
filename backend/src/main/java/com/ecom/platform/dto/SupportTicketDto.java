package com.ecom.platform.dto;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicketDto {
    private Long id;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private String subject;
    private String message;
    private String attachmentUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private List<SupportMessageDto> messages;
}
