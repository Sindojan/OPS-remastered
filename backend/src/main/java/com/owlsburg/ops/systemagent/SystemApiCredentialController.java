package com.owlsburg.ops.systemagent;

import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.systemagent.dto.ApiCredentialResponse;
import com.owlsburg.ops.systemagent.dto.ApiCredentialSaveRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/credentials")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemApiCredentialController {

    private final SystemApiCredentialService credentialService;

    public SystemApiCredentialController(SystemApiCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiCredentialResponse>>> listAll() {
        List<ApiCredentialResponse> credentials = credentialService.listAll().stream()
                .map(ApiCredentialResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(credentials));
    }

    @PutMapping("/{serviceName}")
    public ResponseEntity<ApiResponse<ApiCredentialResponse>> save(
            @PathVariable String serviceName,
            @Valid @RequestBody ApiCredentialSaveRequest request) {
        SystemApiCredentialEntity saved = credentialService.saveCredential(
                serviceName, request.value(), request.description(), request.metadata());
        return ResponseEntity.ok(ApiResponse.ok(ApiCredentialResponse.from(saved)));
    }

    @DeleteMapping("/{serviceName}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String serviceName) {
        credentialService.deleteCredential(serviceName);
        return ResponseEntity.ok(ApiResponse.ok(null, "Credential gelöscht"));
    }
}
