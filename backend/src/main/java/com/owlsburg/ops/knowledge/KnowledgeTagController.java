package com.owlsburg.ops.knowledge;

import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.knowledge.dto.KnowledgeTagResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge/tags")
public class KnowledgeTagController {

    private final KnowledgeTagService tagService;

    public KnowledgeTagController(KnowledgeTagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<KnowledgeTagResponse>>> list() {
        List<KnowledgeTagResponse> tags = tagService.findAll().stream()
                .map(KnowledgeTagResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(tags));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgeTagResponse>> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Name is required"));
        }
        KnowledgeTagEntity entity = tagService.findOrCreate(name.trim());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(KnowledgeTagResponse.from(entity), "Tag created"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        tagService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Tag deleted"));
    }
}
