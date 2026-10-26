package com.ecom.platform.config;

import com.ecom.platform.entity.RoleName;
import com.ecom.platform.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.HexFormat;
import java.util.List;

import static com.ecom.platform.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises the real JJWT signing and parsing, no mocks. */
class JwtTokenProviderTest {

    // 256-bit HMAC keys, hex encoded like the JWT_SECRET environment variable
    private static final String SECRET = "8f1c2b7e4d9a3f6c0b5e8d2a7c4f1e9b3d6a0c8f5e2b9d4a7c1f6e3b0d8a5c2f";
    private static final String OTHER_SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000);

    private Authentication authenticationFor(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Test
    void generatedTokenIsValidAndCarriesEmailAndRoles() {
        User admin = user(1L, "admin@example.com", RoleName.ROLE_ADMIN);

        String token = provider.generateToken(authenticationFor(admin));

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("admin@example.com");

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(HexFormat.of().parseHex(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertThat(claims.get("roles", List.class)).containsExactly("ROLE_ADMIN");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(60_000);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenProvider alreadyExpired = new JwtTokenProvider(SECRET, -1_000);
        String token = alreadyExpired.generateToken(authenticationFor(user(1L, "a@example.com", RoleName.ROLE_CUSTOMER)));

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void malformedOrEmptyTokensAreRejected() {
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
    }

    @Test
    void unsignedTokenIsRejected() {
        String unsigned = Jwts.builder().subject("admin@example.com").compact();

        assertThat(provider.validateToken(unsigned)).isFalse();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = new JwtTokenProvider(OTHER_SECRET, 60_000)
                .generateToken(authenticationFor(user(1L, "attacker@example.com", RoleName.ROLE_ADMIN)));

        assertThat(provider.validateToken(forged)).isFalse();
        assertThatThrownBy(() -> provider.getUsernameFromToken(forged)).isInstanceOf(SignatureException.class);
    }

    @Test
    void tamperedPayloadIsRejected() {
        String token = provider.generateToken(authenticationFor(user(1L, "customer@example.com", RoleName.ROLE_CUSTOMER)));
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin@example.com\",\"roles\":[\"ROLE_ADMIN\"]}".getBytes());

        assertThat(provider.validateToken(parts[0] + "." + forgedPayload + "." + parts[2])).isFalse();
    }

    @Test
    void rejectsSecretThatIsTooShortForHmacSha256() {
        String shortSecret = HexFormat.of().formatHex(Decoders.BASE64.decode("c2hvcnQ="));

        assertThatThrownBy(() -> new JwtTokenProvider(shortSecret, 1000))
                .isInstanceOf(io.jsonwebtoken.security.WeakKeyException.class);
    }
}
