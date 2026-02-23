"use client";

import { usePagedApi, useApi, useMutation } from "./use-api";
import type {
  ConversationResponse,
  CreateConversationRequest,
  MessageResponse,
  MessageCreateRequest,
  ConversationLinkResponse,
} from "@/types/api";

export function useConversations() {
  return usePagedApi<ConversationResponse>("/api/conversations?size=1000");
}

export function useConversation(id: string | null) {
  return useApi<ConversationResponse>(id ? `/api/conversations/${id}` : null);
}

export function useMessages(conversationId: string | null) {
  return useApi<MessageResponse[]>(
    conversationId ? `/api/conversations/${conversationId}/messages` : null
  );
}

export function useConversationLinks(conversationId: string | null) {
  return useApi<ConversationLinkResponse[]>(
    conversationId ? `/api/conversations/${conversationId}/links` : null
  );
}

export function useInboxMutations() {
  const { mutate, loading, error } = useMutation<CreateConversationRequest, ConversationResponse>();

  return {
    createConversation: (data: CreateConversationRequest) =>
      mutate("post", "/api/conversations", data),
    updateStatus: (id: string, status: string) =>
      mutate("patch", `/api/conversations/${id}/status`, { status } as unknown as CreateConversationRequest),
    updatePriority: (id: string, priority: string) =>
      mutate("patch", `/api/conversations/${id}`, { priority } as unknown as CreateConversationRequest),
    assign: (id: string, assignedTo: string) =>
      mutate("patch", `/api/conversations/${id}/assign`, { assignedTo } as unknown as CreateConversationRequest),
    sendMessage: (conversationId: string, data: MessageCreateRequest) =>
      mutate("post", `/api/conversations/${conversationId}/messages`, data as unknown as CreateConversationRequest),
    addTag: (conversationId: string, tag: string) =>
      mutate("post", `/api/conversations/${conversationId}/tags`, { tag } as unknown as CreateConversationRequest),
    addLink: (conversationId: string, linkedType: string, linkedId: string) =>
      mutate("post", `/api/conversations/${conversationId}/links`, { linkedType, linkedId } as unknown as CreateConversationRequest),
    loading,
    error,
  };
}
