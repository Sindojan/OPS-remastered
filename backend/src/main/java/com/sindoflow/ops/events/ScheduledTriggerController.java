package com.sindoflow.ops.events;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.events.dto.CreateScheduledTriggerRequest;
import com.sindoflow.ops.events.dto.ScheduledTriggerResponse;
import com.sindoflow.ops.events.dto.UpdateScheduledTriggerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scheduled-triggers")
public class ScheduledTriggerController {

    private final ScheduledTriggerService triggerService;

    public ScheduledTriggerController(ScheduledTriggerService triggerService) {
        this.triggerService = triggerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduledTriggerResponse>>> list(
            @RequestParam(value = "instanceId", required = false) UUID instanceId,
            @RequestParam(value = "active", required = false) Boolean active) {

        List<ScheduledTriggerEntity> triggers;
        if (instanceId != null) {
            triggers = triggerService.findByInstanceId(instanceId);
        } else if (active != null && active) {
            triggers = triggerService.findAll().stream()
                    .filter(ScheduledTriggerEntity::isActive)
                    .toList();
        } else {
            triggers = triggerService.findAll();
        }

        List<ScheduledTriggerResponse> response = triggers.stream()
                .map(ScheduledTriggerResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduledTriggerResponse>> getById(@PathVariable UUID id) {
        ScheduledTriggerEntity trigger = triggerService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(ScheduledTriggerResponse.from(trigger)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledTriggerResponse>> create(
            @Valid @RequestBody CreateScheduledTriggerRequest request) {

        ScheduledTriggerEntity entity = new ScheduledTriggerEntity();
        entity.setInstanceId(request.instanceId());
        entity.setCronExpression(request.cronExpression());

        ScheduledTriggerEntity created = triggerService.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ScheduledTriggerResponse.from(created), "Scheduled trigger created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduledTriggerResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateScheduledTriggerRequest request) {

        ScheduledTriggerEntity entity = new ScheduledTriggerEntity();
        entity.setInstanceId(request.instanceId());
        entity.setCronExpression(request.cronExpression());

        ScheduledTriggerEntity updated = triggerService.update(id, entity);
        return ResponseEntity.ok(ApiResponse.ok(ScheduledTriggerResponse.from(updated), "Scheduled trigger updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        triggerService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Scheduled trigger deleted"));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ScheduledTriggerResponse>> activate(@PathVariable UUID id) {
        ScheduledTriggerEntity activated = triggerService.activate(id);
        return ResponseEntity.ok(ApiResponse.ok(ScheduledTriggerResponse.from(activated), "Scheduled trigger activated"));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ScheduledTriggerResponse>> deactivate(@PathVariable UUID id) {
        ScheduledTriggerEntity deactivated = triggerService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(ScheduledTriggerResponse.from(deactivated), "Scheduled trigger deactivated"));
    }
}
