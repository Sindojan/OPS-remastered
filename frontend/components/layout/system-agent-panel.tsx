"use client";

import { AgentPanel, type AgentPanelConfig } from "./agent-panel";

const SYSTEM_CEO_ID = "b0000000-0000-0000-0000-000000000001";

const systemConfig: AgentPanelConfig = {
  agentName: "System-CEO",
  agentId: SYSTEM_CEO_ID,
  model: "claude-opus-4-6",
  sessionsPath: "/api/system/chat/sessions",
  messagePath: "/api/system/chat/message",
  lastRunPath: `/api/system/agents/${SYSTEM_CEO_ID}/last-run`,
  greeting: "Guten Tag! Ich bin der System-CEO. Wie kann ich Ihnen bei der Plattform-Verwaltung helfen?",
  placeholder: "Nachricht an System-CEO...",
};

interface SystemAgentPanelProps {
  open: boolean;
  onClose: () => void;
}

export function SystemAgentPanel({ open, onClose }: SystemAgentPanelProps) {
  return <AgentPanel open={open} onClose={onClose} config={systemConfig} />;
}
