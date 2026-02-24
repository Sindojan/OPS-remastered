package com.owlsburg.ops.knowledge;

import com.owlsburg.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "knowledge_articles")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeArticleEntity extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String excerpt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ArticleStatus status = ArticleStatus.DRAFT;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "knowledge_article_tags",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<KnowledgeTagEntity> tags = new HashSet<>();
}
