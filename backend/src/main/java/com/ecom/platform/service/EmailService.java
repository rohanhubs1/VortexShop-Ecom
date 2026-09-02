package com.ecom.platform.service;

import com.ecom.platform.entity.Order;
import com.ecom.platform.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String frontendUrl = "http://localhost:5173";

    public void sendVerificationEmail(User user, String token) {
        String link = frontendUrl + "/verify?token=" + token;
        sendHtmlEmail(user.getEmail(), "Verify your Email - Vortex Ecom", 
            "<h3>Welcome to Vortex Ecom!</h3>" +
            "<p>Please click the link below to verify your account:</p>" +
            "<a href='" + link + "'>Verify My Account</a>");
    }

    public void sendPasswordResetEmail(User user, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        sendHtmlEmail(user.getEmail(), "Reset your Password - Vortex Ecom", 
            "<h3>Password Reset Request</h3>" +
            "<p>Click the link below to reset your password. If you didn't request this, you can safely ignore this email.</p>" +
            "<a href='" + link + "'>Reset Password</a>");
    }

    public void sendOrderDeliveredEmail(Order order) {
        sendHtmlEmail(order.getUser().getEmail(), "Order Delivered - " + order.getOrderNumber(), 
            "<h3>Good news!</h3>" +
            "<p>Your order <strong>" + order.getOrderNumber() + "</strong> has been delivered.</p>" +
            "<p>Log in to your account to print your receipt or leave a review for your items.</p>");
    }

    public void sendOrderCancelledEmail(Order order) {
        sendHtmlEmail(order.getUser().getEmail(), "Order Cancelled - " + order.getOrderNumber(), 
            "<h3>Order Update</h3>" +
            "<p>Your order <strong>" + order.getOrderNumber() + "</strong> has been cancelled.</p>" +
            "<p>If you have any questions, please reach out to our support team.</p>");
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("no-reply@ecom.com");
            helper.setTo(to);
            helper.setSubject(subject);
            
            String wrapper = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>" + htmlBody + "<p style='margin-top: 25px;'>Best regards,<br/><strong>Vortex E-Commerce Team</strong></p></div>";
            helper.setText(wrapper, true);
            mailSender.send(message);
            log.info("Email successfully sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
        }
    }

    public void sendOrderConfirmation(Order order) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("no-reply@ecom.com");
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("Order Confirmation - " + order.getOrderNumber());

            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>");
            htmlContent.append("<h2 style='color: #4CAF50;'>Thank you for your order, ").append(order.getUser().getFirstName()).append("!</h2>");
            htmlContent.append("<p>We are excited to let you know that we have received your order <strong>").append(order.getOrderNumber()).append("</strong> and are preparing it for shipment.</p>");
            
            htmlContent.append("<h3>Order Summary:</h3>");
            htmlContent.append("<table style='width: 100%; border-collapse: collapse; margin-top: 10px;'>");
            htmlContent.append("<thead><tr style='background-color: #f2f2f2;'><th style='padding: 8px; text-align: left;'>Product</th><th style='padding: 8px; text-align: center;'>Qty</th><th style='padding: 8px; text-align: right;'>Price</th></tr></thead>");
            htmlContent.append("<tbody>");
            
            order.getItems().forEach(item -> {
                htmlContent.append("<tr>");
                htmlContent.append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(item.getProduct().getName()).append("</td>");
                htmlContent.append("<td style='padding: 8px; border-bottom: 1px solid #ddd; text-align: center;'>").append(item.getQuantity()).append("</td>");
                htmlContent.append("<td style='padding: 8px; border-bottom: 1px solid #ddd; text-align: right;'>$").append(item.getPrice()).append("</td>");
                htmlContent.append("</tr>");
            });
            
            htmlContent.append("</tbody></table>");
            
            htmlContent.append("<div style='margin-top: 15px; border-top: 2px solid #eee; padding-top: 10px; text-align: right;'>");
            htmlContent.append("<p>Subtotal: $").append(order.getTotalAmount()).append("</p>");
            if (order.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                htmlContent.append("<p style='color: red;'>Discount: -$").append(order.getDiscountAmount()).append("</p>");
            }
            htmlContent.append("<p style='font-size: 1.2em; font-weight: bold;'>Total Paid: $").append(order.getFinalAmount()).append("</p>");
            htmlContent.append("</div>");
            
            htmlContent.append("<p style='margin-top: 25px;'>Best regards,<br/><strong>Vortex E-Commerce Team</strong></p>");
            htmlContent.append("</div>");

            helper.setText(htmlContent.toString(), true);
            mailSender.send(message);
            log.info("Order confirmation email successfully sent for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order: {}", order.getOrderNumber(), e);
        }
    }
}
