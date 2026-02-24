package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.agentinfra.dto.RoleAgentDefaultResponse;
import com.owlsburg.ops.agentinfra.dto.RoleAgentDefaultUpdateRequest;
import com.owlsburg.ops.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings/role-agent-defaults")
public class RoleAgentDefaultController {

    private final PrimaryAgentService primaryAgentService;
    private final AgentInstanceRepository agentInstanceRepository;

    public RoleAgentDefaultController(PrimaryAgentService primaryAgentService,
                                      AgentInstanceRepository agentInstanceRepository) {
        this.primaryAgentService = primaryAgentService;
        this.agentInstanceRepository = agentInstanceRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<RoleAgentDefaultResponse>>> getAll() {
        List<RoleAgentDefaultResponse> defaults = primaryAgentService.getAllDefaults().stream()
                .map(d -> {
                    String instanceName = agentInstanceRepository.findById(d.getAgentInstanceId())
                            .map(AgentInstanceEntity::getName)
                            .orElse(null);
                    return new RoleAgentDefaultResponse(d.getId(), d.getRole(), d.getAgentInstanceId(), instanceName);
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(defaults));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> updateDefaults(@RequestBody List<RoleAgentDefaultUpdateRequest> updates) {
        primaryAgentService.updateDefaults(updates);
        return ResponseEntity.ok(ApiResponse.ok(null, "Role-agent defaults updated"));
    }
}
