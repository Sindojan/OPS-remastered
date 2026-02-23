package com.owlsburg.ops.config;

import com.owlsburg.ops.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Configuration
public class RlsTenantInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RlsTenantInterceptor.class);

    @Bean
    @Primary
    public DataSource tenantAwareDataSource(DataSourceProperties properties) {
        DataSource baseDataSource = properties.initializeDataSourceBuilder().build();
        return new TenantAwareDataSource(baseDataSource);
    }

    static class TenantAwareDataSource extends DelegatingDataSource {

        TenantAwareDataSource(DataSource targetDataSource) {
            super(targetDataSource);
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection connection = super.getConnection();
            setTenantOnConnection(connection);
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection connection = super.getConnection(username, password);
            setTenantOnConnection(connection);
            return connection;
        }

        private void setTenantOnConnection(Connection connection) throws SQLException {
            String tenantId = TenantContext.getCurrentTenant();
            if (tenantId != null) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT set_config('app.current_tenant', ?, false)")) {
                    ps.setString(1, tenantId);
                    ps.execute();
                }
                log.trace("Set RLS tenant context on connection: {}", tenantId);
            } else {
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT set_config('app.current_tenant', '', false)")) {
                    ps.execute();
                }
            }
        }
    }
}
