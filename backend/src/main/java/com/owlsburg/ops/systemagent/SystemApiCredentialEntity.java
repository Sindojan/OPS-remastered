package com.owlsburg.ops.systemagent;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_api_credentials")
@Getter
@Setter
@NoArgsConstructor
public class SystemApiCredentialEntity extends SystemBaseEntity {

    @Column(name = "service_name", nullable = false, unique = true, length = 100)
    private String serviceName;

    @Column(name = "encrypted_value", nullable = false, columnDefinition = "TEXT")
    private String encryptedValue;

    @Column(length = 500)
    private String description;

    @Column(columnDefinition = "jsonb")
    private String metadata = "{}";
}
