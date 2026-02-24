"use client";

import { useApi, usePagedApi, useMutation } from "./use-api";
import type {
  KnowledgeCategoryResponse,
  KnowledgeTagResponse,
  KnowledgeArticleResponse,
  KnowledgeArticleSummaryResponse,
  KnowledgeSearchResultResponse,
} from "@/types/api";

// Categories
export function useKnowledgeCategories() {
  return useApi<KnowledgeCategoryResponse[]>("/api/knowledge/categories");
}

export function useKnowledgeCategoryMutations() {
  const { mutate, loading, error } = useMutation<unknown, KnowledgeCategoryResponse>();
  return {
    createCategory: (data: { name: string; color?: string }) =>
      mutate("post", "/api/knowledge/categories", data),
    updateCategory: (id: string, data: { name?: string; color?: string }) =>
      mutate("patch", `/api/knowledge/categories/${id}`, data),
    deleteCategory: (id: string) =>
      mutate("delete", `/api/knowledge/categories/${id}`, undefined),
    loading, error,
  };
}

// Tags
export function useKnowledgeTags() {
  return useApi<KnowledgeTagResponse[]>("/api/knowledge/tags");
}

export function useKnowledgeTagMutations() {
  const { mutate, loading, error } = useMutation<unknown, KnowledgeTagResponse>();
  return {
    createTag: (name: string) =>
      mutate("post", "/api/knowledge/tags", { name }),
    deleteTag: (id: string) =>
      mutate("delete", `/api/knowledge/tags/${id}`, undefined),
    loading, error,
  };
}

// Articles
export function useKnowledgeArticles(status?: string, categoryId?: string, search?: string) {
  const params = new URLSearchParams();
  params.set("size", "500");
  if (status) params.set("status", status);
  if (categoryId) params.set("categoryId", categoryId);
  if (search) params.set("search", search);
  return usePagedApi<KnowledgeArticleSummaryResponse>(`/api/knowledge/articles?${params}`);
}

export function useKnowledgeArticle(id: string | null) {
  return useApi<KnowledgeArticleResponse>(id ? `/api/knowledge/articles/${id}` : null);
}

export function useKnowledgeArticleMutations() {
  const { mutate, loading, error } = useMutation<unknown, KnowledgeArticleResponse>();
  return {
    createArticle: (data: {
      title: string;
      content?: string;
      excerpt?: string;
      categoryId?: string;
      tagIds?: string[];
    }) => mutate("post", "/api/knowledge/articles", data),
    updateArticle: (id: string, data: {
      title?: string;
      content?: string;
      excerpt?: string;
      categoryId?: string;
      tagIds?: string[];
    }) => mutate("patch", `/api/knowledge/articles/${id}`, data),
    publishArticle: (id: string) =>
      mutate("post", `/api/knowledge/articles/${id}/publish`, {}),
    archiveArticle: (id: string) =>
      mutate("post", `/api/knowledge/articles/${id}/archive`, {}),
    deleteArticle: (id: string) =>
      mutate("delete", `/api/knowledge/articles/${id}`, undefined),
    loading, error,
  };
}

// Search
export function useKnowledgeSearch(query: string | null) {
  return useApi<KnowledgeSearchResultResponse[]>(
    query && query.length >= 2 ? `/api/knowledge/search?q=${encodeURIComponent(query)}` : null
  );
}
