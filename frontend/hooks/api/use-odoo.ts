"use client";

import { useApi, useMutation } from "./use-api";
import { apiClient } from "@/lib/api-client";
import type {
  OdooConfig,
  OdooConnectionTestResult,
  ApiResponse,
} from "@/types/api";

export function useOdooConfig() {
  return useApi<OdooConfig>("/api/odoo/config");
}

export function useOdooMutations() {
  const { mutate, loading, error } = useMutation<unknown, OdooConfig>();

  return {
    saveConfig: (data: { baseUrl: string; databaseName: string; apiKey: string; odooVersion?: string }) =>
      mutate("put", "/api/odoo/config", data),
    testConnection: async () => {
      const res = await apiClient.post<ApiResponse<OdooConnectionTestResult>>(
        "/api/odoo/test-connection",
        {}
      );
      return res.data;
    },
    loading,
    error,
  };
}
