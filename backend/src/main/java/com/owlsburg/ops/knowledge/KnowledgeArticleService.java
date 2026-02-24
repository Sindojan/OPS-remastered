package com.owlsburg.ops.knowledge;

import com.owlsburg.ops.knowledge.dto.KnowledgeArticleCreateRequest;
import com.owlsburg.ops.knowledge.dto.KnowledgeArticleResponse;
import com.owlsburg.ops.knowledge.dto.KnowledgeArticleSummaryResponse;
import com.owlsburg.ops.knowledge.dto.KnowledgeArticleUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class KnowledgeArticleService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeArticleService.class);

    private final KnowledgeArticleRepository articleRepository;
    private final KnowledgeCategoryRepository categoryRepository;
    private final KnowledgeTagService tagService;

    public KnowledgeArticleService(KnowledgeArticleRepository articleRepository,
                                   KnowledgeCategoryRepository categoryRepository,
                                   KnowledgeTagService tagService) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.tagService = tagService;
    }

    @Transactional
    public KnowledgeArticleResponse createAsResponse(KnowledgeArticleCreateRequest request, UUID authorId) {
        KnowledgeArticleEntity article = new KnowledgeArticleEntity();
        article.setTitle(request.title());
        article.setSlug(generateSlug(request.title()));
        article.setContent(request.content());
        article.setExcerpt(request.excerpt());
        article.setStatus(ArticleStatus.DRAFT);
        article.setCategoryId(request.categoryId());
        article.setAuthorId(authorId);

        Set<KnowledgeTagEntity> tags = tagService.findByIds(request.tagIds());
        article.setTags(tags);

        log.info("Creating knowledge article: {}", request.title());
        KnowledgeArticleEntity saved = articleRepository.save(article);
        KnowledgeCategoryEntity category = findCategoryOrNull(saved.getCategoryId());
        return KnowledgeArticleResponse.from(saved, category);
    }

    @Transactional
    public KnowledgeArticleResponse updateAsResponse(UUID id, KnowledgeArticleUpdateRequest request) {
        KnowledgeArticleEntity article = findById(id);

        if (request.title() != null) {
            article.setTitle(request.title());
        }
        if (request.content() != null) {
            article.setContent(request.content());
        }
        if (request.excerpt() != null) {
            article.setExcerpt(request.excerpt());
        }
        article.setCategoryId(request.categoryId());

        if (request.tagIds() != null) {
            Set<KnowledgeTagEntity> tags = tagService.findByIds(request.tagIds());
            article.setTags(tags);
        }

        log.info("Updating knowledge article: {}", id);
        KnowledgeArticleEntity saved = articleRepository.save(article);
        KnowledgeCategoryEntity category = findCategoryOrNull(saved.getCategoryId());
        return KnowledgeArticleResponse.from(saved, category);
    }

    @Transactional
    public KnowledgeArticleResponse publishAsResponse(UUID id) {
        KnowledgeArticleEntity article = findById(id);
        article.setStatus(ArticleStatus.PUBLISHED);
        article.setPublishedAt(Instant.now());
        log.info("Publishing knowledge article: {}", id);
        KnowledgeArticleEntity saved = articleRepository.save(article);
        KnowledgeCategoryEntity category = findCategoryOrNull(saved.getCategoryId());
        return KnowledgeArticleResponse.from(saved, category);
    }

    @Transactional
    public KnowledgeArticleResponse archiveAsResponse(UUID id) {
        KnowledgeArticleEntity article = findById(id);
        article.setStatus(ArticleStatus.ARCHIVED);
        log.info("Archiving knowledge article: {}", id);
        KnowledgeArticleEntity saved = articleRepository.save(article);
        KnowledgeCategoryEntity category = findCategoryOrNull(saved.getCategoryId());
        return KnowledgeArticleResponse.from(saved, category);
    }

    @Transactional
    public void delete(UUID id) {
        KnowledgeArticleEntity article = findById(id);
        if (article.getStatus() != ArticleStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT articles can be deleted. Current status: " + article.getStatus());
        }
        articleRepository.delete(article);
        log.info("Deleted knowledge article: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeArticleSummaryResponse> findAllSummaries(Pageable pageable, ArticleStatus status, UUID categoryId, String search) {
        Page<KnowledgeArticleEntity> page = findAll(pageable, status, categoryId, search);
        return page.map(article -> {
            // Access tags within transaction to avoid LazyInitializationException
            KnowledgeCategoryEntity category = findCategoryOrNull(article.getCategoryId());
            return KnowledgeArticleSummaryResponse.from(article, category);
        });
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeArticleEntity> findAll(Pageable pageable, ArticleStatus status, UUID categoryId, String search) {
        if (search != null && !search.isBlank()) {
            if (status != null && categoryId != null) {
                return articleRepository.searchByTitleAndStatusAndCategoryId(search, status, categoryId, pageable);
            }
            if (status != null) {
                return articleRepository.searchByTitleAndStatus(search, status, pageable);
            }
            if (categoryId != null) {
                return articleRepository.searchByTitleAndCategoryId(search, categoryId, pageable);
            }
            return articleRepository.searchByTitle(search, pageable);
        }

        if (status != null && categoryId != null) {
            return articleRepository.findByStatusAndCategoryId(status, categoryId, pageable);
        }
        if (status != null) {
            return articleRepository.findByStatus(status, pageable);
        }
        if (categoryId != null) {
            return articleRepository.findByCategoryId(categoryId, pageable);
        }
        return articleRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public KnowledgeArticleEntity findById(UUID id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Knowledge article not found: " + id));
    }

    @Transactional(readOnly = true)
    public KnowledgeArticleResponse findByIdAsResponse(UUID id) {
        KnowledgeArticleEntity article = findById(id);
        KnowledgeCategoryEntity category = findCategoryOrNull(article.getCategoryId());
        return KnowledgeArticleResponse.from(article, category);
    }

    public KnowledgeCategoryEntity findCategoryOrNull(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId).orElse(null);
    }

    private String generateSlug(String title) {
        String slug = title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        // Ensure uniqueness
        if (articleRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }
        return slug;
    }
}
