package com.owlsburg.ops.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByEmailAndTenantId(String email, UUID tenantId);
    Page<UserEntity> findByTenantId(UUID tenantId, Pageable pageable);
    boolean existsByEmail(String email);
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
}
