"use client";

import { useApi, useMutation } from "./use-api";
import type {
  MachineResponse,
  CreateMachineRequest,
  MaintenanceIntervalResponse,
  MaintenanceRecordResponse,
  MachineIncidentResponse,
  CreateMaintenanceIntervalRequest,
  PerformMaintenanceRequest,
  ReportIncidentRequest,
} from "@/types/api";

export function useMachines() {
  return useApi<MachineResponse[]>("/api/machines");
}

export function useMachine(id: string | null) {
  return useApi<MachineResponse>(id ? `/api/machines/${id}` : null);
}

export function useMaintenanceIntervals(machineId: string | null) {
  return useApi<MaintenanceIntervalResponse[]>(
    machineId ? `/api/maintenance/intervals?machineId=${machineId}` : null
  );
}

export function useMaintenanceRecords(machineId: string | null) {
  return useApi<MaintenanceRecordResponse[]>(
    machineId ? `/api/maintenance/records?machineId=${machineId}` : null
  );
}

export function useMachineIncidents(machineId: string | null) {
  return useApi<MachineIncidentResponse[]>(
    machineId ? `/api/machines/${machineId}/incidents` : null
  );
}

export function useMachineMutations() {
  const { mutate, loading, error } = useMutation<CreateMachineRequest, MachineResponse>();

  return {
    createMachine: (data: CreateMachineRequest) =>
      mutate("post", "/api/machines", data),
    updateMachine: (id: string, data: Partial<CreateMachineRequest>) =>
      mutate("put", `/api/machines/${id}`, data as CreateMachineRequest),
    changeStatus: (id: string, newStatus: string) =>
      mutate("patch", `/api/machines/${id}/status`, { newStatus } as unknown as CreateMachineRequest),
    createInterval: (data: CreateMaintenanceIntervalRequest) =>
      mutate("post", "/api/maintenance/intervals", data as unknown as CreateMachineRequest),
    performMaintenance: (data: PerformMaintenanceRequest) =>
      mutate("post", "/api/maintenance/perform", data as unknown as CreateMachineRequest),
    reportIncident: (machineId: string, data: ReportIncidentRequest) =>
      mutate("post", `/api/machines/${machineId}/incidents`, data as unknown as CreateMachineRequest),
    resolveIncident: (machineId: string, incidentId: string, notes?: string) =>
      mutate("patch", `/api/machines/${machineId}/incidents/${incidentId}/resolve`, { resolutionNotes: notes } as unknown as CreateMachineRequest),
    loading,
    error,
  };
}
