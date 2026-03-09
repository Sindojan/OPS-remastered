package com.owlsburg.ops.agentinfra.tools.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlsburg.ops.agentinfra.tools.*;
import com.owlsburg.ops.people.EmployeeEntity;
import com.owlsburg.ops.people.EmployeeService;
import com.owlsburg.ops.people.TimeTrackingService;
import com.owlsburg.ops.people.dto.MyDayResponse;
import com.owlsburg.ops.people.dto.TimeEntryResponse;
import com.owlsburg.ops.people.TimeEntryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetAttendanceTodayTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetAttendanceTodayTool.class);

    private final EmployeeService employeeService;
    private final TimeTrackingService timeTrackingService;
    private final ObjectMapper objectMapper;

    public GetAttendanceTodayTool(EmployeeService employeeService,
                                  TimeTrackingService timeTrackingService,
                                  ObjectMapper objectMapper) {
        this.employeeService = employeeService;
        this.timeTrackingService = timeTrackingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_attendance_today";
    }

    @Override
    public String getDescription() {
        return "Heutige Anwesenheit abfragen – zeigt eingestempelte und abwesende Mitarbeiter.";
    }

    @Override
    public String getInputSchema() {
        return """
            {"type":"object","properties":{
              "stationId":{"type":"string","description":"Optional: Nur Mitarbeiter einer bestimmten Station"}
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
            UUID stationFilter = inputNode.has("stationId") && !inputNode.get("stationId").isNull()
                    ? UUID.fromString(inputNode.get("stationId").asText()) : null;

            List<EmployeeEntity> employees;
            if (stationFilter != null) {
                employees = employeeService.getByStation(stationFilter);
            } else {
                employees = employeeService.findAll(Pageable.unpaged()).getContent();
            }

            int anwesend = 0;
            int abwesend = 0;
            List<Map<String, Object>> details = new ArrayList<>();

            for (EmployeeEntity emp : employees) {
                try {
                    MyDayResponse myDay = timeTrackingService.getMyDay(emp.getId());
                    if (myDay.clockedIn()) {
                        anwesend++;
                        Map<String, Object> detail = new LinkedHashMap<>();
                        detail.put("name", emp.getFirstName() + " " + emp.getLastName());
                        detail.put("station", emp.getStationId() != null ? emp.getStationId().toString() : null);

                        // Find clock-in time
                        myDay.entries().stream()
                                .filter(e -> e.type() == TimeEntryType.CLOCK_IN)
                                .reduce((first, second) -> second) // last CLOCK_IN
                                .ifPresent(entry -> detail.put("eingestempeltSeit", entry.timestamp().toString()));

                        details.add(detail);
                    } else {
                        abwesend++;
                    }
                } catch (Exception e) {
                    abwesend++;
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("anwesend", anwesend);
            result.put("abwesend", abwesend);
            result.put("details", details);

            String json = objectMapper.writeValueAsString(result);
            if (json.length() > 2000) {
                // Truncate details list
                while (json.length() > 2000 && details.size() > 5) {
                    details.remove(details.size() - 1);
                    result.put("details", details);
                    result.put("hinweis", "Liste gekürzt, insgesamt " + anwesend + " anwesend");
                    json = objectMapper.writeValueAsString(result);
                }
            }
            return ToolResult.success(json);
        } catch (Exception e) {
            log.error("Error executing get_attendance_today: {}", e.getMessage(), e);
            return ToolResult.error("Fehler beim Laden der Anwesenheit: " + e.getMessage());
        }
    }
}
