package com.owlsburg.ops.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.auth.dto.LoginRequest;
import com.owlsburg.ops.common.GlobalExceptionHandler;
import com.owlsburg.ops.config.LoginRateLimiter;
import com.owlsburg.ops.tenant.TenantEntity;
import com.owlsburg.ops.tenant.TenantService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantService tenantService;

    @Mock
    private AuthService authService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @InjectMocks
    private AuthController authController;

    private UserEntity testUser;
    private TenantEntity testTenant;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        UUID tenantId = UUID.randomUUID();

        testUser = new UserEntity();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@owlsburg.de");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.ADMIN);
        testUser.setTenantId(tenantId);
        testUser.setActive(true);
        testUser.setPasswordHash("hashed");

        testTenant = new TenantEntity();
        testTenant.setId(tenantId);
        testTenant.setName("Test Tenant");
        testTenant.setActive(true);
        testTenant.setStatus("ACTIVE");

        // Default: rate limiter allows
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.simple(100, java.time.Duration.ofMinutes(1)))
                .build();
        when(loginRateLimiter.resolveBucket(any())).thenReturn(bucket);
    }

    @Test
    void login_withValidCredentials_returnsOk() throws Exception {
        when(userService.findByEmail("test@owlsburg.de")).thenReturn(testUser);
        when(tenantService.findById(testUser.getTenantId())).thenReturn(testTenant);
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        LoginRequest request = new LoginRequest("test@owlsburg.de", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        when(userService.findByEmail("test@owlsburg.de")).thenReturn(testUser);
        when(tenantService.findById(testUser.getTenantId())).thenReturn(testTenant);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginRequest request = new LoginRequest("test@owlsburg.de", "wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        when(userService.findByEmail("unknown@owlsburg.de"))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("User not found"));

        LoginRequest request = new LoginRequest("unknown@owlsburg.de", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_withDeactivatedAccount_returns403() throws Exception {
        testUser.setActive(false);
        when(userService.findByEmail("test@owlsburg.de")).thenReturn(testUser);

        LoginRequest request = new LoginRequest("test@owlsburg.de", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is deactivated"));
    }

    @Test
    void login_withSuspendedTenant_returns403() throws Exception {
        testTenant.setStatus("SUSPENDED");
        when(userService.findByEmail("test@owlsburg.de")).thenReturn(testUser);
        when(tenantService.findById(testUser.getTenantId())).thenReturn(testTenant);

        LoginRequest request = new LoginRequest("test@owlsburg.de", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
