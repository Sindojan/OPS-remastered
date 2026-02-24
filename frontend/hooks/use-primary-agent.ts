"use client";

import { useApi } from "@/hooks/api/use-api";
import type { MeResponse } from "@/types/api";

export function usePrimaryAgent() {
  const { data, loading, error } = useApi<MeResponse>("/api/users/me");
  return {
    agent: data?.primaryAgentInstance ?? null,
    loading,
    error,
  };
}
