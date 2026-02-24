"use client";

import { useState, useMemo, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Plus,
  Building2,
  Eye,
  AlertTriangle,
  XCircle,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/shared/kpi-card";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import {
  DomainStatusBadge,
  getCustomerStatusVariant,
} from "@/components/shared/domain-status-badge";
import { SkeletonCard } from "@/components/shared/skeleton-variants";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

import { toast } from "sonner";
import { useCustomers, useCustomerMutations } from "@/hooks/api/use-customers";
import type { CustomerResponse } from "@/types/api";
import { formatDate, humanizeStatus } from "@/lib/format";

// ─── Schemas ────────────────────────────────────────────

const createCustomerSchema = z.object({
  customerNumber: z.string().min(1, "Customer number is required"),
  companyName: z.string().min(1, "Company name is required"),
  shortName: z.string().optional(),
  taxId: z.string().optional(),
});

type CreateCustomerFormData = z.infer<typeof createCustomerSchema>;

const CUSTOMER_STATUSES = ["ACTIVE", "INACTIVE"];

// ─── Component ──────────────────────────────────────────

export default function CustomersPage() {
  const router = useRouter();
  const { data: customers, loading, error, refetch } = useCustomers();
  const mutations = useCustomerMutations();

  const [createOpen, setCreateOpen] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");

  const form = useForm<CreateCustomerFormData>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(createCustomerSchema) as any,
    defaultValues: {
      customerNumber: `K-${Date.now().toString(36).toUpperCase().slice(-4)}`,
      companyName: "",
      shortName: "",
      taxId: "",
    },
  });

  // ─── KPI Computations ──────────────────────────────────

  const kpis = useMemo(() => {
    if (!customers) return null;
    const total = customers.length;
    const active = customers.filter((c) => c.status === "ACTIVE").length;
    const inactive = customers.filter((c) => c.status === "INACTIVE").length;
    return { total, active, inactive };
  }, [customers]);

  // ─── Filtered Data ─────────────────────────────────────

  const filteredCustomers = useMemo(() => {
    if (!customers) return [];
    if (statusFilter === "ALL") return customers;
    return customers.filter((c) => c.status === statusFilter);
  }, [customers, statusFilter]);

  // ─── Table Columns ─────────────────────────────────────

  const columns: ColumnDef<CustomerResponse>[] = useMemo(
    () => [
      {
        id: "customerNumber",
        header: "Customer #",
        cell: (row) => (
          <span className="font-mono text-xs font-semibold text-foreground">
            {row.customerNumber ?? "–"}
          </span>
        ),
      },
      {
        id: "companyName",
        header: "Company Name",
        accessorKey: "companyName",
        sortable: true,
      },
      {
        id: "shortName",
        header: "Short Name",
        cell: (row) => (
          <span className="text-muted-foreground">
            {row.shortName ?? "–"}
          </span>
        ),
      },
      {
        id: "city",
        header: "City",
        cell: (row) => {
          const addr = row.addresses?.[0];
          return (
            <span className="text-muted-foreground">
              {addr?.city ?? "–"}
            </span>
          );
        },
      },
      {
        id: "status",
        header: "Status",
        accessorKey: "status",
        sortable: true,
        cell: (row) => (
          <DomainStatusBadge variant={getCustomerStatusVariant(row.status)}>
            {humanizeStatus(row.status)}
          </DomainStatusBadge>
        ),
      },
      {
        id: "createdAt",
        header: "Created",
        accessorKey: "createdAt",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {formatDate(row.createdAt)}
          </span>
        ),
      },
    ],
    []
  );

  // ─── Handlers ──────────────────────────────────────────

  const onCreateSubmit = useCallback(
    async (data: CreateCustomerFormData) => {
      try {
        const payload = {
          companyName: data.companyName,
          customerNumber: data.customerNumber,
          shortName: data.shortName || undefined,
          taxId: data.taxId || undefined,
        };
        const result = await mutations.createCustomer(payload);
        if (result) {
          toast.success("Customer created");
          setCreateOpen(false);
          form.reset({
            customerNumber: `K-${Date.now().toString(36).toUpperCase().slice(-4)}`,
            companyName: "",
            shortName: "",
            taxId: "",
          });
          refetch();
        }
      } catch (err) {
        toast.error("Failed to create customer", {
          description: err instanceof Error ? err.message : "Unknown error",
        });
      }
    },
    [mutations, form, refetch]
  );

  // ─── Error State ───────────────────────────────────────

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <AlertTriangle className="h-8 w-8 text-destructive" />
        <p className="text-sm text-muted-foreground">{error}</p>
        <Button variant="outline" size="sm" onClick={refetch}>
          Retry
        </Button>
      </div>
    );
  }

  // ─── Render ────────────────────────────────────────────

  return (
    <div className="space-y-6">
      <PageHeader
        title="Customers"
        description="Customer management and relationships"
        actions={
          <Button
            size="sm"
            className="gap-1.5"
            onClick={() => setCreateOpen(true)}
          >
            <Plus className="h-3.5 w-3.5" />
            New Customer
          </Button>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {loading ? (
          <>
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </>
        ) : (
          <>
            <KpiCard
              label="Total Customers"
              value={String(kpis?.total ?? 0)}
            />
            <KpiCard
              label="Active"
              value={String(kpis?.active ?? 0)}
              trend={
                kpis && kpis.total > 0
                  ? {
                      direction: kpis.active / kpis.total > 0.7 ? "up" : "down",
                      value: `${Math.round((kpis.active / kpis.total) * 100)}%`,
                    }
                  : undefined
              }
            />
            <KpiCard
              label="Inactive"
              value={String(kpis?.inactive ?? 0)}
            />
            <KpiCard label="Price Groups" value="–" />
          </>
        )}
      </div>

      {/* Data Table */}
      <DataTable<CustomerResponse>
        data={filteredCustomers}
        columns={columns}
        searchPlaceholder="Search customers..."
        searchKey="companyName"
        loading={loading}
        pageSize={15}
        onRowClick={(row) => router.push(`/customers/${row.id}`)}
        filterSlots={
          <div className="flex items-center gap-2">
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="h-9 w-40 text-sm">
                <SelectValue placeholder="All Statuses" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Statuses</SelectItem>
                {CUSTOMER_STATUSES.map((s) => (
                  <SelectItem key={s} value={s}>
                    {humanizeStatus(s)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        }
        rowActions={[
          {
            label: "View Details",
            icon: <Eye className="h-3.5 w-3.5" />,
            onClick: (row) => router.push(`/customers/${row.id}`),
          },
          {
            label: "Deactivate",
            icon: <XCircle className="h-3.5 w-3.5" />,
            onClick: async (row) => {
              try {
                await mutations.deleteCustomer(row.id);
                toast.success("Customer deactivated");
                refetch();
              } catch (err) {
                toast.error("Failed to deactivate", {
                  description: err instanceof Error ? err.message : "Unknown error",
                });
              }
            },
            variant: "destructive",
          },
        ]}
        emptyState={{
          icon: <Building2 className="h-8 w-8 text-muted-foreground/40" />,
          title: "No customers found",
          description: "Add your first customer to get started.",
          action: (
            <Button
              variant="outline"
              size="sm"
              className="mt-2"
              onClick={() => setCreateOpen(true)}
            >
              <Plus className="mr-1.5 h-3.5 w-3.5" />
              New Customer
            </Button>
          ),
        }}
      />

      {/* ═══ Create Customer Dialog ═══ */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Building2 className="h-4 w-4 text-primary" />
              New Customer
            </DialogTitle>
            <DialogDescription>
              Register a new customer in the system.
            </DialogDescription>
          </DialogHeader>
          <form
            onSubmit={form.handleSubmit(onCreateSubmit as any)}
            className="space-y-4"
          >
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="companyName">Company Name *</Label>
                <Input
                  id="companyName"
                  placeholder="Acme Corp"
                  {...form.register("companyName")}
                />
                {form.formState.errors.companyName && (
                  <p className="text-xs text-destructive">
                    {form.formState.errors.companyName.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="customerNumber">Customer # *</Label>
                <Input
                  id="customerNumber"
                  className="font-mono"
                  {...form.register("customerNumber")}
                />
                {form.formState.errors.customerNumber && (
                  <p className="text-xs text-destructive">
                    {form.formState.errors.customerNumber.message}
                  </p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="shortName">Short Name</Label>
                <Input
                  id="shortName"
                  placeholder="ACME"
                  {...form.register("shortName")}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="taxId">Tax ID</Label>
                <Input
                  id="taxId"
                  placeholder="DE123456789"
                  className="font-mono text-xs"
                  {...form.register("taxId")}
                />
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setCreateOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit" size="sm" disabled={mutations.loading}>
                {mutations.loading ? "Creating..." : "Create Customer"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
