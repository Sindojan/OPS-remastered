package com.sindoflow.ops.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token_blacklist")
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenBlacklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}
