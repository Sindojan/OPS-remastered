package com.owlsburg.ops.systemagent;

import com.owlsburg.ops.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/system/llm-config")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemLlmConfigController {

    private final SystemLlmConfigService configService;

    public SystemLlmConfigController(SystemLlmConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SystemLlmConfigResponse>> getConfig() {
        Optional<SystemLlmConfigEntity> config = configService.getConfig();
        SystemLlmConfigResponse response = config.map(c -> new SystemLlmConfigResponse(
                c.getProvider(),
                c.getDefaultModel(),
                c.getApiKeyEnc() != null && !c.getApiKeyEnc().isBlank(),
                c.getSettings()
        )).orElse(new SystemLlmConfigResponse("anthropic", null, false, null));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SystemLlmConfigResponse>> saveConfig(
            @Valid @RequestBody SystemLlmConfigSaveRequest request) {
        SystemLlmConfigEntity saved = configService.saveConfig(
                request.provider() != null ? request.provider() : "anthropic",
                request.apiKey(),
                request.defaultModel(),
                request.settings());
        SystemLlmConfigResponse response = new SystemLlmConfigResponse(
                saved.getProvider(),
                saved.getDefaultModel(),
                saved.getApiKeyEnc() != null && !saved.getApiKeyEnc().isBlank(),
                saved.getSettings()
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    public record SystemLlmConfigResponse(String provider, String defaultModel,
                                            boolean hasApiKey, String settings) {}

    public record SystemLlmConfigSaveRequest(
            String provider,
            @Size(max = 200) String apiKey,
            String defaultModel,
            String settings) {}
}
