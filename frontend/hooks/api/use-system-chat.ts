"use client";

import { useApi, useMutation } from "@/hooks/api/use-api";
import type { SystemChatSession, SystemChatMessage } from "@/types/system-agent";

export function useSystemChatSessions() {
  return useApi<SystemChatSession[]>("/api/system/chat/sessions");
}

export function useSystemChatMessages(sessionId: string | null) {
  return useApi<SystemChatMessage[]>(
    sessionId ? `/api/system/chat/sessions/${sessionId}/messages` : null
  );
}

export function useSystemChatMutations() {
  const { mutate, loading, error } = useMutation<unknown, unknown>();

  return {
    deleteSession: (id: string) =>
      mutate("delete", `/api/system/chat/sessions/${id}`),
    createSession: (agentInstanceId: string) =>
      mutate("post", "/api/system/chat/sessions", { agentInstanceId }),
    loading,
    error,
  };
}
