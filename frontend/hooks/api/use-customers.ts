"use client";

import { useApi } from "./use-api";
import type { CustomerResponse } from "@/types/api";

export function useCustomers() {
  return useApi<CustomerResponse[]>("/api/customers");
}

export function useCustomer(id: string | null) {
  return useApi<CustomerResponse>(id ? `/api/customers/${id}` : null);
}
