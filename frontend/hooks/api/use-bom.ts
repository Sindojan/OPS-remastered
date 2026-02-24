"use client";

import { useApi, usePagedApi, useMutation } from "./use-api";
import type {
  PartResponse,
  CreatePartRequest,
  BomVersionResponse,
  BomItemResponse,
  BomItemRequest,
  ProcessPlanResponse,
  ProcessStepResponse,
  ProcessStepRequest,
  CalculationResponse,
  CalculateRequest,
} from "@/types/api";

// ─── Parts ────────────────────────────────────────────

export function useParts() {
  return usePagedApi<PartResponse>("/api/parts?size=500");
}

export function usePart(id: string | null) {
  return useApi<PartResponse>(id ? `/api/parts/${id}` : null);
}

export function usePartMutations() {
  const { mutate, loading, error } = useMutation<unknown, PartResponse>();

  return {
    createPart: (data: CreatePartRequest) =>
      mutate("post", "/api/parts", data),
    updatePart: (id: string, data: Partial<CreatePartRequest>) =>
      mutate("put", `/api/parts/${id}`, data),
    deletePart: (id: string) =>
      mutate("delete", `/api/parts/${id}`, undefined),
    loading,
    error,
  };
}

// ─── BOM Versions & Items ─────────────────────────────

export function useBomVersionActive(partId: string | null) {
  return useApi<BomVersionResponse>(partId ? `/api/bom/parts/${partId}/active` : null);
}

export function useBomItems(versionId: string | null) {
  return useApi<BomItemResponse[]>(versionId ? `/api/bom/versions/${versionId}/items` : null);
}

export function useBomMutations() {
  const { mutate, loading, error } = useMutation<unknown, unknown>();

  return {
    createVersion: (data: { partId: string; versionNumber?: number; validFrom?: string; createdBy?: string }) =>
      mutate("post", "/api/bom/versions", data),
    addItem: (versionId: string, data: BomItemRequest) =>
      mutate("post", `/api/bom/versions/${versionId}/items`, data),
    removeItem: (itemId: string) =>
      mutate("delete", `/api/bom/items/${itemId}`, undefined),
    activateVersion: (versionId: string) =>
      mutate("patch", `/api/bom/versions/${versionId}/activate`, undefined),
    loading,
    error,
  };
}

// ─── Process Plans & Steps ────────────────────────────

export function useProcessPlans(partId: string | null) {
  return useApi<ProcessPlanResponse[]>(partId ? `/api/process-plans?partId=${partId}` : null);
}

export function useProcessSteps(planId: string | null) {
  return useApi<ProcessStepResponse[]>(planId ? `/api/process-plans/${planId}/steps` : null);
}

export function useProcessPlanMutations() {
  const { mutate, loading, error } = useMutation<unknown, unknown>();

  return {
    createPlan: (data: { partId: string; versionNumber?: number; name: string; validFrom?: string; createdBy?: string }) =>
      mutate("post", "/api/process-plans", data),
    addStep: (planId: string, data: ProcessStepRequest) =>
      mutate("post", `/api/process-plans/${planId}/steps`, data),
    updateStep: (stepId: string, data: ProcessStepRequest) =>
      mutate("put", `/api/process-plans/steps/${stepId}`, data),
    removeStep: (stepId: string) =>
      mutate("delete", `/api/process-plans/steps/${stepId}`, undefined),
    activatePlan: (planId: string) =>
      mutate("patch", `/api/process-plans/${planId}/activate`, undefined),
    loading,
    error,
  };
}

// ─── Calculations ─────────────────────────────────────

export function useCalculations(partId: string | null) {
  return useApi<CalculationResponse[]>(partId ? `/api/calculations/history/part/${partId}` : null);
}

export function useCalculationMutations() {
  const { mutate, loading, error } = useMutation<unknown, CalculationResponse>();

  return {
    calculate: (data: CalculateRequest) =>
      mutate("post", "/api/calculations/calculate", data),
    loading,
    error,
  };
}
