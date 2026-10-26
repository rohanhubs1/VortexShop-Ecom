package com.ecom.platform.service;

import com.ecom.platform.config.JwtTokenProvider;
import com.ecom.platform.dto.AuthRequest;
import com.ecom.platform.dto.AuthResponse;
import com.ecom.platform.dto.RegisterRequest;
import com.ecom.platform.dto.UserDto;
import com.ecom.platform.entity.*;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.ecom.platform.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private CartRepository cartRepository;
    @Mock private WishlistRepository wishlistRepository;
    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private RegisterRequest registerRequest() {
        return new RegisterRequest("new@example.com", "secret123", "New", "Customer", "9876543210");
    }

    @Test
    void loginReturnsJwtAndProfileForVerifiedUser() {
        User user = customer(1L, "rohan@example.com");
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("rohan@example.com")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new AuthRequest("rohan@example.com", "secret123"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("rohan@example.com");
        assertThat(response.getPhone()).isEqualTo("9999999999");
        assertThat(response.getRoles()).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void loginRefusesUnverifiedUserWithoutIssuingToken() {
        User user = customer(1L, "rohan@example.com");
        user.setVerified(false);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("rohan@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new AuthRequest("rohan@example.com", "secret123")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("verify your email");
        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    void loginPropagatesBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new AuthRequest("rohan@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void registerCreatesUnverifiedCustomerWithCartWishlistAndVerificationEmail() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(role(RoleName.ROLE_CUSTOMER)));
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        UserDto dto = authService.register(registerRequest());

        assertThat(dto.getId()).isEqualTo(42L);
        assertThat(dto.getRoles()).containsExactly("ROLE_CUSTOMER");
        assertThat(dto.isVerified()).isFalse();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("bcrypt-hash");

        verify(cartRepository).save(argThat(cart -> cart.getUser().getId().equals(42L)));
        verify(wishlistRepository).save(argThat(wishlist -> wishlist.getUser().getId().equals(42L)));

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(verificationTokenRepository).save(tokenCaptor.capture());
        VerificationToken token = tokenCaptor.getValue();
        assertThat(token.getExpiryDate()).isCloseTo(LocalDateTime.now().plusHours(24), within(1, java.time.temporal.ChronoUnit.MINUTES));
        verify(emailService).sendVerificationEmail(any(User.class), eq(token.getToken()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email address already in use.");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void registerFailsWhenCustomerRoleIsNotSeeded() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registerRequest())).isInstanceOf(BadRequestException.class);
    }

    @Test
    void verifyEmailMarksUserVerifiedAndConsumesToken() {
        User user = customer(1L, "rohan@example.com");
        user.setVerified(false);
        VerificationToken token = VerificationToken.builder().token("abc").user(user).expiryDate(LocalDateTime.now().plusHours(1)).build();
        when(verificationTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        authService.verifyEmail("abc");

        assertThat(user.isVerified()).isTrue();
        verify(userRepository).save(user);
        verify(verificationTokenRepository).delete(token);
    }

    @Test
    void verifyEmailRejectsExpiredToken() {
        VerificationToken token = VerificationToken.builder().token("abc").user(customer(1L, "a@b.com"))
                .expiryDate(LocalDateTime.now().minusMinutes(1)).build();
        when(verificationTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("abc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Verification token has expired");
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmailRejectsUnknownToken() {
        when(verificationTokenRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("nope")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void forgotPasswordReplacesOldTokenAndEmailsNewOne() {
        User user = customer(1L, "rohan@example.com");
        PasswordResetToken old = PasswordResetToken.builder().token("old").user(user).expiryDate(LocalDateTime.now()).build();
        when(userRepository.findByEmail("rohan@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.of(old));

        authService.forgotPassword("rohan@example.com");

        verify(passwordResetTokenRepository).delete(old);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isNotEqualTo("old");
        assertThat(captor.getValue().getExpiryDate()).isCloseTo(LocalDateTime.now().plusHours(1), within(1, java.time.temporal.ChronoUnit.MINUTES));
        verify(emailService).sendPasswordResetEmail(user, captor.getValue().getToken());
    }

    @Test
    void forgotPasswordRejectsUnknownEmail() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword("ghost@example.com")).isInstanceOf(BadRequestException.class);
        verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
    }

    @Test
    void resetPasswordStoresEncodedPasswordAndConsumesToken() {
        User user = customer(1L, "rohan@example.com");
        PasswordResetToken token = PasswordResetToken.builder().token("t").user(user).expiryDate(LocalDateTime.now().plusMinutes(30)).build();
        when(passwordResetTokenRepository.findByToken("t")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newSecret")).thenReturn("new-hash");

        authService.resetPassword("t", "newSecret");

        assertThat(user.getPassword()).isEqualTo("new-hash");
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        PasswordResetToken token = PasswordResetToken.builder().token("t").user(customer(1L, "a@b.com"))
                .expiryDate(LocalDateTime.now().minusMinutes(1)).build();
        when(passwordResetTokenRepository.findByToken("t")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword("t", "newSecret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reset token has expired");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void resetPasswordRejectsUnknownToken() {
        when(passwordResetTokenRepository.findByToken("t")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("t", "x")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void getCurrentUserReturnsAuthenticatedProfile() {
        User user = customer(1L, "rohan@example.com");
        authenticateAs(user);
        when(userRepository.findByEmail("rohan@example.com")).thenReturn(Optional.of(user));

        assertThat(authService.getCurrentUser().getEmail()).isEqualTo("rohan@example.com");
    }

    @Test
    void getCurrentUserRejectsMissingOrAnonymousAuthentication() {
        assertThatThrownBy(() -> authService.getCurrentUser()).isInstanceOf(BadRequestException.class);

        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThatThrownBy(() -> authService.getCurrentUser()).isInstanceOf(BadRequestException.class);
    }

    @Test
    void getCurrentUserFailsWhenUserWasDeleted() {
        authenticateAs(customer(1L, "gone@example.com"));
        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser()).isInstanceOf(ResourceNotFoundException.class);
    }
}
