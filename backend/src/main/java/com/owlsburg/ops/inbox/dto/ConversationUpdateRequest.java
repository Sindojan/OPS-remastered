package com.owlsburg.ops.inbox.dto;

import com.owlsburg.ops.inbox.ConversationPriority;

import java.util.UUID;

public record ConversationUpdateRequest(
        String subject,
        UUID customerId,
        ConversationPriority priority
) {}
