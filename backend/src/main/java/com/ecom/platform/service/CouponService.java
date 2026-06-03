package com.ecom.platform.service;

import com.ecom.platform.dto.CouponCreateRequest;
import com.ecom.platform.dto.CouponDto;
import com.ecom.platform.entity.Coupon;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public List<CouponDto> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CouponDto getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        return convertToDto(coupon);
    }

    @Transactional
    public CouponDto createCoupon(CouponCreateRequest request) {
        if (couponRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Coupon code already exists.");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .discountPercentage(request.getDiscountPercentage())
                .maxDiscount(request.getMaxDiscount())
                .minimumOrderAmount(request.getMinimumOrderAmount())
                .expiryDate(request.getExpiryDate())
                .active(request.isActive())
                .build();

        Coupon saved = couponRepository.save(coupon);
        return convertToDto(saved);
    }

    @Transactional
    public CouponDto updateCoupon(Long id, CouponCreateRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        if (!coupon.getCode().equalsIgnoreCase(request.getCode()) && couponRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Coupon code already exists.");
        }

        coupon.setCode(request.getCode().toUpperCase());
        coupon.setDiscountPercentage(request.getDiscountPercentage());
        coupon.setMaxDiscount(request.getMaxDiscount());
        coupon.setMinimumOrderAmount(request.getMinimumOrderAmount());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setActive(request.isActive());

        Coupon updated = couponRepository.save(coupon);
        return convertToDto(updated);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        couponRepository.delete(coupon);
    }

    @Transactional(readOnly = true)
    public CouponDto validateCoupon(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found."));

        if (!coupon.isActive()) {
            throw new BadRequestException("This coupon is inactive.");
        }

        if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This coupon has expired.");
        }

        return convertToDto(coupon);
    }

    private CouponDto convertToDto(Coupon coupon) {
        return CouponDto.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountPercentage(coupon.getDiscountPercentage())
                .maxDiscount(coupon.getMaxDiscount())
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .expiryDate(coupon.getExpiryDate())
                .active(coupon.isActive())
                .build();
    }
}
