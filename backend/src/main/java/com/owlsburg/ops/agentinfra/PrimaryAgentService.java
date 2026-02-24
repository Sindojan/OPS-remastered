package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.agentinfra.dto.RoleAgentDefaultUpdateRequest;
import com.owlsburg.ops.auth.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrimaryAgentService {

    private static final Logger log = LoggerFactory.getLogger(PrimaryAgentService.class);

    private final AgentInstanceRepository agentInstanceRepository;
    private final RoleAgentDefaultRepository roleAgentDefaultRepository;

    public PrimaryAgentService(AgentInstanceRepository agentInstanceRepository,
                               RoleAgentDefaultRepository roleAgentDefaultRepository) {
        this.agentInstanceRepository = agentInstanceRepository;
        this.roleAgentDefaultRepository = roleAgentDefaultRepository;
    }

    @Transactional(readOnly = true)
    public AgentInstanceEntity resolveForUser(UserEntity user) {
        // 1. User has explicit assignment -> use it
        if (user.getPrimaryAgentInstanceId() != null) {
            return agentInstanceRepository.findById(user.getPrimaryAgentInstanceId())
                    .orElseGet(() -> {
                        log.warn("User {} has invalid primary agent instance {}, falling back to role default",
                                user.getId(), user.getPrimaryAgentInstanceId());
                        return resolveByRole(user.getRole().name());
                    });
        }
        // 2. Role default
        return resolveByRole(user.getRole().name());
    }

    private AgentInstanceEntity resolveByRole(String role) {
        return roleAgentDefaultRepository.findByRole(role)
                .map(d -> agentInstanceRepository.findById(d.getAgentInstanceId()).orElse(null))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<RoleAgentDefaultEntity> getAllDefaults() {
        return roleAgentDefaultRepository.findAll();
    }

    @Transactional
    public void updateDefaults(List<RoleAgentDefaultUpdateRequest> updates) {
        for (var update : updates) {
            var existing = roleAgentDefaultRepository.findByRole(update.role());
            if (existing.isPresent()) {
                existing.get().setAgentInstanceId(update.agentInstanceId());
                roleAgentDefaultRepository.save(existing.get());
            } else {
                var entity = new RoleAgentDefaultEntity();
                entity.setRole(update.role());
                entity.setAgentInstanceId(update.agentInstanceId());
                roleAgentDefaultRepository.save(entity);
            }
        }
        log.info("Updated {} role-agent defaults", updates.size());
    }
}
