package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ChatSessionEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Column(name = "agent_instance_id", nullable = false)
    private java.util.UUID agentInstanceId;

    @Column
    private String title;
}
