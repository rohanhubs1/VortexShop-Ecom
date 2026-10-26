package com.ecom.platform.service;

import com.ecom.platform.dto.CouponCreateRequest;
import com.ecom.platform.dto.CouponDto;
import com.ecom.platform.entity.Coupon;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.ecom.platform.support.TestFixtures.coupon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private CouponCreateRequest request(String code) {
        return CouponCreateRequest.builder()
                .code(code)
                .discountPercentage(15)
                .maxDiscount(new BigDecimal("100.00"))
                .minimumOrderAmount(new BigDecimal("500.00"))
                .expiryDate(LocalDateTime.now().plusDays(30))
                .active(true)
                .build();
    }

    @Test
    void listsAllCoupons() {
        when(couponRepository.findAll()).thenReturn(List.of(coupon("A", 5, "10", "0"), coupon("B", 10, "20", "0")));

        assertThat(couponService.getAllCoupons()).extracting(CouponDto::getCode).containsExactly("A", "B");
    }

    @Test
    void getCouponById() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon("A", 5, "10", "0")));
        when(couponRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(couponService.getCouponById(1L).getDiscountPercentage()).isEqualTo(5);
        assertThatThrownBy(() -> couponService.getCouponById(2L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createStoresCodeInUpperCase() {
        when(couponRepository.existsByCode("diwali15")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        CouponDto dto = couponService.createCoupon(request("diwali15"));

        assertThat(dto.getCode()).isEqualTo("DIWALI15");
        assertThat(dto.getMaxDiscount()).isEqualByComparingTo("100.00");
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    void createRejectsDuplicateCode() {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        assertThatThrownBy(() -> couponService.createCoupon(request("SAVE10")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Coupon code already exists.");
        verify(couponRepository, never()).save(any());
    }

    @Test
    void updateAllowsKeepingSameCodeInDifferentCase() {
        Coupon existing = coupon("SAVE10", 10, "50", "0");
        when(couponRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(couponRepository.save(existing)).thenReturn(existing);

        CouponDto dto = couponService.updateCoupon(1L, request("save10"));

        assertThat(dto.getCode()).isEqualTo("SAVE10");
        assertThat(dto.getDiscountPercentage()).isEqualTo(15);
        verify(couponRepository, never()).existsByCode(any());
    }

    @Test
    void updateRejectsRenamingToAnExistingCode() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon("SAVE10", 10, "50", "0")));
        when(couponRepository.existsByCode("SAVE20")).thenReturn(true);

        assertThatThrownBy(() -> couponService.updateCoupon(1L, request("SAVE20")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateRejectsMissingCoupon() {
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.updateCoupon(1L, request("X"))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesCoupon() {
        Coupon existing = coupon("SAVE10", 10, "50", "0");
        when(couponRepository.findById(1L)).thenReturn(Optional.of(existing));

        couponService.deleteCoupon(1L);

        verify(couponRepository).delete(existing);
    }

    @Test
    void deleteRejectsMissingCoupon() {
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.deleteCoupon(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void validateAcceptsActiveCouponCaseInsensitively() {
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon("SAVE10", 10, "50", "0")));

        assertThat(couponService.validateCoupon("save10").getCode()).isEqualTo("SAVE10");
    }

    @Test
    void validateRejectsInactiveCoupon() {
        Coupon inactive = coupon("OFF", 10, "50", "0");
        inactive.setActive(false);
        when(couponRepository.findByCode("OFF")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> couponService.validateCoupon("OFF"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This coupon is inactive.");
    }

    @Test
    void validateRejectsExpiredCoupon() {
        Coupon expired = coupon("OLD", 10, "50", "0");
        expired.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        when(couponRepository.findByCode("OLD")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> couponService.validateCoupon("OLD"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This coupon has expired.");
    }

    @Test
    void validateRejectsUnknownCoupon() {
        when(couponRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.validateCoupon("nope")).isInstanceOf(ResourceNotFoundException.class);
    }
}
