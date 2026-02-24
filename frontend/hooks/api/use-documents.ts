"use client";

import { usePagedApi, useMutation } from "./use-api";
import { apiClient } from "@/lib/api-client";
import type { DocumentResponse, ApiResponse } from "@/types/api";
import { useCallback } from "react";

export function useDocuments(category?: string, status?: string) {
  const params = new URLSearchParams();
  params.set("size", "500");
  if (category) params.set("category", category);
  if (status) params.set("status", status);
  return usePagedApi<DocumentResponse>(`/api/documents?${params}`);
}

export function useDocumentMutations() {
  const { mutate, loading, error } = useMutation<unknown, DocumentResponse>();

  const uploadDocument = useCallback(async (
    file: File,
    title: string,
    description?: string,
    category?: string,
  ): Promise<DocumentResponse | null> => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("title", title);
    if (description) formData.append("description", description);
    if (category) formData.append("category", category);

    const res = await apiClient.upload<ApiResponse<DocumentResponse>>("/api/documents", formData);
    return res.data;
  }, []);

  return {
    uploadDocument,
    updateDocument: (id: string, data: {
      title?: string;
      description?: string;
      categoryId?: string;
      excerpt?: string;
    }) => mutate("patch", `/api/documents/${id}`, data),
    deleteDocument: (id: string) =>
      mutate("delete", `/api/documents/${id}`, undefined),
    linkDocument: (id: string, linkedType: string, linkedId: string) =>
      mutate("post", `/api/documents/${id}/link`, { linkedType, linkedId }),
    unlinkDocument: (id: string, linkId: string) =>
      mutate("delete", `/api/documents/${id}/link/${linkId}`, undefined),
    loading, error,
  };
}
