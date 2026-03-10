package com.owlsburg.ops.agentinfra.llm;

import com.owlsburg.ops.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenant_llm_config")
@Getter
@Setter
@NoArgsConstructor
public class TenantLlmConfigEntity extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "api_key_enc")
    private String apiKeyEnc;

    @Column(name = "default_model", length = 100)
    private String defaultModel;

    @Column(columnDefinition = "jsonb")
    private String settings = "{}";
}
