package com.owlsburg.ops.odoo;

import com.owlsburg.ops.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "odoo_config")
@Getter
@Setter
@NoArgsConstructor
public class OdooConfigEntity extends BaseEntity {

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "database_name", nullable = false, length = 200)
    private String databaseName;

    @Column(name = "api_key_enc", nullable = false)
    private String apiKeyEnc;

    @Column(name = "odoo_version", nullable = false, length = 20)
    private String odooVersion = "19.0";

    @Column(name = "connection_status", nullable = false, length = 30)
    private String connectionStatus = "UNCONFIGURED";

    @Column(name = "last_connected_at")
    private Instant lastConnectedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String settings = "{}";
}
