package com.owlsburg.ops.systemagent;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_llm_config")
@Getter
@Setter
@NoArgsConstructor
public class SystemLlmConfigEntity extends SystemBaseEntity {

    @Column(nullable = false, length = 50)
    private String provider = "ANTHROPIC";

    @Column(name = "api_key_enc", nullable = false, columnDefinition = "TEXT")
    private String apiKeyEnc;

    @Column(name = "default_model", nullable = false, length = 100)
    private String defaultModel = "claude-sonnet-4-6";

    @Column(columnDefinition = "jsonb")
    private String settings = "{}";
}
