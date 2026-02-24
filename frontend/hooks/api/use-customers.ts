"use client";

import { useApi, usePagedApi, useMutation } from "./use-api";
import type { CustomerResponse, ContactResponse, AddressResponse } from "@/types/api";

export function useCustomers() {
  return usePagedApi<CustomerResponse>("/api/customers?size=500");
}

export function useCustomer(id: string | null) {
  return useApi<CustomerResponse>(id ? `/api/customers/${id}` : null);
}

export function useCustomerMutations() {
  const { mutate, loading, error } = useMutation<unknown, CustomerResponse>();

  return {
    createCustomer: (data: {
      companyName: string;
      taxId?: string;
      customerNumber?: string;
      shortName?: string;
    }) => mutate("post", "/api/customers", data),

    updateCustomer: (id: string, data: {
      companyName?: string;
      taxId?: string;
      customerNumber?: string;
      shortName?: string;
    }) => mutate("put", `/api/customers/${id}`, data),

    deleteCustomer: (id: string) =>
      mutate("delete", `/api/customers/${id}`, undefined),

    addContact: (customerId: string, data: {
      firstName: string;
      lastName: string;
      email?: string;
      phone?: string;
      position?: string;
      isPrimary?: boolean;
    }) => mutate("post", `/api/customers/${customerId}/contacts`, data),

    updateContact: (customerId: string, contactId: string, data: {
      firstName?: string;
      lastName?: string;
      email?: string;
      phone?: string;
      position?: string;
      isPrimary?: boolean;
    }) => mutate("put", `/api/customers/${customerId}/contacts/${contactId}`, data),

    deleteContact: (customerId: string, contactId: string) =>
      mutate("delete", `/api/customers/${customerId}/contacts/${contactId}`, undefined),

    addAddress: (customerId: string, data: {
      type: string;
      street: string;
      zip: string;
      city: string;
      country?: string;
    }) => mutate("post", `/api/customers/${customerId}/addresses`, data),

    updateAddress: (customerId: string, addressId: string, data: {
      type?: string;
      street?: string;
      zip?: string;
      city?: string;
      country?: string;
    }) => mutate("put", `/api/customers/${customerId}/addresses/${addressId}`, data),

    deleteAddress: (customerId: string, addressId: string) =>
      mutate("delete", `/api/customers/${customerId}/addresses/${addressId}`, undefined),

    loading,
    error,
  };
}
