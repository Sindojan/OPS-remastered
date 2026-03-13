package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class CheckServiceHealthTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(CheckServiceHealthTool.class);

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public CheckServiceHealthTool(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "check_service_health";
    }

    @Override
    public String getDescription() {
        return "Prüft den Gesundheitszustand aller Services: Datenbank, JVM, Threads.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("## Service Health Check\n\n");
            sb.append("| Komponente | Status | Details |\n");
            sb.append("|------------|--------|----------|\n");

            // Database check
            String dbStatus = checkDatabase();
            sb.append("| PostgreSQL | ").append(dbStatus).append(" |\n");

            // JVM Heap check
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            long heapUsedMb = heapUsage.getUsed() / (1024 * 1024);
            long heapMaxMb = heapUsage.getMax() / (1024 * 1024);
            double heapPercent = heapMaxMb > 0 ? (double) heapUsedMb / heapMaxMb * 100 : 0;

            String heapStatus;
            if (heapPercent < 70) {
                heapStatus = "OK | Heap: " + heapUsedMb + "/" + heapMaxMb + " MB (" + String.format("%.1f", heapPercent) + "%)";
            } else if (heapPercent < 90) {
                heapStatus = "WARNING | Heap: " + heapUsedMb + "/" + heapMaxMb + " MB (" + String.format("%.1f", heapPercent) + "%)";
            } else {
                heapStatus = "CRITICAL | Heap: " + heapUsedMb + "/" + heapMaxMb + " MB (" + String.format("%.1f", heapPercent) + "%)";
            }
            sb.append("| JVM Heap | ").append(heapStatus).append(" |\n");

            // Thread check
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            int threadCount = threadBean.getThreadCount();
            int peakThreadCount = threadBean.getPeakThreadCount();

            String threadStatus;
            if (threadCount < 200) {
                threadStatus = "OK | Aktiv: " + threadCount + ", Peak: " + peakThreadCount;
            } else if (threadCount < 500) {
                threadStatus = "WARNING | Aktiv: " + threadCount + ", Peak: " + peakThreadCount;
            } else {
                threadStatus = "CRITICAL | Aktiv: " + threadCount + ", Peak: " + peakThreadCount;
            }
            sb.append("| Threads | ").append(threadStatus).append(" |\n");

            // Overall status
            boolean hasWarning = heapPercent >= 70 || threadCount >= 200;
            boolean hasCritical = heapPercent >= 90 || threadCount >= 500;
            String overallStatus = hasCritical ? "CRITICAL" : hasWarning ? "WARNING" : "OK";
            sb.append("\n**Gesamtstatus:** ").append(overallStatus);

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error checking service health: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Health-Check: " + e.getMessage());
        }
    }

    private String checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            conn.setNetworkTimeout(Runnable::run, 5000);
            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(5);
                stmt.execute("SELECT 1");
                return "OK | Verbindung erfolgreich";
            }
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return "CRITICAL | " + e.getMessage();
        }
    }
}
