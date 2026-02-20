package com.sindoflow.ops.auth;

import com.sindoflow.ops.auth.dto.LoginRequest;
import com.sindoflow.ops.auth.dto.LoginResponse;
import com.sindoflow.ops.auth.dto.RefreshRequest;
import com.sindoflow.ops.auth.dto.UserResponse;
import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.common.TenantContext;
import com.sindoflow.ops.tenant.TenantEntity;
import com.sindoflow.ops.tenant.TenantService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;
    private final RefreshTokenBlacklistRepository blacklistRepository;

    public AuthController(UserService userService, JwtService jwtService, PasswordEncoder passwordEncoder,
                          TenantService tenantService, RefreshTokenBlacklistRepository blacklistRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.tenantService = tenantService;
        this.blacklistRepository = blacklistRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        // Verify tenant exists and is active
        TenantEntity tenant = tenantService.findByTenantId(request.tenantId());
        if (!tenant.isActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Tenant is deactivated"));
        }

        // Set tenant context to query the correct schema
        TenantContext.setCurrentTenant(request.tenantId());
        try {
            UserEntity user;
            try {
                user = userService.findByEmail(request.email());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid credentials"));
            }

            if (!user.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Account is deactivated"));
            }

            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid credentials"));
            }

            String accessToken = jwtService.generateAccessToken(user, request.tenantId());
            String refreshToken = jwtService.generateRefreshToken(user, request.tenantId());

            LoginResponse response = new LoginResponse(accessToken, refreshToken, UserResponse.from(user));
            log.info("User logged in: {} (tenant: {})", user.getEmail(), request.tenantId());
            return ResponseEntity.ok(ApiResponse.ok(response));
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid refresh token"));
        }

        if (!"refresh".equals(jwtService.getTokenType(refreshToken))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not a refresh token"));
        }

        // Check blacklist
        String tokenHash = JwtService.hashToken(refreshToken);
        String tenantId = jwtService.getTenantId(refreshToken);

        TenantContext.setCurrentTenant(tenantId);
        try {
            if (blacklistRepository.existsByTokenHash(tokenHash)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Token has been revoked"));
            }

            java.util.UUID userId = jwtService.getUserId(refreshToken);
            UserEntity user = userService.findById(userId);

            if (!user.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Account is deactivated"));
            }

            // Blacklist old refresh token
            RefreshTokenBlacklistEntity blacklistEntry = new RefreshTokenBlacklistEntity();
            blacklistEntry.setTokenHash(tokenHash);
            blacklistEntry.setExpiredAt(jwtService.getExpiration(refreshToken).toInstant());
            blacklistRepository.save(blacklistEntry);

            // Generate new tokens
            String newAccessToken = jwtService.generateAccessToken(user, tenantId);
            String newRefreshToken = jwtService.generateRefreshToken(user, tenantId);

            LoginResponse response = new LoginResponse(newAccessToken, newRefreshToken, UserResponse.from(user));
            return ResponseEntity.ok(ApiResponse.ok(response));
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        String refreshToken = request.refreshToken();

        if (jwtService.isTokenValid(refreshToken)) {
            String tokenHash = JwtService.hashToken(refreshToken);
            String tenantId = jwtService.getTenantId(refreshToken);

            TenantContext.setCurrentTenant(tenantId);
            try {
                if (!blacklistRepository.existsByTokenHash(tokenHash)) {
                    RefreshTokenBlacklistEntity blacklistEntry = new RefreshTokenBlacklistEntity();
                    blacklistEntry.setTokenHash(tokenHash);
                    blacklistEntry.setExpiredAt(jwtService.getExpiration(refreshToken).toInstant());
                    blacklistRepository.save(blacklistEntry);
                }
            } finally {
                TenantContext.clear();
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }
}
