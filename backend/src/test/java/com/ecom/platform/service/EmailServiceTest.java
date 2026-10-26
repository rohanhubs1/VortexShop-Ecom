package com.ecom.platform.service;

import com.ecom.platform.entity.Order;
import com.ecom.platform.entity.OrderItem;
import com.ecom.platform.entity.User;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.Properties;

import static com.ecom.platform.support.TestFixtures.customer;
import static com.ecom.platform.support.TestFixtures.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage message;
    private User user;

    @BeforeEach
    void setUp() {
        message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        user = customer(1L, "rohan@example.com");
    }

    private Order order(String discount) {
        Order order = Order.builder().user(user).orderNumber("ORD-1234")
                .totalAmount(new BigDecimal("200.00")).discountAmount(new BigDecimal(discount))
                .finalAmount(new BigDecimal("200.00").subtract(new BigDecimal(discount))).build();
        order.getItems().add(OrderItem.builder().product(product(7L, "Phone", "100.00", 5)).quantity(2).price(new BigDecimal("100.00")).build());
        return order;
    }

    @Test
    void verificationEmailContainsLinkWithToken() throws Exception {
        emailService.sendVerificationEmail(user, "tok-123");

        verify(mailSender).send(message);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("rohan@example.com");
        assertThat(message.getSubject()).contains("Verify");
        message.saveChanges();
        assertThat(bodyOf(message)).contains("/verify?token=tok-123");
    }

    @Test
    void passwordResetEmailContainsResetLink() throws Exception {
        emailService.sendPasswordResetEmail(user, "reset-9");

        verify(mailSender).send(message);
        message.saveChanges();
        assertThat(bodyOf(message)).contains("/reset-password?token=reset-9");
    }

    @Test
    void orderStatusEmailsUseOrderNumberInSubject() throws Exception {
        emailService.sendOrderDeliveredEmail(order("0"));
        assertThat(message.getSubject()).isEqualTo("Order Delivered - ORD-1234");

        emailService.sendOrderCancelledEmail(order("0"));
        assertThat(message.getSubject()).isEqualTo("Order Cancelled - ORD-1234");
        verify(mailSender, times(2)).send(message);
    }

    @Test
    void orderConfirmationListsItemsAndDiscount() throws Exception {
        emailService.sendOrderConfirmation(order("20.00"));

        verify(mailSender).send(message);
        message.saveChanges();
        String body = bodyOf(message);
        assertThat(body).contains("ORD-1234", "Phone", "Discount: -$20.00", "Total Paid: $180.00");
    }

    @Test
    void orderConfirmationOmitsDiscountLineWhenNoDiscount() throws Exception {
        emailService.sendOrderConfirmation(order("0"));

        message.saveChanges();
        assertThat(bodyOf(message)).doesNotContain("Discount:");
    }

    @Test
    void mailServerFailureDoesNotBreakTheCaller() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertThatNoException().isThrownBy(() -> emailService.sendVerificationEmail(user, "tok"));
        assertThatNoException().isThrownBy(() -> emailService.sendOrderConfirmation(order("0")));
    }

    /** Returns the decoded HTML body, digging through the multipart structure MimeMessageHelper builds. */
    private static String bodyOf(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String html = bodyOf(multipart.getBodyPart(i));
                if (html != null) {
                    return html;
                }
            }
        }
        return null;
    }
}
