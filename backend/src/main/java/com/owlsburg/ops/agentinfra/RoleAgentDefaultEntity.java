package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "role_agent_defaults")
@Getter
@Setter
@NoArgsConstructor
public class RoleAgentDefaultEntity extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String role;

    @Column(name = "agent_instance_id", nullable = false)
    private UUID agentInstanceId;
}
