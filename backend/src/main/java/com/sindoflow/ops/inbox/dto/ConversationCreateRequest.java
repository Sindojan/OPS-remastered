package com.sindoflow.ops.inbox.dto;

import com.sindoflow.ops.inbox.ConversationPriority;
import com.sindoflow.ops.inbox.ConversationSource;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ConversationCreateRequest(
        @NotBlank String subject,
        UUID customerId,
        ConversationPriority priority,
        ConversationSource source
) {}
