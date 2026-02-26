"use client";

import { useApi } from "@/hooks/api/use-api";
import { useMutation } from "@/hooks/api/use-api";
import type { ChatSessionResponse, ChatMessageResponse } from "@/types/api";

export function useChatSessions() {
  return useApi<ChatSessionResponse[]>("/api/chat/sessions");
}

export function useChatMessages(sessionId: string | null) {
  return useApi<ChatMessageResponse[]>(
    sessionId ? `/api/chat/sessions/${sessionId}/messages` : null
  );
}

export function useChatMutations() {
  const { mutate, loading, error } = useMutation<unknown, unknown>();

  return {
    deleteSession: (id: string) => mutate("delete", `/api/chat/sessions/${id}`),
    createSession: (agentInstanceId: string) =>
      mutate("post", "/api/chat/sessions", { agentInstanceId }),
    loading,
    error,
  };
}
