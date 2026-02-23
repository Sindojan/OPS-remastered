export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  email: string;
  role: string;
}

export interface LlmConfig {
  provider: string;
  defaultModel: string;
  hasApiKey: boolean;
}

export interface SaveLlmConfigRequest {
  provider: string;
  apiKey: string;
  defaultModel: string;
}

export interface AgentTemplate {
  id: string;
  name: string;
  role: string;
  description: string;
  allowedTools: string;
  maxTokensPerRun: number;
  dailyTokenBudget: number;
  status: string;
}

export interface AgentInstance {
  id: string;
  templateId: string;
  name: string;
  parentInstanceId: string | null;
  type: string;
  status: string;
  config: string;
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
}
