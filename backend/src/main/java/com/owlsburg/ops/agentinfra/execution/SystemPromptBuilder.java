package com.owlsburg.ops.agentinfra.execution;

import com.owlsburg.ops.agentinfra.AgentTemplateEntity;
import com.owlsburg.ops.agentinfra.tools.AgentTool;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public final class SystemPromptBuilder {

    private SystemPromptBuilder() {}

    public static String build(AgentTemplateEntity template, List<AgentTool> tools, String tenantId) {
        StringBuilder sb = new StringBuilder();

        // Base prompt from template
        if (template.getBasePrompt() != null && !template.getBasePrompt().isBlank()) {
            sb.append(template.getBasePrompt());
            sb.append("\n\n");
        }

        // Context info
        sb.append("## Kontext\n");
        sb.append("- Datum: ").append(LocalDate.now(ZoneOffset.UTC)).append("\n");
        sb.append("- Tenant: ").append(tenantId != null ? tenantId : "unbekannt").append("\n");
        sb.append("- Rolle: ").append(template.getRole()).append("\n");
        sb.append("\n");

        // Available tools
        if (!tools.isEmpty()) {
            sb.append("## Verfügbare Tools\n");
            sb.append("Du kannst folgende Tools verwenden:\n\n");
            for (AgentTool tool : tools) {
                sb.append("### ").append(tool.getName()).append("\n");
                sb.append(tool.getDescription()).append("\n");
                sb.append("Input-Schema: ").append(tool.getInputSchema()).append("\n\n");
            }
        }

        // Delegation capability
        sb.append("### delegate_to_agent\n");
        sb.append("Delegiere eine Aufgabe an einen anderen Agenten.\n");
        sb.append("Input-Schema: {\"type\":\"object\",\"properties\":{\"targetInstanceName\":{\"type\":\"string\",\"description\":\"Name der Ziel-Agent-Instanz\"},\"task\":{\"type\":\"string\",\"description\":\"Aufgabenbeschreibung für den Ziel-Agenten\"}},\"required\":[\"targetInstanceName\",\"task\"]}\n\n");

        // Constraints
        sb.append("## Regeln\n");
        sb.append("- Antworte immer auf Deutsch.\n");
        sb.append("- Nutze Tools um Daten abzufragen, bevor du Aussagen triffst.\n");
        sb.append("- Wenn du eine Aufgabe nicht selbst erledigen kannst, delegiere sie an den passenden Agenten.\n");
        sb.append("- Halte deine Antworten präzise und handlungsorientiert.\n");
        sb.append("- Maximale Iterationen: 15. Komme innerhalb dieser Grenze zu einem Ergebnis.\n");

        return sb.toString();
    }
}
