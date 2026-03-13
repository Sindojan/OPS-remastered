package com.owlsburg.ops.config;

import com.owlsburg.ops.auth.Role;
import com.owlsburg.ops.auth.UserEntity;
import com.owlsburg.ops.auth.UserRepository;
import com.owlsburg.ops.systemagent.SystemLlmConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Initializes System-Admin credentials and System-LLM API-Key from environment variables.
 * Runs after Flyway migrations on every application start.
 */
@Component
public class SystemAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(SystemAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemLlmConfigService systemLlmConfigService;

    @Value("${system-admin.email:#{null}}")
    private String systemAdminEmail;

    @Value("${system-admin.password:#{null}}")
    private String systemAdminPassword;

    @Value("${system-llm.api-key:#{null}}")
    private String systemLlmApiKey;

    public SystemAdminInitializer(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   SystemLlmConfigService systemLlmConfigService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemLlmConfigService = systemLlmConfigService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(5)
    public void initialize() {
        try {
            initializeSystemAdmin();
        } catch (Exception e) {
            log.error("Failed to initialize System-Admin credentials: {}", e.getMessage(), e);
        }
        try {
            initializeSystemLlmConfig();
        } catch (Exception e) {
            log.error("Failed to initialize System-LLM config: {}", e.getMessage(), e);
        }
    }

    private void initializeSystemAdmin() {
        if (systemAdminPassword == null || systemAdminPassword.isBlank()) {
            log.debug("SYSTEM_ADMIN_PASSWORD not set – using default credentials from migration");
            return;
        }

        String email = (systemAdminEmail != null && !systemAdminEmail.isBlank())
                ? systemAdminEmail
                : "philipp.ebert@strate-software.com";

        // Find existing SYSTEM_ADMIN: try target email first, then default migration email
        Optional<UserEntity> existing = userRepository.findByEmail(email);
        if (existing.isEmpty()) {
            existing = userRepository.findByEmail("philipp.ebert@strate-software.com");
        }

        if (existing.isEmpty()) {
            log.warn("No SYSTEM_ADMIN user found – skipping credential update");
            return;
        }

        UserEntity admin = existing.get();
        if (admin.getRole() != Role.SYSTEM_ADMIN) {
            log.warn("User with default SYSTEM_ADMIN email has role {} – skipping", admin.getRole());
            return;
        }

        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(systemAdminPassword));
        userRepository.save(admin);
        log.info("System-Admin credentials updated from environment (email={})", email);
    }

    private void initializeSystemLlmConfig() {
        if (systemLlmApiKey == null || systemLlmApiKey.isBlank()) {
            log.debug("SYSTEM_LLM_API_KEY not set – skipping LLM config initialization");
            return;
        }

        if (systemLlmConfigService.getConfig().isPresent()) {
            log.debug("System LLM config already exists – not overwriting with env value");
            return;
        }

        systemLlmConfigService.saveConfig("anthropic", systemLlmApiKey, "claude-sonnet-4-6", null);
        log.info("System LLM config initialized from environment (provider=anthropic, model=claude-sonnet-4-6)");
    }
}
