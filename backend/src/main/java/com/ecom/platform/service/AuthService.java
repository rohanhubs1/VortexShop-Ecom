package com.ecom.platform.service;

import com.ecom.platform.config.JwtTokenProvider;
import com.ecom.platform.dto.AuthRequest;
import com.ecom.platform.dto.AuthResponse;
import com.ecom.platform.dto.RegisterRequest;
import com.ecom.platform.dto.UserDto;
import com.ecom.platform.entity.*;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.repository.CartRepository;
import com.ecom.platform.repository.RoleRepository;
import com.ecom.platform.repository.UserRepository;
import com.ecom.platform.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse authenticate(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("User login failed"));

        List<String> roles = userDetails.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .toList();

        return AuthResponse.builder()
                .token(jwt)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roles)
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
                .active(true)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build();

        User savedUser = userRepository.save(user);

        // Pre-create Cart and Wishlist for new customer
        Cart cart = Cart.builder().user(savedUser).build();
        cartRepository.save(cart);

        Wishlist wishlist = Wishlist.builder().user(savedUser).build();
        wishlistRepository.save(wishlist);

        return convertToDto(savedUser);
    }

    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new BadRequestException("No authenticated user session.");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("User session expired"));
        return convertToDto(user);
    }

    public UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(role -> role.getName().name()).toList())
                .build();
    }
}
