package com.owlsburg.ops.auth;

import com.owlsburg.ops.auth.dto.LoginRequest;
import com.owlsburg.ops.auth.dto.LoginResponse;
import com.owlsburg.ops.auth.dto.RefreshRequest;
import com.owlsburg.ops.auth.dto.UserResponse;
import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.config.LoginRateLimiter;
import com.owlsburg.ops.tenant.TenantEntity;
import com.owlsburg.ops.tenant.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;
    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(UserService userService, JwtService jwtService, PasswordEncoder passwordEncoder,
                          TenantService tenantService, AuthService authService,
                          LoginRateLimiter loginRateLimiter) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.tenantService = tenantService;
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // Rate limiting
        String ip = httpRequest.getRemoteAddr();
        if (!loginRateLimiter.resolveBucket(ip).tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Too many login attempts. Try again later."));
        }

        // Find user by email
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

        // Check tenant status (skip for SYSTEM_ADMIN)
        TenantEntity tenant = tenantService.findById(user.getTenantId());
        if (user.getRole() != Role.SYSTEM_ADMIN) {
            if ("SUSPENDED".equals(tenant.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Your company account has been suspended. Contact support."));
            }
            if ("DELETED".equals(tenant.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Account not available"));
            }
            if (!tenant.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Tenant is deactivated"));
            }
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials"));
        }

        String tenantIdStr = user.getTenantId().toString();
        String accessToken = jwtService.generateAccessToken(user, tenantIdStr);
        String refreshToken = jwtService.generateRefreshToken(user, tenantIdStr);

        log.info("User logged in: {} (tenant: {})", user.getEmail(), tenantIdStr);
        return ResponseEntity.ok(ApiResponse.ok(new LoginResponse(accessToken, refreshToken, UserResponse.from(user))));
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

        String tokenHash = JwtService.hashToken(refreshToken);

        if (authService.isTokenBlacklisted(tokenHash)) {
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
        authService.blacklistToken(tokenHash, jwtService.getExpiration(refreshToken).toInstant());

        // Generate new tokens
        String tenantId = jwtService.getTenantId(refreshToken);
        String newAccessToken = jwtService.generateAccessToken(user, tenantId);
        String newRefreshToken = jwtService.generateRefreshToken(user, tenantId);

        return ResponseEntity.ok(ApiResponse.ok(new LoginResponse(newAccessToken, newRefreshToken, UserResponse.from(user))));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        String refreshToken = request.refreshToken();

        if (jwtService.isTokenValid(refreshToken)) {
            String tokenHash = JwtService.hashToken(refreshToken);
            authService.blacklistToken(tokenHash, jwtService.getExpiration(refreshToken).toInstant());
        }

        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }
}
