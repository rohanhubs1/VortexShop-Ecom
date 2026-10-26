package com.ecom.platform.controller;

import com.ecom.platform.config.JwtTokenProvider;
import com.ecom.platform.config.SecurityConfig;
import com.ecom.platform.dto.AuthResponse;
import com.ecom.platform.dto.UserDto;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.service.AuthService;
import com.ecom.platform.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AuthService authService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private static final String LOGIN_JSON = "{\"email\":\"rohan@example.com\",\"password\":\"secret123\"}";

    @Test
    void loginIsPublicAndReturnsToken() throws Exception {
        when(authService.login(any())).thenReturn(AuthResponse.builder()
                .token("jwt-token").email("rohan@example.com").roles(List.of("ROLE_CUSTOMER")).build());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CUSTOMER"));
    }

    @Test
    void wrongPasswordReturns401WithGenericMessage() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginValidatesEmailFormat() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").value("Please provide a valid email address"))
                .andExpect(jsonPath("$.details.password").value("Password is required"));
        verifyNoInteractions(authService);
    }

    @Test
    void registerValidatesPasswordLength() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"123\",\"firstName\":\"New\",\"lastName\":\"User\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").value("Password must be at least 6 characters long"));
    }

    @Test
    void registerReturnsCreatedUser() throws Exception {
        when(authService.register(any())).thenReturn(UserDto.builder().id(42L).email("new@example.com").build());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"secret123\",\"firstName\":\"New\",\"lastName\":\"User\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void duplicateEmailReturns400() throws Exception {
        when(authService.register(any())).thenThrow(new BadRequestException("Email address already in use."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"secret123\",\"firstName\":\"New\",\"lastName\":\"User\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email address already in use."));
    }

    @Test
    void verifyAndPasswordResetFlows() throws Exception {
        mockMvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"rohan@example.com\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"t\",\"newPassword\":\"newSecret\"}"))
                .andExpect(status().isOk());

        verify(authService).verifyEmail("abc");
        verify(authService).forgotPassword("rohan@example.com");
        verify(authService).resetPassword("t", "newSecret");
    }
}
