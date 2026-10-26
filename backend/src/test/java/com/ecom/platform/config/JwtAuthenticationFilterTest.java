package com.ecom.platform.config;

import com.ecom.platform.entity.User;
import com.ecom.platform.service.CustomUserDetailsService;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.ecom.platform.support.TestFixtures.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider tokenProvider;
    @Mock private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithHeader(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    @Test
    void validBearerTokenAuthenticatesRequest() throws Exception {
        User user = customer(1L, "rohan@example.com");
        when(tokenProvider.validateToken("good")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("good")).thenReturn("rohan@example.com");
        when(userDetailsService.loadUserByUsername("rohan@example.com")).thenReturn(user);

        filter.doFilter(requestWithHeader("Bearer good"), response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isSameAs(user);
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_CUSTOMER");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void missingHeaderLeavesRequestAnonymous() throws Exception {
        filter.doFilter(requestWithHeader(null), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void nonBearerSchemeIsIgnored() throws Exception {
        filter.doFilter(requestWithHeader("Basic dXNlcjpwYXNz"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void invalidTokenLeavesRequestAnonymous() throws Exception {
        when(tokenProvider.validateToken("expired")).thenReturn(false);

        filter.doFilter(requestWithHeader("Bearer expired"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void forgedTokenExceptionIsSwallowedAndRequestStaysAnonymous() throws Exception {
        when(tokenProvider.validateToken("forged")).thenThrow(new SignatureException("bad signature"));

        filter.doFilter(requestWithHeader("Bearer forged"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}
