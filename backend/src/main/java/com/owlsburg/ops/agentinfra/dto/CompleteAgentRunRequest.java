package com.owlsburg.ops.agentinfra.dto;

import java.math.BigDecimal;

public record CompleteAgentRunRequest(
        String output,
        int totalTokensUsed,
        BigDecimal costUsd
) {}
