"use client";

import { usePagedApi, useApi, useMutation } from "./use-api";
import type {
  EmployeeResponse,
  CreateEmployeeRequest,
  TimeEntryResponse,
  MyDayResponse,
  AbsenceResponse,
  CreateAbsenceRequest,
  QualificationResponse,
} from "@/types/api";

export function useEmployees() {
  return usePagedApi<EmployeeResponse>("/api/employees?size=1000");
}

export function useEmployee(id: string | null) {
  return useApi<EmployeeResponse>(id ? `/api/employees/${id}` : null);
}

export function useTimeEntries(employeeId: string | null) {
  return useApi<TimeEntryResponse[]>(
    employeeId ? `/api/time-entries?employeeId=${employeeId}` : null
  );
}

export function useMyDay(employeeId: string | null) {
  return useApi<MyDayResponse>(
    employeeId ? `/api/employees/${employeeId}/my-day` : null
  );
}

export function useAbsences(employeeId: string | null) {
  return useApi<AbsenceResponse[]>(
    employeeId ? `/api/absences?employeeId=${employeeId}` : null
  );
}

export function useQualifications(employeeId: string | null) {
  return useApi<QualificationResponse[]>(
    employeeId ? `/api/employees/${employeeId}/qualifications` : null
  );
}

export function usePeopleMutations() {
  const { mutate, loading, error } = useMutation<CreateEmployeeRequest, EmployeeResponse>();

  return {
    createEmployee: (data: CreateEmployeeRequest) =>
      mutate("post", "/api/employees", data),
    updateEmployee: (id: string, data: Partial<CreateEmployeeRequest>) =>
      mutate("put", `/api/employees/${id}`, data as CreateEmployeeRequest),
    clockIn: (employeeId: string) =>
      mutate("post", "/api/time-entries/clock-in", { employeeId } as unknown as CreateEmployeeRequest),
    clockOut: (employeeId: string) =>
      mutate("post", "/api/time-entries/clock-out", { employeeId } as unknown as CreateEmployeeRequest),
    jobStart: (employeeId: string, jobId: string) =>
      mutate("post", "/api/time-entries/job-start", { employeeId, jobId } as unknown as CreateEmployeeRequest),
    jobEnd: (employeeId: string, jobId: string) =>
      mutate("post", "/api/time-entries/job-end", { employeeId, jobId } as unknown as CreateEmployeeRequest),
    createAbsence: (data: CreateAbsenceRequest) =>
      mutate("post", "/api/absences", data as unknown as CreateEmployeeRequest),
    approveAbsence: (id: string) =>
      mutate("patch", `/api/absences/${id}/approve`),
    rejectAbsence: (id: string) =>
      mutate("patch", `/api/absences/${id}/reject`),
    addQualification: (employeeId: string, data: { qualification: string; certifiedAt?: string; expiresAt?: string }) =>
      mutate("post", `/api/employees/${employeeId}/qualifications`, data as unknown as CreateEmployeeRequest),
    loading,
    error,
  };
}
