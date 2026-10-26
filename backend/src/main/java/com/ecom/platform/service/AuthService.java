package com.ecom.platform.service;

import com.ecom.platform.config.JwtTokenProvider;
import com.ecom.platform.dto.AuthRequest;
import com.ecom.platform.dto.AuthResponse;
import com.ecom.platform.dto.RegisterRequest;
import com.ecom.platform.dto.UserDto;
import com.ecom.platform.entity.*;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new BadRequestException("User not found"));
        if (!user.isVerified()) {
            throw new BadRequestException("Please verify your email address before logging in. Check your inbox for the verification link.");
        }
        String jwt = tokenProvider.generateToken(authentication);
        UserDto userDto = convertToDto(user);
        return AuthResponse.builder()
                .token(jwt)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(java.util.stream.Collectors.toList()))
                .build();
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address already in use.");
        }

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new BadRequestException("User Role not initialized."));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .roles(java.util.Collections.singleton(customerRole))
                .isVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        // Initialize empty cart
        Cart cart = Cart.builder().user(savedUser).build();
        cartRepository.save(cart);

        // Initialize empty wishlist
        Wishlist wishlist = Wishlist.builder().user(savedUser).build();
        wishlistRepository.save(wishlist);
        
        // Generate and save verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        verificationTokenRepository.save(verificationToken);
        
        // Send verification email
        emailService.sendVerificationEmail(savedUser, token);

        return convertToDto(savedUser);
    }
    
    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));
                
        if (verificationToken.isExpired()) {
            throw new BadRequestException("Verification token has expired");
        }
        
        User user = verificationToken.getUser();
        user.setVerified(true);
        userRepository.save(user);
        
        verificationTokenRepository.delete(verificationToken);
    }
    
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found with this email"));
                
        // Delete any existing reset tokens for this user
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);
        
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();
        passwordResetTokenRepository.save(resetToken);
        
        emailService.sendPasswordResetEmail(user, token);
    }
    
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));
                
        if (resetToken.isExpired()) {
            throw new BadRequestException("Reset token has expired");
        }
        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        passwordResetTokenRepository.delete(resetToken);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        org.springframework.security.core.context.SecurityContext context = org.springframework.security.core.context.SecurityContextHolder.getContext();
        if (context == null || context.getAuthentication() == null) {
            throw new BadRequestException("Not authenticated");
        }
        Object principal = context.getAuthentication().getPrincipal();
        if (!(principal instanceof org.springframework.security.core.userdetails.UserDetails)) {
            throw new BadRequestException("Not authenticated");
        }
        String email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new com.ecom.platform.exception.ResourceNotFoundException("User not found"));
        return convertToDto(user);
    }

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(java.util.stream.Collectors.toList()))
                .isVerified(user.isVerified())
                .build();
    }
}



