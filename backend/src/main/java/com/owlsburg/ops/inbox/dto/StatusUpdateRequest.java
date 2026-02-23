package com.owlsburg.ops.inbox.dto;

import com.owlsburg.ops.inbox.ConversationStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull ConversationStatus status
) {}
