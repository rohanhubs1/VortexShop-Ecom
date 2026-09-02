package com.ecom.platform.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateSupportTicketRequest {
    @NotBlank(message = "Subject is required")
    private String subject;
    @NotBlank(message = "Message is required")
    private String message;
    private String attachmentUrl;
}
