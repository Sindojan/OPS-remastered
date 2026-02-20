package com.sindoflow.ops.events;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.events.dto.DomainEventResponse;
import com.sindoflow.ops.events.dto.PublishDomainEventRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class DomainEventController {

    private final DomainEventService eventService;

    public DomainEventController(DomainEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DomainEventResponse>>> list(
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "processed", required = false) Boolean processed) {

        List<DomainEventEntity> events;
        if (processed != null && !processed) {
            events = eventService.getUnprocessed();
        } else if (eventType != null) {
            events = eventService.findByEventType(eventType);
        } else {
            events = eventService.findAll();
        }

        List<DomainEventResponse> response = events.stream()
                .map(DomainEventResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DomainEventResponse>> getById(@PathVariable UUID id) {
        DomainEventEntity event = eventService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(DomainEventResponse.from(event)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DomainEventResponse>> publish(
            @Valid @RequestBody PublishDomainEventRequest request) {

        DomainEventEntity event = eventService.publish(
                request.eventType(),
                request.sourceType(),
                request.sourceId(),
                request.payload()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(DomainEventResponse.from(event), "Domain event published"));
    }

    @PatchMapping("/{id}/process")
    public ResponseEntity<ApiResponse<DomainEventResponse>> markProcessed(@PathVariable UUID id) {
        DomainEventEntity event = eventService.markProcessed(id);
        return ResponseEntity.ok(ApiResponse.ok(DomainEventResponse.from(event), "Domain event marked as processed"));
    }
}
