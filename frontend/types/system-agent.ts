export interface SystemAgentInstance {
  id: string;
  name: string;
  templateName: string;
  role: string | null;
  model: string;
  status: string;
  activityStatus: string;
  toolCount: number;
  lastActivity: string | null;
}

export interface SystemAgentDetail {
  id: string;
  name: string;
  templateName: string;
  role: string | null;
  model: string;
  status: string;
  activityStatus: string;
  customSystemPrompt: string | null;
  allowedTools: string[];
  templateTools: string[];
  hasToolOverride: boolean;
  maxTokensPerRun: number;
  dailyTokenBudget: number;
  templateMaxTokensPerRun: number;
  templateDailyTokenBudget: number;
  openIncidents: number;
  lastActivity: string | null;
}

export interface SystemAgentRunSummary {
  id: string;
  status: string;
  triggerType: string;
  tokensUsed: number;
  costUsd: number | null;
  startedAt: string;
  completedAt: string | null;
  errorMessage: string | null;
}

export interface SystemAgentMemorySummary {
  id: string;
  type: string;
  category: string;
  key: string;
  value: string;
  importance: number;
  createdAt: string;
}

export interface SystemAgentIncidentSummary {
  id: string;
  type: string;
  description: string;
  createdAt: string;
  resolvedAt: string | null;
}

export interface SystemToolInfo {
  name: string;
  description: string;
}

export interface SystemChatSession {
  id: string;
  title: string | null;
  agentInstanceId: string;
  createdAt: string;
  updatedAt: string;
}

export interface SystemChatMessage {
  id: string;
  role: string;
  content: string;
  createdAt: string;
}

export interface ApiCredential {
  id: string;
  serviceName: string;
  description: string | null;
  hasValue: boolean;
  metadata: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SystemLlmConfig {
  provider: string;
  defaultModel: string | null;
  hasApiKey: boolean;
  settings: string | null;
}
