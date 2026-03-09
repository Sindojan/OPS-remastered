package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.people.AbsenceEntity;
import com.owlsburg.ops.people.AbsenceService;
import com.owlsburg.ops.people.AbsenceStatus;
import com.owlsburg.ops.people.EmployeeEntity;
import com.owlsburg.ops.people.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetAbsencesTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetAbsencesTool.class);

    private final AbsenceService absenceService;
    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;

    public GetAbsencesTool(AbsenceService absenceService,
                           EmployeeService employeeService,
                           ObjectMapper objectMapper) {
        this.absenceService = absenceService;
        this.employeeService = employeeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_absences";
    }

    @Override
    public String getDescription() {
        return "Aktuelle und geplante Abwesenheiten abrufen, optional gefiltert nach Status und Zeitraum.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "status":{"type":"string","enum":["APPROVED","PENDING","REJECTED"],"description":"Filter nach Genehmigungsstatus"},
              "dateFrom":{"type":"string","description":"Startdatum (ISO, z.B. 2026-03-01)"},
              "dateTo":{"type":"string","description":"Enddatum (ISO, z.B. 2026-03-31)"}
            }}""";
    }

    @Override
    public ToolPermission getPermission() {
        return ToolPermission.READ_ONLY;
    }

    @Override
    public String getModuleId() {
        return "people";
    }

    @Override
    public ToolResult execute(ToolExecutionContext context, String input) {
        try {
            JsonNode inputNode = objectMapper.readTree(input);

            LocalDate from = inputNode.has("dateFrom")
                    ? LocalDate.parse(inputNode.get("dateFrom").asText())
                    : LocalDate.now();
            LocalDate to = inputNode.has("dateTo")
                    ? LocalDate.parse(inputNode.get("dateTo").asText())
                    : from.plusMonths(1);

            List<AbsenceEntity> absences = absenceService.findByDateRange(from, to);

            // Filter by status if provided
            if (inputNode.has("status") && !inputNode.get("status").isNull()) {
                AbsenceStatus statusFilter = AbsenceStatus.valueOf(inputNode.get("status").asText());
                absences = absences.stream()
                        .filter(a -> a.getStatus() == statusFilter)
                        .toList();
            }

            List<Map<String, Object>> result = absences.stream()
                    .limit(30)
                    .map(a -> {
                        Map<String, Object> m = new LinkedHashMap<>();

                        // Get employee name
                        try {
                            EmployeeEntity emp = employeeService.getById(a.getEmployeeId());
                            m.put("mitarbeiter", emp.getFirstName() + " " + emp.getLastName());
                        } catch (Exception e) {
                            m.put("mitarbeiter", "Unbekannt");
                        }

                        String typLabel = switch (a.getType()) {
                            case VACATION -> "Urlaub";
                            case SICK -> "Krank";
                            case OTHER -> "Sonstig";
                        };
                        m.put("typ", typLabel);
                        m.put("von", a.getFromDate().toString());
                        m.put("bis", a.getToDate().toString());
                        m.put("status", a.getStatus().name());
                        return m;
                    })
                    .toList();

            String json = objectMapper.writeValueAsString(Map.of(
                    "zeitraum", from + " bis " + to,
                    "anzahl", result.size(),
                    "abwesenheiten", result
            ));
            if (json.length() > 2000) {
                json = json.substring(0, 1950) + "...\n[Ergebnis gekürzt]";
            }
            return ToolResult.success(json);
        } catch (Exception e) {
            log.error("Error executing get_absences: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Abwesenheiten: " + e.getMessage());
        }
    }
}
