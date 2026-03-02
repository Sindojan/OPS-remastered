package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.customers.CustomerEntity;
import com.owlsburg.ops.customers.CustomerService;
import com.owlsburg.ops.production.JobService;
import com.owlsburg.ops.production.JobStatus;
import com.owlsburg.ops.production.dto.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class GetCustomerOrdersTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetCustomerOrdersTool.class);

    private static final Set<JobStatus> OPEN_STATUSES = Set.of(
            JobStatus.DRAFT, JobStatus.RELEASED, JobStatus.IN_PRODUCTION, JobStatus.ON_HOLD
    );

    private final JobService jobService;
    private final CustomerService customerService;
    private final ObjectMapper objectMapper;

    public GetCustomerOrdersTool(JobService jobService,
                                 CustomerService customerService,
                                 ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.customerService = customerService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_customer_orders";
    }

    @Override
    public String getDescription() {
        return "Offene Aufträge eines bestimmten Kunden oder aller Kunden abrufen.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "customerId":{"type":"string","description":"UUID des Kunden (ohne = alle Kunden)"}
            }}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);
            Pageable pageable = PageRequest.of(0, 50);

            List<Map<String, Object>> result = new ArrayList<>();

            if (inputNode.has("customerId") && !inputNode.get("customerId").isNull()) {
                UUID customerId = UUID.fromString(inputNode.get("customerId").asText());
                Page<JobResponse> jobs = jobService.findByCustomer(customerId, pageable);

                String customerName = getCustomerName(customerId);

                jobs.getContent().stream()
                        .filter(j -> OPEN_STATUSES.contains(j.status()))
                        .forEach(job -> result.add(buildJobEntry(job, customerName)));
            } else {
                // All open jobs grouped by customer
                for (JobStatus status : OPEN_STATUSES) {
                    Page<JobResponse> jobs = jobService.findByStatus(status, pageable);
                    for (JobResponse job : jobs.getContent()) {
                        String customerName = job.customerId() != null
                                ? getCustomerName(job.customerId())
                                : "Kein Kunde";
                        result.add(buildJobEntry(job, customerName));
                    }
                }
            }

            String json = objectMapper.writeValueAsString(Map.of(
                    "anzahl", result.size(),
                    "aufträge", result
            ));
            if (json.length() > 2000) {
                json = json.substring(0, 1950) + "...\n[Ergebnis gekürzt, nutze customerId für spezifische Abfrage]";
            }
            return ToolResult.success(json);
        } catch (Exception e) {
            log.error("Error executing get_customer_orders: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Kundenaufträge: " + e.getMessage());
        }
    }

    private Map<String, Object> buildJobEntry(JobResponse job, String customerName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kundenName", customerName);
        m.put("auftragsNummer", job.jobNumber());
        m.put("titel", job.title());
        m.put("status", job.status().name());
        m.put("deadline", job.deadline() != null ? job.deadline().toString() : null);
        m.put("überfällig", job.overdue());
        return m;
    }

    private String getCustomerName(UUID customerId) {
        try {
            CustomerEntity customer = customerService.findById(customerId);
            return customer.getCompanyName();
        } catch (Exception e) {
            return "Unbekannt";
        }
    }
}
