package com.ecom.platform.controller;

import com.ecom.platform.dto.CouponDto;
import com.ecom.platform.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.ecom.platform.entity.User;
import com.ecom.platform.entity.OrderStatus;
import com.ecom.platform.repository.UserRepository;
import com.ecom.platform.repository.OrderRepository;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import java.util.Map;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Public endpoints for coupons")
public class PublicCouponController {

    private final CouponService couponService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @GetMapping
    @Operation(summary = "Get all active coupons")
    public ResponseEntity<List<CouponDto>> getActiveCoupons() {
        List<CouponDto> allCoupons = couponService.getAllCoupons();
        List<CouponDto> activeCoupons = allCoupons.stream()
                .filter(CouponDto::isActive)
                .filter(c -> c.getExpiryDate().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(activeCoupons);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate a coupon for the current user")
    public ResponseEntity<CouponDto> validateCoupon(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("Coupon code is required");
        }
        
        List<CouponDto> allCoupons = couponService.getAllCoupons();
        CouponDto found = allCoupons.stream()
                .filter(c -> c.getCode().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid coupon code"));

        if (!found.isActive() || found.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This coupon has expired or is inactive");
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user != null) {
                boolean alreadyUsed = orderRepository.findByUser(user).stream()
                        .anyMatch(o -> o.getStatus() != OrderStatus.CANCELLED && 
                                       found.getCode().equals(o.getCouponCode()));
                if (alreadyUsed) {
                    throw new BadRequestException("Coupon already claimed");
                }
            }
        }

        return ResponseEntity.ok(found);
    }
}
