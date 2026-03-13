package com.owlsburg.ops.systemagent.tools.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.systemagent.tools.SystemAgentTool;
import com.owlsburg.ops.systemagent.tools.SystemToolExecutionContext;
import com.owlsburg.ops.systemagent.tools.SystemToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;

@Component
public class GetSystemMetricsTool implements SystemAgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetSystemMetricsTool.class);

    private final ObjectMapper objectMapper;

    public GetSystemMetricsTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_system_metrics";
    }

    @Override
    public String getDescription() {
        return "Gibt aktuelle System-Metriken zurück: JVM Heap, Threads, CPU-Last, Uptime.";
    }

    @Override
    public String getInputSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public SystemToolResult execute(SystemToolExecutionContext context, String input) {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            long heapUsedMb = heapUsage.getUsed() / (1024 * 1024);
            long heapMaxMb = heapUsage.getMax() / (1024 * 1024);
            double heapPercent = heapMaxMb > 0 ? (double) heapUsedMb / heapMaxMb * 100 : 0;

            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            long nonHeapUsedMb = nonHeapUsage.getUsed() / (1024 * 1024);

            int threadCount = threadBean.getThreadCount();
            int peakThreadCount = threadBean.getPeakThreadCount();
            int daemonThreadCount = threadBean.getDaemonThreadCount();

            double systemLoadAverage = osBean.getSystemLoadAverage();
            int availableProcessors = osBean.getAvailableProcessors();

            Duration uptime = Duration.ofMillis(runtimeBean.getUptime());
            long hours = uptime.toHours();
            long minutes = uptime.toMinutesPart();
            long seconds = uptime.toSecondsPart();

            StringBuilder sb = new StringBuilder();
            sb.append("## System-Metriken\n\n");

            sb.append("### JVM Memory\n");
            sb.append("| Metrik | Wert |\n");
            sb.append("|--------|------|\n");
            sb.append("| Heap Used | ").append(heapUsedMb).append(" MB |\n");
            sb.append("| Heap Max | ").append(heapMaxMb).append(" MB |\n");
            sb.append("| Heap Auslastung | ").append(String.format("%.1f", heapPercent)).append("% |\n");
            sb.append("| Non-Heap Used | ").append(nonHeapUsedMb).append(" MB |\n\n");

            sb.append("### Threads\n");
            sb.append("| Metrik | Wert |\n");
            sb.append("|--------|------|\n");
            sb.append("| Aktive Threads | ").append(threadCount).append(" |\n");
            sb.append("| Peak Threads | ").append(peakThreadCount).append(" |\n");
            sb.append("| Daemon Threads | ").append(daemonThreadCount).append(" |\n\n");

            sb.append("### System\n");
            sb.append("| Metrik | Wert |\n");
            sb.append("|--------|------|\n");
            sb.append("| CPU Load Average | ").append(systemLoadAverage >= 0 ? String.format("%.2f", systemLoadAverage) : "N/A").append(" |\n");
            sb.append("| Verfügbare CPUs | ").append(availableProcessors).append(" |\n");
            sb.append("| JVM Uptime | ").append(hours).append("h ").append(minutes).append("m ").append(seconds).append("s |\n");

            return SystemToolResult.success(sb.toString());
        } catch (Exception e) {
            log.error("Error getting system metrics: {}", e.getMessage());
            return SystemToolResult.error("Fehler beim Abrufen der System-Metriken: " + e.getMessage());
        }
    }
}
