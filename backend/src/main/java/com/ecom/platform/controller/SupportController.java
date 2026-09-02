package com.ecom.platform.controller;

import com.ecom.platform.dto.CreateSupportTicketRequest;
import com.ecom.platform.dto.ReplyMessageRequest;
import com.ecom.platform.dto.SupportTicketDto;
import com.ecom.platform.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support", description = "Customer Support API")
public class SupportController {

    private final SupportService supportService;

    @PostMapping
    @Operation(summary = "Submit a new support ticket")
    public ResponseEntity<SupportTicketDto> createTicket(@Valid @RequestBody CreateSupportTicketRequest request) {
        return ResponseEntity.ok(supportService.createTicket(request));
    }

    @GetMapping
    @Operation(summary = "Get my support tickets")
    public ResponseEntity<List<SupportTicketDto>> getMyTickets() {
        return ResponseEntity.ok(supportService.getMyTickets());
    }

    @PostMapping("/{id}/reply")
    @Operation(summary = "Customer reply to their support ticket")
    public ResponseEntity<SupportTicketDto> replyTicket(@PathVariable Long id, @Valid @RequestBody ReplyMessageRequest request) {
        return ResponseEntity.ok(supportService.replyToTicket(id, request, "CUSTOMER"));
    }
}
