"use client";

import { useApi, useMutation } from "./use-api";
import type {
  JobResponse,
  CreateJobRequest,
  UpdateJobRequest,
  ChangeJobStatusRequest,
  AssignJobRequest,
  QualityCheckResponse,
  CreateQualityCheckRequest,
  StationResponse,
  ShiftResponse,
} from "@/types/api";

export function useJobs() {
  return useApi<JobResponse[]>("/api/jobs");
}

export function useJob(id: string | null) {
  return useApi<JobResponse>(id ? `/api/jobs/${id}` : null);
}

export function useStations() {
  return useApi<StationResponse[]>("/api/stations");
}

export function useStation(id: string | null) {
  return useApi<StationResponse>(id ? `/api/stations/${id}` : null);
}

export function useShifts() {
  return useApi<ShiftResponse[]>("/api/shifts");
}

export function useQualityChecks(jobId: string | null) {
  return useApi<QualityCheckResponse[]>(
    jobId ? `/api/quality-checks?jobId=${jobId}` : null
  );
}

export function useJobMutations() {
  const { mutate, loading, error } = useMutation<CreateJobRequest, JobResponse>();

  return {
    createJob: (data: CreateJobRequest) => mutate("post", "/api/jobs", data),
    updateJob: (id: string, data: UpdateJobRequest) =>
      mutate("put", `/api/jobs/${id}`, data as unknown as CreateJobRequest),
    changeStatus: (id: string, data: ChangeJobStatusRequest) =>
      mutate("patch", `/api/jobs/${id}/status`, data as unknown as CreateJobRequest),
    assignJob: (id: string, data: AssignJobRequest) =>
      mutate("patch", `/api/jobs/${id}/assign`, data as unknown as CreateJobRequest),
    deleteJob: (id: string) => mutate("delete", `/api/jobs/${id}`),
    createQualityCheck: (data: CreateQualityCheckRequest) =>
      mutate("post", "/api/quality-checks", data as unknown as CreateJobRequest),
    loading,
    error,
  };
}
