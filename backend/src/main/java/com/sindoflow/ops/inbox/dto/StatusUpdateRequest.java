package com.sindoflow.ops.inbox.dto;

import com.sindoflow.ops.inbox.ConversationStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull ConversationStatus status
) {}
