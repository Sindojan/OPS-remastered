package com.sindoflow.ops.inbox.dto;

import com.sindoflow.ops.inbox.ConversationPriority;

import java.util.UUID;

public record ConversationUpdateRequest(
        String subject,
        UUID customerId,
        ConversationPriority priority
) {}
