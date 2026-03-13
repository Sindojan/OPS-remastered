package com.owlsburg.ops.systemagent;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_agent_templates")
@Getter
@Setter
@NoArgsConstructor
public class SystemAgentTemplateEntity extends SystemBaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100, unique = true)
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
    private int dailyTokenBudget = 200000;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private int version = 1;
}
