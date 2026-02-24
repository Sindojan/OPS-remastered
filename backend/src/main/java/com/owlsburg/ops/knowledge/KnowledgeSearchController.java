package com.owlsburg.ops.knowledge;

import com.owlsburg.ops.common.ApiResponse;
import com.owlsburg.ops.knowledge.dto.KnowledgeSearchResultResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/search")
public class KnowledgeSearchController {

    private final KnowledgeSearchService searchService;

    public KnowledgeSearchController(KnowledgeSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<KnowledgeSearchResultResponse>>> search(
            @RequestParam("q") String query) {
        List<KnowledgeSearchResultResponse> results = searchService.search(query);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
