"use client";

import { useApi, useMutation } from "./use-api";
import type {
  ArticleResponse,
  CreateArticleRequest,
  StockSummaryResponse,
  MovementResponse,
  CreateMovementRequest,
  SupplierResponse,
  CreateSupplierRequest,
  SupplierArticleResponse,
  CriticalArticleResponse,
  CategoryResponse,
  UnitResponse,
} from "@/types/api";

export function useArticles() {
  return useApi<ArticleResponse[]>("/api/articles");
}

export function useArticle(id: string | null) {
  return useApi<ArticleResponse>(id ? `/api/articles/${id}` : null);
}

export function useStockSummary(articleId: string | null) {
  return useApi<StockSummaryResponse>(
    articleId ? `/api/stock/summary?articleId=${articleId}` : null
  );
}

export function useMovements(articleId: string | null) {
  return useApi<MovementResponse[]>(
    articleId ? `/api/stock/movements?articleId=${articleId}` : null
  );
}

export function useSuppliers() {
  return useApi<SupplierResponse[]>("/api/suppliers");
}

export function useSupplier(id: string | null) {
  return useApi<SupplierResponse>(id ? `/api/suppliers/${id}` : null);
}

export function useSupplierArticles(supplierId: string | null) {
  return useApi<SupplierArticleResponse[]>(
    supplierId ? `/api/suppliers/${supplierId}/articles` : null
  );
}

export function useCriticalArticles() {
  return useApi<CriticalArticleResponse[]>("/api/stock/critical");
}

export function useCategories() {
  return useApi<CategoryResponse[]>("/api/articles/categories");
}

export function useUnits() {
  return useApi<UnitResponse[]>("/api/articles/units");
}

export function useInventoryMutations() {
  const { mutate, loading, error } = useMutation<CreateArticleRequest, ArticleResponse>();

  return {
    createArticle: (data: CreateArticleRequest) =>
      mutate("post", "/api/articles", data),
    updateArticle: (id: string, data: Partial<CreateArticleRequest>) =>
      mutate("put", `/api/articles/${id}`, data as CreateArticleRequest),
    createMovement: (data: CreateMovementRequest) =>
      mutate("post", "/api/stock/movements", data as unknown as CreateArticleRequest),
    createSupplier: (data: CreateSupplierRequest) =>
      mutate("post", "/api/suppliers", data as unknown as CreateArticleRequest),
    updateSupplier: (id: string, data: Partial<CreateSupplierRequest>) =>
      mutate("put", `/api/suppliers/${id}`, data as unknown as CreateArticleRequest),
    loading,
    error,
  };
}
