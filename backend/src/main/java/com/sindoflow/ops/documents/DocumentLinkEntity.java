package com.sindoflow.ops.documents;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_links")
@Getter
@Setter
@NoArgsConstructor
public class DocumentLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "linked_type", nullable = false, length = 50)
    private String linkedType;

    @Column(name = "linked_id", nullable = false)
    private UUID linkedId;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}
