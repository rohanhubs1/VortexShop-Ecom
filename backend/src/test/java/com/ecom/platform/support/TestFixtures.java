package com.ecom.platform.support;

import com.ecom.platform.entity.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Small builders for entities used across the unit tests, so each test only spells out
 * the fields it actually cares about.
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static Role role(RoleName name) {
        return Role.builder().id((long) name.ordinal() + 1).name(name).build();
    }

    public static User customer(Long id, String email) {
        return user(id, email, RoleName.ROLE_CUSTOMER);
    }

    public static User user(Long id, String email, RoleName... roles) {
        Set<Role> roleSet = new HashSet<>();
        for (RoleName roleName : roles) {
            roleSet.add(role(roleName));
        }
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded-password")
                .firstName("Test")
                .lastName("User")
                .phone("9999999999")
                .isVerified(true)
                .roles(roleSet)
                .build();
    }

    public static Category category(Long id, String name) {
        return Category.builder().id(id).name(name).slug(name.toLowerCase()).build();
    }

    public static Product product(Long id, String name, String price, int stock) {
        return Product.builder()
                .id(id)
                .category(category(1L, "Electronics"))
                .name(name)
                .slug(name.toLowerCase().replace(' ', '-'))
                .description(name + " description")
                .price(new BigDecimal(price))
                .stockQuantity(stock)
                .build();
    }

    public static Address address(Long id, User user) {
        return Address.builder()
                .id(id)
                .user(user)
                .title("Home")
                .streetAddress("1 Main Street")
                .city("Noida")
                .state("UP")
                .zipCode("201301")
                .country("India")
                .build();
    }

    public static Coupon coupon(String code, int percent, String maxDiscount, String minimumOrder) {
        return Coupon.builder()
                .id(1L)
                .code(code)
                .discountPercentage(percent)
                .maxDiscount(new BigDecimal(maxDiscount))
                .minimumOrderAmount(new BigDecimal(minimumOrder))
                .expiryDate(LocalDateTime.now().plusDays(7))
                .active(true)
                .build();
    }

    /** Puts the given user into the security context, the same way JwtAuthenticationFilter does. */
    public static void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
