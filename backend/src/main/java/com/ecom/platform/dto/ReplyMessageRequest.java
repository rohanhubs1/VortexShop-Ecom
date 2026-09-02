package com.ecom.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyMessageRequest {
    @NotBlank(message = "Reply message cannot be empty")
    private String message;
    private String attachmentUrl;
}
