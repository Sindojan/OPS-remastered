package com.sindoflow.ops.agentinfra.llm;

import com.sindoflow.ops.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/settings/llm")
public class LlmConfigController {

    private final LlmConfigService configService;

    public LlmConfigController(LlmConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<LlmConfigResponse>> getConfig() {
        Optional<TenantLlmConfigEntity> configOpt = configService.getConfig();

        LlmConfigResponse response;
        if (configOpt.isPresent()) {
            TenantLlmConfigEntity config = configOpt.get();
            response = new LlmConfigResponse(
                    config.getProvider(),
                    config.getDefaultModel(),
                    config.getApiKeyEnc() != null && !config.getApiKeyEnc().isBlank()
            );
        } else {
            response = new LlmConfigResponse("anthropic", "claude-sonnet-4-20250514", false);
        }

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<LlmConfigResponse>> saveConfig(
            @Valid @RequestBody SaveLlmConfigRequest request) {

        TenantLlmConfigEntity saved = configService.saveConfig(
                request.provider(), request.apiKey(), request.defaultModel()
        );

        LlmConfigResponse response = new LlmConfigResponse(
                saved.getProvider(),
                saved.getDefaultModel(),
                true
        );

        return ResponseEntity.ok(ApiResponse.ok(response, "LLM configuration saved"));
    }

    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<String>>> listModels() {
        List<String> models = configService.listModels();
        return ResponseEntity.ok(ApiResponse.ok(models));
    }

    @PostMapping("/models")
    public ResponseEntity<ApiResponse<List<String>>> listModelsWithKey(
            @RequestBody SaveLlmConfigRequest request) {
        try {
            List<String> models = configService.listModelsWithKey(
                    request.provider(), request.apiKey()
            );
            return ResponseEntity.ok(ApiResponse.ok(models));
        } catch (LlmProviderException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid API key or provider error: " + e.getMessage()));
        }
    }
}
