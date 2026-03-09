package com.owlsburg.ops.common;

import com.owlsburg.ops.auth.JwtAuthenticationFilter;
import com.owlsburg.ops.auth.JwtService;
import com.owlsburg.ops.auth.UserEntity;
import com.owlsburg.ops.auth.Role;
import com.owlsburg.ops.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for security constraints:
 * - JWT token validation
 * - Token generation contains correct claims
 * - Expired/invalid tokens are rejected
 */
@ExtendWith(MockitoExtension.class)
class SecurityIntegrationTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-for-unit-tests-min-32-chars!!",
                86400000L, 604800000L
        );
    }

    @Test
    void validToken_canBeVerified() {
        UserEntity user = createTestUser();
        String token = jwtService.generateAccessToken(user, user.getTenantId().toString());

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("access", jwtService.getTokenType(token));
        assertEquals(user.getId(), jwtService.getUserId(token));
        assertEquals(user.getTenantId().toString(), jwtService.getTenantId(token));
        assertEquals("ADMIN", jwtService.getRole(token));
    }

    @Test
    void invalidToken_isRejected() {
        assertFalse(jwtService.isTokenValid("invalid-token"));
        assertFalse(jwtService.isTokenValid(""));
        assertFalse(jwtService.isTokenValid("eyJ.invalid.token"));
    }

    @Test
    void expiredToken_isRejected() {
        JwtService expiredService = new JwtService(
                "test-secret-key-for-unit-tests-min-32-chars!!", 0L, 0L
        );
        UserEntity user = createTestUser();
        String token = expiredService.generateAccessToken(user, user.getTenantId().toString());

        assertFalse(expiredService.isTokenValid(token));
    }

    @Test
    void tokenFromDifferentSecret_isRejected() {
        JwtService otherService = new JwtService(
                "other-secret-key-for-unit-tests-min-32-ch!!", 86400000L, 604800000L
        );
        UserEntity user = createTestUser();
        String token = otherService.generateAccessToken(user, user.getTenantId().toString());

        // Token created with different secret should not validate
        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void refreshToken_cannotBeUsedAsAccessToken() {
        UserEntity user = createTestUser();
        String refreshToken = jwtService.generateRefreshToken(user, user.getTenantId().toString());

        // Token is valid (structurally) but is a refresh token, not access
        assertTrue(jwtService.isTokenValid(refreshToken));
        assertEquals("refresh", jwtService.getTokenType(refreshToken));
        assertNull(jwtService.getEmail(refreshToken)); // refresh tokens have no email claim
    }

    @Test
    void accessToken_containsAllRequiredClaims() {
        UserEntity user = createTestUser();
        String token = jwtService.generateAccessToken(user, user.getTenantId().toString());

        assertEquals(user.getEmail(), jwtService.getEmail(token));
        assertEquals(user.getRole().name(), jwtService.getRole(token));
        assertEquals(user.getTenantId().toString(), jwtService.getTenantId(token));
        assertNotNull(jwtService.getExpiration(token));
    }

    private UserEntity createTestUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("security-test@owlsburg.de");
        user.setFirstName("Security");
        user.setLastName("Test");
        user.setRole(Role.ADMIN);
        user.setTenantId(UUID.randomUUID());
        user.setActive(true);
        return user;
    }
}
