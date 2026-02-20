package com.sindoflow.ops.agentinfra;

import com.sindoflow.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agent_templates")
@Getter
@Setter
@NoArgsConstructor
public class AgentTemplateEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_prompt", columnDefinition = "TEXT")
    private String basePrompt;

    @Column(name = "allowed_tools", nullable = false, columnDefinition = "jsonb")
    private String allowedTools = "[]";

    @Column(name = "trigger_types", nullable = false, columnDefinition = "jsonb")
    private String triggerTypes = "[]";

    @Column(name = "max_tokens_per_run", nullable = false)
    private int maxTokensPerRun = 4096;

    @Column(name = "daily_token_budget", nullable = false)
    private int dailyTokenBudget = 100000;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private int version = 1;
}
