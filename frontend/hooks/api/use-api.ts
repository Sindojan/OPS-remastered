"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { apiClient } from "@/lib/api-client";
import type { ApiResponse } from "@/types/api";

interface UseApiState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

interface UseApiReturn<T> extends UseApiState<T> {
  refetch: () => Promise<void>;
}

export function useApi<T>(path: string | null): UseApiReturn<T> {
  const [state, setState] = useState<UseApiState<T>>({
    data: null,
    loading: !!path,
    error: null,
  });
  const abortRef = useRef(false);

  const fetchData = useCallback(async () => {
    if (!path) return;
    setState((s) => ({ ...s, loading: true, error: null }));
    try {
      const res = await apiClient.get<ApiResponse<T>>(path);
      if (!abortRef.current) {
        setState({ data: res.data, loading: false, error: null });
      }
    } catch (e) {
      if (!abortRef.current) {
        setState((s) => ({
          ...s,
          loading: false,
          error: e instanceof Error ? e.message : "Unknown error",
        }));
      }
    }
  }, [path]);

  useEffect(() => {
    abortRef.current = false;
    fetchData();
    return () => {
      abortRef.current = true;
    };
  }, [fetchData]);

  return { ...state, refetch: fetchData };
}

export function useMutation<TReq, TRes = unknown>() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const mutate = useCallback(
    async (
      method: "post" | "put" | "patch" | "delete",
      path: string,
      body?: TReq
    ): Promise<TRes | null> => {
      setLoading(true);
      setError(null);
      try {
        let res: ApiResponse<TRes>;
        switch (method) {
          case "post":
            res = await apiClient.post<ApiResponse<TRes>>(path, body);
            break;
          case "put":
            res = await apiClient.put<ApiResponse<TRes>>(path, body);
            break;
          case "patch":
            res = await apiClient.patch<ApiResponse<TRes>>(path, body);
            break;
          case "delete":
            res = await apiClient.delete<ApiResponse<TRes>>(path);
            break;
        }
        setLoading(false);
        return res.data;
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Unknown error";
        setError(msg);
        setLoading(false);
        return null;
      }
    },
    []
  );

  return { mutate, loading, error };
}
