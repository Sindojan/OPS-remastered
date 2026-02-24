"use client";

import { useApi, useMutation } from "./use-api";
import { apiClient } from "@/lib/api-client";
import type {
  TenantConfigResponse,
  TenantConfigUpdateRequest,
  BudgetOverviewResponse,
  NotificationSettingsResponse,
  NotificationSettingsUpdateRequest,
  AvailableToolResponse,
  AgentTemplateDetailResponse,
  ApiResponse,
} from "@/types/api";

// ─── Tenant Config ─────────────────────────────────────

export function useTenantConfig() {
  return useApi<TenantConfigResponse>("/api/tenant");
}

export function useTenantConfigMutations() {
  const { mutate, loading, error } = useMutation<TenantConfigUpdateRequest, TenantConfigResponse>();
  return {
    patch: (data: TenantConfigUpdateRequest) =>
      mutate("patch", "/api/tenant", data),
    uploadLogo: async (file: File) => {
      const formData = new FormData();
      formData.append("file", file);
      return apiClient.upload<ApiResponse<TenantConfigResponse>>("/api/tenant/logo", formData);
    },
    deleteLogo: () =>
      mutate("delete", "/api/tenant/logo", undefined),
    loading,
    error,
  };
}

// ─── Budget ────────────────────────────────────────────

export function useBudgetOverview() {
  return useApi<BudgetOverviewResponse>("/api/budget/overview");
}

// ─── Notification Settings ─────────────────────────────

export function useNotificationSettings() {
  return useApi<NotificationSettingsResponse>("/api/users/me/notifications");
}

export function useNotificationSettingsMutations() {
  const { mutate, loading, error } = useMutation<NotificationSettingsUpdateRequest, NotificationSettingsResponse>();
  return {
    save: (data: NotificationSettingsUpdateRequest) =>
      mutate("put", "/api/users/me/notifications", data),
    loading,
    error,
  };
}

// ─── Available Tools ───────────────────────────────────

export function useAvailableTools() {
  return useApi<AvailableToolResponse[]>("/api/agent-templates/available-tools");
}

// ─── Agent Template Detail ─────────────────────────────

export function useAgentTemplateDetail(id: string | null) {
  return useApi<AgentTemplateDetailResponse>(id ? `/api/agent-templates/${id}` : null);
}

export function useAgentTemplateMutations() {
  const { mutate, loading, error } = useMutation<unknown, AgentTemplateDetailResponse>();
  return {
    patch: (id: string, data: { basePrompt?: string; allowedTools?: string; maxTokensPerRun?: number }) =>
      mutate("patch", `/api/agent-templates/${id}`, data),
    loading,
    error,
  };
}
