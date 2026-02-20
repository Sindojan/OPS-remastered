package com.sindoflow.ops.auth;

import com.sindoflow.ops.auth.dto.*;
import com.sindoflow.ops.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAll(Pageable pageable) {
        Page<UserResponse> users = userService.findAll(pageable).map(UserResponse::from);
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
        UserEntity user = userService.create(
                request.email(), request.password(), request.firstName(), request.lastName(), request.role()
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
}
