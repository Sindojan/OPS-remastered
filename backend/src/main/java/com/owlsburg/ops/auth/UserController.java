package com.owlsburg.ops.auth;

import com.owlsburg.ops.agentinfra.AgentInstanceEntity;
import com.owlsburg.ops.agentinfra.AgentTemplateEntity;
import com.owlsburg.ops.agentinfra.AgentTemplateRepository;
import com.owlsburg.ops.agentinfra.PrimaryAgentService;
import com.owlsburg.ops.agentinfra.dto.MeResponse;
import com.owlsburg.ops.agentinfra.dto.PrimaryAgentResponse;
import com.owlsburg.ops.agentinfra.dto.PrimaryAgentUpdateRequest;
import com.owlsburg.ops.auth.dto.*;
import com.owlsburg.ops.auth.notifications.NotificationSettingsResponse;
import com.owlsburg.ops.auth.notifications.NotificationSettingsService;
import com.owlsburg.ops.auth.notifications.NotificationSettingsUpdateRequest;
import com.owlsburg.ops.auth.notifications.UserNotificationSettingsEntity;
import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.common.ModuleService;
import com.owlsburg.ops.common.TenantContext;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PrimaryAgentService primaryAgentService;
    private final AgentTemplateRepository agentTemplateRepository;
    private final NotificationSettingsService notificationSettingsService;
    private final ModuleService moduleService;

    public UserController(UserService userService,
                          PrimaryAgentService primaryAgentService,
                          AgentTemplateRepository agentTemplateRepository,
                          NotificationSettingsService notificationSettingsService,
                          ModuleService moduleService) {
        this.userService = userService;
        this.primaryAgentService = primaryAgentService;
        this.agentTemplateRepository = agentTemplateRepository;
        this.notificationSettingsService = notificationSettingsService;
        this.moduleService = moduleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAll(Pageable pageable) {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        Page<UserResponse> users = userService.findAll(tenantId, pageable).map(UserResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable UUID id) {
        UserResponse user = UserResponse.from(userService.findById(id));
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserCreateRequest request) {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        UserEntity user = userService.create(
                request.email(), request.password(), request.firstName(), request.lastName(), request.role(), tenantId
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(UserResponse.from(user), "User created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        UserEntity user = userService.update(id, request.email(), request.firstName(), request.lastName());
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(@PathVariable UUID id, @Valid @RequestBody RoleUpdateRequest request) {
        UserEntity user = userService.updateRole(id, request.role());
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusUpdateRequest request) {
        UserEntity user = userService.updateStatus(id, request.active());
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "User deactivated"));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable UUID id, @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok(null, "Password reset successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserEntity user = userService.findById(userId);

        AgentInstanceEntity agentInstance = primaryAgentService.resolveForUser(user);
        PrimaryAgentResponse agentResponse = null;
        if (agentInstance != null) {
            AgentTemplateEntity template = agentTemplateRepository.findById(agentInstance.getTemplateId())
                    .orElse(null);
            agentResponse = PrimaryAgentResponse.from(agentInstance, template);
        }

        // Get enabled modules for this tenant
        java.util.List<String> enabledModules = user.getTenantId() != null
                ? new java.util.ArrayList<>(moduleService.getEnabledModules(user.getTenantId()))
                : java.util.List.of();

        MeResponse response = new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getTenantId(),
                agentResponse,
                enabledModules
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/primary-agent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updatePrimaryAgent(
            @PathVariable UUID id,
            @RequestBody PrimaryAgentUpdateRequest request) {
        UserEntity user = userService.findById(id);
        user.setPrimaryAgentInstanceId(request.agentInstanceId());
        UserEntity saved = userService.save(user);
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(saved)));
    }

    @GetMapping("/me/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getNotificationSettings(
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserNotificationSettingsEntity settings = notificationSettingsService.getForUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(NotificationSettingsResponse.from(settings)));
    }

    @PutMapping("/me/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateNotificationSettings(
            Authentication authentication,
            @RequestBody NotificationSettingsUpdateRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserNotificationSettingsEntity settings = notificationSettingsService.updateForUser(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(NotificationSettingsResponse.from(settings)));
    }
}
