package com.owlsburg.ops.agentinfra.memory;

public enum MemoryType {
    // Semantic (Wissen)
    FACT,
    PREFERENCE,
    RULE,

    // Procedural (Verhalten)
    LEARNING,
    STRATEGY,
    WORKFLOW,

    // Episodic (Interaktionen)
    DECISION,
    EVENT,

    // Allgemein
    NOTE;

    public boolean isSemantic() {
        return this == FACT || this == PREFERENCE || this == RULE;
    }

    public boolean isProcedural() {
        return this == LEARNING || this == STRATEGY || this == WORKFLOW;
    }

    public boolean isEpisodic() {
        return this == DECISION || this == EVENT;
    }
}
