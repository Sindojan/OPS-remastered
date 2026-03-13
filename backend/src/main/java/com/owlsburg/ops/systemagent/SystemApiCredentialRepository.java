package com.owlsburg.ops.systemagent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemApiCredentialRepository extends JpaRepository<SystemApiCredentialEntity, UUID> {

    Optional<SystemApiCredentialEntity> findByServiceName(String serviceName);

    void deleteByServiceName(String serviceName);
}
