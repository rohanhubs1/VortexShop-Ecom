package com.ecom.platform.service;

import com.ecom.platform.dto.CreateSupportTicketRequest;
import com.ecom.platform.dto.ReplyMessageRequest;
import com.ecom.platform.dto.SupportMessageDto;
import com.ecom.platform.dto.SupportTicketDto;
import com.ecom.platform.entity.SupportMessage;
import com.ecom.platform.entity.SupportTicket;
import com.ecom.platform.entity.User;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.SupportMessageRepository;
import com.ecom.platform.repository.SupportTicketRepository;
import com.ecom.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;
    private final SupportMessageRepository supportMessageRepository;

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        throw new BadRequestException("Not authenticated");
    }

    @Transactional
    public SupportTicketDto createTicket(CreateSupportTicketRequest request) {
        User user = getAuthenticatedUser();
        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .subject(request.getSubject())
                .message(request.getMessage())
                .attachmentUrl(request.getAttachmentUrl())
                .status("OPEN")
                .build();
        return convertToDto(supportTicketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDto> getMyTickets() {
        User user = getAuthenticatedUser();
        return supportTicketRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDto> getAllTickets() {
        return supportTicketRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SupportTicketDto resolveTicket(Long id) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        ticket.setStatus("RESOLVED");
        ticket.setResolvedAt(LocalDateTime.now());
        return convertToDto(supportTicketRepository.save(ticket));
    }

    @Transactional
    public SupportTicketDto replyToTicket(Long id, ReplyMessageRequest request, String senderRole) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if ("CUSTOMER".equals(senderRole)) {
            User user = getAuthenticatedUser();
            if (!ticket.getUser().getId().equals(user.getId())) {
                throw new BadRequestException("Unauthorized access to this ticket");
            }
            if ("RESOLVED".equals(ticket.getStatus())) {
                ticket.setStatus("OPEN");
                ticket.setResolvedAt(null);
                supportTicketRepository.save(ticket);
            }
        }

        SupportMessage message = SupportMessage.builder()
                .ticket(ticket)
                .senderRole(senderRole)
                .message(request.getMessage())
                .attachmentUrl(request.getAttachmentUrl())
                .build();

        supportMessageRepository.save(message);
        return convertToDto(ticket);
    }

    public long getOpenTicketCount() {
        return supportTicketRepository.countByStatus("OPEN");
    }

    private SupportTicketDto convertToDto(SupportTicket ticket) {
        List<SupportMessageDto> messageDtos = supportMessageRepository.findByTicketOrderByCreatedAtAsc(ticket).stream()
                .map(msg -> SupportMessageDto.builder()
                        .id(msg.getId())
                        .ticketId(ticket.getId())
                        .senderRole(msg.getSenderRole())
                        .message(msg.getMessage())
                        .attachmentUrl(msg.getAttachmentUrl())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return SupportTicketDto.builder()
                .id(ticket.getId())
                .userId(ticket.getUser().getId())
                .customerName(ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName())
                .customerEmail(ticket.getUser().getEmail())
                .subject(ticket.getSubject())
                .message(ticket.getMessage())
                .attachmentUrl(ticket.getAttachmentUrl())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .messages(messageDtos)
                .build();
    }
}
