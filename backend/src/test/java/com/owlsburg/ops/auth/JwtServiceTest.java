package com.owlsburg.ops.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-for-unit-tests-min-32-chars!!",
                86400000L,   // 24h
                604800000L   // 7d
        );
    }

    @Test
    void generateAccessToken_containsExpectedClaims() {
        UserEntity user = createTestUser();
        String tenantId = UUID.randomUUID().toString();

        String token = jwtService.generateAccessToken(user, tenantId);

        assertNotNull(token);
        Claims claims = jwtService.parseToken(token);
        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals(user.getEmail(), claims.get("email", String.class));
        assertEquals(tenantId, claims.get("tenantId", String.class));
        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void generateRefreshToken_hasRefreshType() {
        UserEntity user = createTestUser();
        String tenantId = UUID.randomUUID().toString();

        String token = jwtService.generateRefreshToken(user, tenantId);

        Claims claims = jwtService.parseToken(token);
        assertEquals("refresh", claims.get("type", String.class));
        assertNull(claims.get("email", String.class)); // refresh tokens don't carry email
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        UserEntity user = createTestUser();
        String token = jwtService.generateAccessToken(user, UUID.randomUUID().toString());

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.here"));
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        // Create a service with 0ms expiration
        JwtService expiredService = new JwtService(
                "test-secret-key-for-unit-tests-min-32-chars!!",
                0L, 0L
        );
        UserEntity user = createTestUser();
        String token = expiredService.generateAccessToken(user, UUID.randomUUID().toString());

        assertFalse(expiredService.isTokenValid(token));
    }

    @Test
    void getUserId_returnsCorrectId() {
        UserEntity user = createTestUser();
        String token = jwtService.generateAccessToken(user, UUID.randomUUID().toString());

        assertEquals(user.getId(), jwtService.getUserId(token));
    }

    @Test
    void getExpiration_returnsFutureDate() {
        UserEntity user = createTestUser();
        String token = jwtService.generateAccessToken(user, UUID.randomUUID().toString());

        Date expiration = jwtService.getExpiration(token);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void hashToken_producesConsistentHash() {
        String token = "some-token-value";
        String hash1 = JwtService.hashToken(token);
        String hash2 = JwtService.hashToken(token);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 hex = 64 chars
    }

    @Test
    void hashToken_producesDifferentHashesForDifferentTokens() {
        assertNotEquals(
                JwtService.hashToken("token-a"),
                JwtService.hashToken("token-b")
        );
    }

    private UserEntity createTestUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("test@owlsburg.de");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.ADMIN);
        user.setTenantId(UUID.randomUUID());
        return user;
    }
}
