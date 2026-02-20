package com.sindoflow.ops.agentinfra;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AgentTemplateService {

    private static final Logger log = LoggerFactory.getLogger(AgentTemplateService.class);

    private final AgentTemplateRepository templateRepository;

    public AgentTemplateService(AgentTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public List<AgentTemplateEntity> findAll() {
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AgentTemplateEntity findById(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AgentTemplate not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<AgentTemplateEntity> findByStatus(String status) {
        return templateRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<AgentTemplateEntity> findByRole(String role) {
        return templateRepository.findByRole(role);
    }

    @Transactional
    public AgentTemplateEntity create(AgentTemplateEntity template) {
        validateAllowedTools(template.getAllowedTools());
        log.info("Creating agent template: {}", template.getName());
        return templateRepository.save(template);
    }

    @Transactional
    public AgentTemplateEntity update(UUID id, AgentTemplateEntity updated) {
        AgentTemplateEntity existing = findById(id);
        existing.setName(updated.getName());
        existing.setRole(updated.getRole());
        existing.setDescription(updated.getDescription());
        existing.setBasePrompt(updated.getBasePrompt());
        validateAllowedTools(updated.getAllowedTools());
        existing.setAllowedTools(updated.getAllowedTools());
        existing.setTriggerTypes(updated.getTriggerTypes());
        existing.setMaxTokensPerRun(updated.getMaxTokensPerRun());
        existing.setDailyTokenBudget(updated.getDailyTokenBudget());
        existing.setStatus(updated.getStatus());
        existing.setVersion(updated.getVersion());
        log.info("Updated agent template: {}", id);
        return templateRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        AgentTemplateEntity existing = findById(id);
        templateRepository.delete(existing);
        log.info("Deleted agent template: {}", id);
    }

    /**
     * Validates that allowedTools is a valid JSON array of non-empty strings.
     * Basic validation - checks for non-null and non-blank.
     */
    public void validateAllowedTools(String allowedToolsJson) {
        if (allowedToolsJson == null || allowedToolsJson.isBlank()) {
            throw new IllegalArgumentException("allowedTools must not be null or blank");
        }
        // Basic structural validation - must start with [ and end with ]
        String trimmed = allowedToolsJson.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("allowedTools must be a JSON array");
        }
    }
}
