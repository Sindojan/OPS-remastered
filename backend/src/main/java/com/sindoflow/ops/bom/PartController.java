package com.sindoflow.ops.bom;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.bom.dto.CreatePartRequest;
import com.sindoflow.ops.bom.dto.PartResponse;
import com.sindoflow.ops.bom.dto.UpdatePartRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PartResponse>> create(@Valid @RequestBody CreatePartRequest request) {
        PartEntity entity = partService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(PartResponse.from(entity)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PartResponse>> getById(@PathVariable UUID id) {
        PartEntity entity = partService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(PartResponse.from(entity)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PartResponse>>> findAll(Pageable pageable) {
        Page<PartResponse> page = partService.findAll(pageable).map(PartResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PartResponse>> update(@PathVariable UUID id,
                                                             @Valid @RequestBody UpdatePartRequest request) {
        PartEntity entity = partService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(PartResponse.from(entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        partService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Part deleted"));
    }
}
