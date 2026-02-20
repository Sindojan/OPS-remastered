package com.sindoflow.ops.people.dto;

import java.util.List;
import java.util.UUID;

public record MyDayResponse(
        UUID employeeId,
        boolean clockedIn,
        List<TimeEntryResponse> entries
) {}
