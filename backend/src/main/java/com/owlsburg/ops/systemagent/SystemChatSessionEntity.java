package com.owlsburg.ops.systemagent;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "system_chat_sessions")
@Getter
@Setter
@NoArgsConstructor
public class SystemChatSessionEntity extends SystemBaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "agent_instance_id", nullable = false)
    private UUID agentInstanceId;

    @Column
    private String title;
}
