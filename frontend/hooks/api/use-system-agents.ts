"use client";

import { useApi, useMutation } from "@/hooks/api/use-api";
import type {
  SystemAgentInstance,
  SystemAgentDetail,
  SystemAgentRunSummary,
  SystemAgentMemorySummary,
  SystemAgentIncidentSummary,
  SystemToolInfo,
} from "@/types/system-agent";

export function useSystemAgents() {
  return useApi<SystemAgentInstance[]>("/api/system/agents");
}

export function useSystemAgentDetail(id: string | null) {
  return useApi<SystemAgentDetail>(id ? `/api/system/agents/${id}` : null);
}

export function useSystemAgentRuns(id: string | null, page = 0) {
  return useApi<{ content: SystemAgentRunSummary[]; totalElements: number }>(
    id ? `/api/system/agents/${id}/runs?page=${page}&size=20` : null
  );
}

export function useSystemAgentMemories(
  id: string | null,
  category?: string
) {
  const params = category ? `?category=${category}&limit=50` : "?limit=50";
  return useApi<SystemAgentMemorySummary[]>(
    id ? `/api/system/agents/${id}/memories${params}` : null
  );
}

export function useSystemAgentIncidents(id: string | null) {
  return useApi<SystemAgentIncidentSummary[]>(
    id ? `/api/system/agents/${id}/incidents` : null
  );
}

export function useSystemAvailableTools() {
  return useApi<SystemToolInfo[]>("/api/system/agents/available-tools");
}

export function useSystemAgentMutations() {
  const { mutate, loading, error } = useMutation<unknown, unknown>();

  return {
    updateAgent: (id: string, data: Record<string, unknown>) =>
      mutate("patch", `/api/system/agents/${id}`, data),
    loading,
    error,
  };
}
