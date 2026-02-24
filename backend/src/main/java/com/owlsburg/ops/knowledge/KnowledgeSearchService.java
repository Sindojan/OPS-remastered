package com.owlsburg.ops.knowledge;

import com.owlsburg.ops.documents.DocumentEntity;
import com.owlsburg.ops.documents.DocumentRepository;
import com.owlsburg.ops.knowledge.dto.KnowledgeSearchResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    private final KnowledgeArticleRepository articleRepository;
    private final KnowledgeCategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;

    public KnowledgeSearchService(KnowledgeArticleRepository articleRepository,
                                  KnowledgeCategoryRepository categoryRepository,
                                  DocumentRepository documentRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeSearchResultResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String searchTerm = "%" + query.toLowerCase() + "%";
        log.info("Knowledge search for: {}", query);

        List<KnowledgeSearchResultResponse> results = new ArrayList<>();

        // Search articles
        List<KnowledgeArticleEntity> articles = articleRepository.searchByTitle(query,
                org.springframework.data.domain.Pageable.ofSize(50)).getContent();

        for (KnowledgeArticleEntity article : articles) {
            String categoryName = null;
            if (article.getCategoryId() != null) {
                categoryName = categoryRepository.findById(article.getCategoryId())
                        .map(KnowledgeCategoryEntity::getName).orElse(null);
            }
            results.add(new KnowledgeSearchResultResponse(
                    "article",
                    article.getId(),
                    article.getTitle(),
                    article.getExcerpt(),
                    categoryName,
                    article.getUpdatedAt()
            ));
        }

        // Search documents
        List<DocumentEntity> documents = documentRepository.searchByTitleOrDescription(searchTerm);
        for (DocumentEntity doc : documents) {
            results.add(new KnowledgeSearchResultResponse(
                    "document",
                    doc.getId(),
                    doc.getTitle(),
                    doc.getExcerpt() != null ? doc.getExcerpt() : doc.getDescription(),
                    doc.getCategory(),
                    doc.getUpdatedAt()
            ));
        }

        // Sort: title matches first (exact match), then by updatedAt desc
        String lowerQuery = query.toLowerCase();
        results.sort(Comparator
                .<KnowledgeSearchResultResponse, Boolean>comparing(
                        r -> !r.title().toLowerCase().startsWith(lowerQuery))
                .thenComparing(r -> r.updatedAt() != null ? r.updatedAt() : java.time.Instant.EPOCH,
                        Comparator.reverseOrder()));

        return results;
    }
}
