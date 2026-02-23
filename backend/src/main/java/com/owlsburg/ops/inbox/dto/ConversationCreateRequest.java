package com.owlsburg.ops.inbox.dto;

import com.owlsburg.ops.inbox.ConversationPriority;
import com.owlsburg.ops.inbox.ConversationSource;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ConversationCreateRequest(
        @NotBlank String subject,
        UUID customerId,
        ConversationPriority priority,
        ConversationSource source
) {}
