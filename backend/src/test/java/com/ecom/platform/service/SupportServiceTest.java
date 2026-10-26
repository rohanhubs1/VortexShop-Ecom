package com.ecom.platform.service;

import com.ecom.platform.dto.CreateSupportTicketRequest;
import com.ecom.platform.dto.ReplyMessageRequest;
import com.ecom.platform.dto.SupportTicketDto;
import com.ecom.platform.entity.SupportMessage;
import com.ecom.platform.entity.SupportTicket;
import com.ecom.platform.entity.User;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.SupportMessageRepository;
import com.ecom.platform.repository.SupportTicketRepository;
import com.ecom.platform.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.ecom.platform.support.TestFixtures.authenticateAs;
import static com.ecom.platform.support.TestFixtures.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportServiceTest {

    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private UserRepository userRepository;
    @Mock private SupportMessageRepository supportMessageRepository;

    @InjectMocks
    private SupportService supportService;

    private User customer;

    @BeforeEach
    void setUp() {
        customer = customer(1L, "rohan@example.com");
        authenticateAs(customer);
        lenient().when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        lenient().when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private SupportTicket ticket(User owner, String status) {
        return SupportTicket.builder().id(70L).user(owner).subject("Late delivery").message("Where is my order?")
                .status(status).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void createTicketStartsOpenForCurrentUser() {
        SupportTicketDto dto = supportService.createTicket(new CreateSupportTicketRequest("Late delivery", "Where is it?", null));

        assertThat(dto.getStatus()).isEqualTo("OPEN");
        assertThat(dto.getCustomerEmail()).isEqualTo("rohan@example.com");
        assertThat(dto.getMessages()).isEmpty();
    }

    @Test
    void listsOwnAndAllTicketsWithMessageThread() {
        SupportTicket ticket = ticket(customer, "OPEN");
        when(supportTicketRepository.findByUserOrderByCreatedAtDesc(customer)).thenReturn(List.of(ticket));
        when(supportTicketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));
        when(supportMessageRepository.findByTicketOrderByCreatedAtAsc(ticket)).thenReturn(List.of(
                SupportMessage.builder().id(1L).ticket(ticket).senderRole("ADMIN").message("Looking into it").build()));

        assertThat(supportService.getMyTickets()).singleElement()
                .satisfies(t -> assertThat(t.getMessages()).extracting(m -> m.getMessage()).containsExactly("Looking into it"));
        assertThat(supportService.getAllTickets()).hasSize(1);
    }

    @Test
    void resolveTicketStampsResolutionTime() {
        SupportTicket ticket = ticket(customer, "OPEN");
        when(supportTicketRepository.findById(70L)).thenReturn(Optional.of(ticket));

        SupportTicketDto dto = supportService.resolveTicket(70L);

        assertThat(dto.getStatus()).isEqualTo("RESOLVED");
        assertThat(dto.getResolvedAt()).isNotNull();
    }

    @Test
    void customerReplyReopensResolvedTicket() {
        SupportTicket ticket = ticket(customer, "RESOLVED");
        ticket.setResolvedAt(LocalDateTime.now());
        when(supportTicketRepository.findById(70L)).thenReturn(Optional.of(ticket));

        supportService.replyToTicket(70L, new ReplyMessageRequest("Still not here", null), "CUSTOMER");

        assertThat(ticket.getStatus()).isEqualTo("OPEN");
        assertThat(ticket.getResolvedAt()).isNull();
        ArgumentCaptor<SupportMessage> captor = ArgumentCaptor.forClass(SupportMessage.class);
        verify(supportMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getSenderRole()).isEqualTo("CUSTOMER");
    }

    @Test
    void customerCannotReplyToSomeoneElsesTicket() {
        when(supportTicketRepository.findById(70L)).thenReturn(Optional.of(ticket(customer(2L, "other@example.com"), "OPEN")));

        assertThatThrownBy(() -> supportService.replyToTicket(70L, new ReplyMessageRequest("hi", null), "CUSTOMER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unauthorized access to this ticket");
        verify(supportMessageRepository, never()).save(any());
    }

    @Test
    void adminReplyLeavesStatusUnchanged() {
        SupportTicket ticket = ticket(customer(2L, "other@example.com"), "RESOLVED");
        when(supportTicketRepository.findById(70L)).thenReturn(Optional.of(ticket));

        supportService.replyToTicket(70L, new ReplyMessageRequest("Refund issued", null), "ADMIN");

        assertThat(ticket.getStatus()).isEqualTo("RESOLVED");
        verify(supportMessageRepository).save(any(SupportMessage.class));
    }

    @Test
    void missingTicketAndOpenCount() {
        when(supportTicketRepository.findById(70L)).thenReturn(Optional.empty());
        when(supportTicketRepository.countByStatus("OPEN")).thenReturn(3L);

        assertThatThrownBy(() -> supportService.resolveTicket(70L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(supportService.getOpenTicketCount()).isEqualTo(3L);
    }
}
