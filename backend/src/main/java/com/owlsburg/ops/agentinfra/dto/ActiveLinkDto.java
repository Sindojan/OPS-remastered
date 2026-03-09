package com.owlsburg.ops.agentinfra.dto;

import java.time.Instant;
import java.util.UUID;

public record ActiveLinkDto(
        UUID senderInstanceId,
        UUID targetInstanceId,
        Instant lastMessageAt,
        String messageType
) {}
