"use client";

import { useState, useMemo, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Plus,
  Users,
  Eye,
  UserX,
  AlertTriangle,
  Copy,
  Check,
  EyeOff,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/shared/kpi-card";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import {
  DomainStatusBadge,
  getEmployeeStatusVariant,
} from "@/components/shared/domain-status-badge";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
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
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

import { useEmployees, usePeopleMutations } from "@/hooks/api/use-people";
import type { EmployeeResponse, EmployeeStatus } from "@/types/api";
import { formatDate, humanizeStatus } from "@/lib/format";
import { toast } from "sonner";

// ─── Schemas ────────────────────────────────────────────

const createEmployeeSchema = z.object({
  employeeNumber: z.string().min(1, "Employee number is required"),
  firstName: z.string().min(1, "First name is required"),
  lastName: z.string().min(1, "Last name is required"),
  email: z.string().email("Invalid email").optional().or(z.literal("")),
  phone: z.string().optional(),
  role: z.string().optional(),
  password: z.string().optional(),
  hireDate: z.string().optional(),
  stationId: z.string().optional(),
});

type CreateEmployeeFormData = z.infer<typeof createEmployeeSchema>;

const EMPLOYEE_STATUSES: EmployeeStatus[] = ["ACTIVE", "INACTIVE", "ON_LEAVE"];

const EMPLOYEE_ROLES = [
  "Operator",
  "Technician",
  "Shift Lead",
  "Quality Inspector",
  "Maintenance Technician",
  "Warehouse Clerk",
  "Logistics",
  "Team Lead",
  "Production Manager",
  "Office / Admin",
] as const;

const ROLE_TO_SYSTEM_ROLE: Record<string, string> = {
  "Operator": "WORKER",
  "Technician": "WORKER",
  "Shift Lead": "TEAM_LEAD",
  "Quality Inspector": "WORKER",
  "Maintenance Technician": "WORKER",
  "Warehouse Clerk": "WORKER",
  "Logistics": "WORKER",
  "Team Lead": "TEAM_LEAD",
  "Production Manager": "MANAGER",
  "Office / Admin": "ADMIN",
};

const SYSTEM_ROLE_LABELS: Record<string, string> = {
  WORKER: "Worker",
  TEAM_LEAD: "Team Lead",
  MANAGER: "Manager",
  ADMIN: "Admin",
};

// ─── Extended row type with computed field ──────────────

interface EmployeeRow extends EmployeeResponse {
  fullName: string;
}

// ─── Component ──────────────────────────────────────────

export default function EmployeesPage() {
  const router = useRouter();
  const { data: employees, loading, error, refetch } = useEmployees();
  const mutations = usePeopleMutations();

  const [createOpen, setCreateOpen] = useState(false);
  const [deactivateTarget, setDeactivateTarget] = useState<EmployeeResponse | null>(null);
  const [createdCredentials, setCreatedCredentials] = useState<{
    email: string;
    password: string;
    role: string;
  } | null>(null);
  const [copied, setCopied] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [useRandomPassword, setUseRandomPassword] = useState(true);

  // Filters
  const [statusFilter, setStatusFilter] = useState<string>("ALL");

  const form = useForm<CreateEmployeeFormData>({
    resolver: zodResolver(createEmployeeSchema),
    defaultValues: {
      employeeNumber: `EMP-${Date.now().toString(36).toUpperCase().slice(-6)}`,
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      role: "",
      hireDate: "",
      stationId: "",
    },
  });

  // ─── KPI Computations ──────────────────────────────────

  const kpis = useMemo(() => {
    if (!employees) return null;
    const active = employees.filter((e) => e.status === "ACTIVE").length;
    const onLeave = employees.filter((e) => e.status === "ON_LEAVE").length;
    return { total: employees.length, active, onLeave };
  }, [employees]);

  // ─── Filtered & Extended Data ─────────────────────────

  const tableData = useMemo(() => {
    if (!employees) return [];
    let result = employees;
    if (statusFilter !== "ALL") {
      result = result.filter((e) => e.status === statusFilter);
    }
    return result.map((e) => ({
      ...e,
      fullName: `${e.firstName} ${e.lastName}`,
    }));
  }, [employees, statusFilter]);

  // ─── Table Columns ─────────────────────────────────────

  const columns: ColumnDef<EmployeeRow>[] = useMemo(
    () => [
      {
        id: "employeeNumber",
        header: "Employee #",
        accessorKey: "employeeNumber",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs font-semibold text-foreground">
            {row.employeeNumber}
          </span>
        ),
      },
      {
        id: "fullName",
        header: "Name",
        accessorKey: "fullName",
        sortable: true,
        cell: (row) => (
          <span className="font-medium text-foreground">
            {row.firstName} {row.lastName}
          </span>
        ),
      },
      {
        id: "role",
        header: "Role",
        accessorKey: "role",
        sortable: true,
        cell: (row) => (
          <span className="text-muted-foreground">{row.role || "–"}</span>
        ),
      },
      {
        id: "stationId",
        header: "Station",
        accessorKey: "stationId",
        sortable: false,
        cell: (row) =>
          row.stationId ? (
            <span className="font-mono text-xs text-muted-foreground">
              {row.stationId.slice(0, 8)}...
            </span>
          ) : (
            <span className="text-muted-foreground">–</span>
          ),
      },
      {
        id: "status",
        header: "Status",
        accessorKey: "status",
        sortable: true,
        cell: (row) => (
          <DomainStatusBadge
            variant={getEmployeeStatusVariant(row.status)}
            pulse={row.status === "ON_LEAVE"}
          >
            {humanizeStatus(row.status)}
          </DomainStatusBadge>
        ),
      },
      {
        id: "email",
        header: "Email",
        accessorKey: "email",
        sortable: true,
        cell: (row) => (
          <span className="text-xs text-muted-foreground">
            {row.email || "–"}
          </span>
        ),
      },
      {
        id: "hireDate",
        header: "Hire Date",
        accessorKey: "hireDate",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {formatDate(row.hireDate)}
          </span>
        ),
      },
    ],
    []
  );

  // ─── Handlers ──────────────────────────────────────────

  const onCreateSubmit = useCallback(
    async (data: CreateEmployeeFormData) => {
      const systemRole = data.role ? ROLE_TO_SYSTEM_ROLE[data.role] : undefined;
      const payload: Record<string, unknown> = {
        employeeNumber: data.employeeNumber,
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email || undefined,
        phone: data.phone || undefined,
        role: data.role || undefined,
        hireDate: data.hireDate || undefined,
        stationId: data.stationId || undefined,
      };

      // If email + role are set, send systemRole to auto-create user account
      if (data.email && systemRole) {
        payload.systemRole = systemRole;
        if (!useRandomPassword && data.password) {
          payload.password = data.password;
        }
      }

      try {
        const result = await mutations.createEmployee(payload as any);
        if (result) {
          // Check if user credentials were returned
          const res = result as any;
          if (res.userCredentials) {
            setCreatedCredentials({
              email: res.userCredentials.email,
              password: res.userCredentials.password,
              role: res.userCredentials.role,
            });
          }
          toast.success("Employee created successfully");
          setCreateOpen(false);
          form.reset({
            employeeNumber: `EMP-${Date.now().toString(36).toUpperCase().slice(-6)}`,
            firstName: "",
            lastName: "",
            email: "",
            phone: "",
            role: "",
            password: "",
            hireDate: "",
            stationId: "",
          });
          setUseRandomPassword(true);
          refetch();
        }
      } catch (err) {
        toast.error("Failed to create employee", { description: err instanceof Error ? err.message : "Unknown error" });
      }
    },
    [mutations, form, refetch, useRandomPassword]
  );

  const handleDeactivate = useCallback(async () => {
    if (!deactivateTarget) return;
    try {
      const result = await mutations.updateEmployee(deactivateTarget.id, {
        firstName: deactivateTarget.firstName,
        lastName: deactivateTarget.lastName,
        employeeNumber: deactivateTarget.employeeNumber,
      });
      if (result) {
        toast.success("Employee deactivated successfully");
        setDeactivateTarget(null);
        refetch();
      }
    } catch (err) {
      toast.error("Failed to deactivate employee", { description: err instanceof Error ? err.message : "Unknown error" });
    }
  }, [mutations, deactivateTarget, refetch]);

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
        title="People"
        description="Employee management and workforce overview"
        actions={
          <Button
            size="sm"
            className="gap-1.5"
            onClick={() => setCreateOpen(true)}
          >
            <Plus className="h-3.5 w-3.5" />
            New Employee
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
              label="Active Employees"
              value={String(kpis?.active ?? 0)}
              trend={
                kpis && kpis.total > 0
                  ? {
                      direction:
                        kpis.active / kpis.total > 0.8 ? "up" : "neutral",
                      value: `${Math.round((kpis.active / kpis.total) * 100)}% of total`,
                    }
                  : undefined
              }
            />
            <KpiCard label="Present Today" value="–" />
            <KpiCard
              label="On Leave Today"
              value={String(kpis?.onLeave ?? 0)}
              trend={
                kpis && kpis.onLeave > 0
                  ? { direction: "down", value: `${kpis.onLeave} away` }
                  : undefined
              }
            />
            <KpiCard label="Pending Requests" value="–" />
          </>
        )}
      </div>

      {/* Data Table */}
      <DataTable<EmployeeRow>
        data={tableData}
        columns={columns}
        searchPlaceholder="Search by name..."
        searchKey="fullName"
        loading={loading}
        pageSize={15}
        onRowClick={(row) => router.push(`/people/${row.id}`)}
        filterSlots={
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="h-9 w-40 text-sm">
              <SelectValue placeholder="All Statuses" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Statuses</SelectItem>
              {EMPLOYEE_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {humanizeStatus(s)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        }
        rowActions={[
          {
            label: "View Details",
            icon: <Eye className="h-3.5 w-3.5" />,
            onClick: (row) => router.push(`/people/${row.id}`),
          },
          {
            label: "Deactivate",
            icon: <UserX className="h-3.5 w-3.5" />,
            onClick: (row) => setDeactivateTarget(row),
            variant: "destructive",
          },
        ]}
        emptyState={{
          icon: <Users className="h-8 w-8 text-muted-foreground/40" />,
          title: "No employees found",
          description: "Add your first employee to get started.",
          action: (
            <Button
              variant="outline"
              size="sm"
              className="mt-2"
              onClick={() => setCreateOpen(true)}
            >
              <Plus className="mr-1.5 h-3.5 w-3.5" />
              New Employee
            </Button>
          ),
        }}
      />

      {/* ═══ Deactivate Confirmation ═══ */}
      <ConfirmationDialog
        open={!!deactivateTarget}
        title="Deactivate Employee"
        description={
          deactivateTarget
            ? `Are you sure you want to deactivate ${deactivateTarget.firstName} ${deactivateTarget.lastName} (${deactivateTarget.employeeNumber})? They will no longer appear in active workforce lists.`
            : ""
        }
        variant="destructive"
        confirmLabel="Deactivate"
        onConfirm={handleDeactivate}
        onCancel={() => setDeactivateTarget(null)}
      />

      {/* ═══ Create Employee Dialog ═══ */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Users className="h-4 w-4 text-primary" />
              New Employee
            </DialogTitle>
            <DialogDescription>
              Register a new employee in the system.
            </DialogDescription>
          </DialogHeader>
          <form
            onSubmit={form.handleSubmit(onCreateSubmit as any)}
            className="space-y-4"
          >
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="firstName">First Name *</Label>
                <Input
                  id="firstName"
                  placeholder="Max"
                  {...form.register("firstName")}
                />
                {form.formState.errors.firstName && (
                  <p className="text-xs text-destructive">
                    {form.formState.errors.firstName.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="lastName">Last Name *</Label>
                <Input
                  id="lastName"
                  placeholder="Mustermann"
                  {...form.register("lastName")}
                />
                {form.formState.errors.lastName && (
                  <p className="text-xs text-destructive">
                    {form.formState.errors.lastName.message}
                  </p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="employeeNumber">Employee # *</Label>
                <Input
                  id="employeeNumber"
                  className="font-mono"
                  {...form.register("employeeNumber")}
                />
                {form.formState.errors.employeeNumber && (
                  <p className="text-xs text-destructive">
                    {form.formState.errors.employeeNumber.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="max@company.de"
                  {...form.register("email")}
                />
                {form.formState.errors.email && (
                  <p className="text-xs text-destructive">
                    {form.formState.errors.email.message}
                  </p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="phone">Phone</Label>
                <Input
                  id="phone"
                  placeholder="+49 170 1234567"
                  {...form.register("phone")}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="role">Role</Label>
                <Select
                  value={form.watch("role") || ""}
                  onValueChange={(v) => form.setValue("role", v)}
                >
                  <SelectTrigger id="role">
                    <SelectValue placeholder="Select role..." />
                  </SelectTrigger>
                  <SelectContent>
                    {EMPLOYEE_ROLES.map((r) => (
                      <SelectItem key={r} value={r}>
                        {r}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {form.watch("role") && ROLE_TO_SYSTEM_ROLE[form.watch("role")!] && (
                  <p className="text-[11px] text-muted-foreground">
                    System role: <span className="font-semibold">{SYSTEM_ROLE_LABELS[ROLE_TO_SYSTEM_ROLE[form.watch("role")!]] || ROLE_TO_SYSTEM_ROLE[form.watch("role")!]}</span>
                  </p>
                )}
              </div>
            </div>

            {/* Password section – only if email is filled */}
            {form.watch("email") && form.watch("role") && (
              <div className="rounded-lg border bg-muted/30 p-3 space-y-3">
                <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  User Account
                </p>
                <p className="text-xs text-muted-foreground">
                  A user account with role <span className="font-semibold">{SYSTEM_ROLE_LABELS[ROLE_TO_SYSTEM_ROLE[form.watch("role")!]] || "–"}</span> will be created automatically.
                </p>
                <div className="flex items-center gap-2">
                  <Checkbox
                    id="random-pw"
                    checked={useRandomPassword}
                    onCheckedChange={(v) => setUseRandomPassword(!!v)}
                  />
                  <Label htmlFor="random-pw" className="text-sm font-normal">
                    Generate random password
                  </Label>
                </div>
                {!useRandomPassword && (
                  <div className="space-y-1">
                    <Label htmlFor="password">Password</Label>
                    <div className="relative">
                      <Input
                        id="password"
                        type={showPassword ? "text" : "password"}
                        placeholder="Min. 8 characters"
                        {...form.register("password")}
                      />
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="absolute right-1 top-1/2 -translate-y-1/2 h-7 w-7 p-0"
                        onClick={() => setShowPassword(!showPassword)}
                      >
                        {showPassword ? (
                          <EyeOff className="h-3.5 w-3.5" />
                        ) : (
                          <Eye className="h-3.5 w-3.5" />
                        )}
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="hireDate">Hire Date</Label>
                <Input
                  id="hireDate"
                  type="date"
                  {...form.register("hireDate")}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="stationId">Station ID</Label>
                <Input
                  id="stationId"
                  placeholder="Station UUID"
                  className="font-mono text-xs"
                  {...form.register("stationId")}
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
                {mutations.loading ? "Creating..." : "Create Employee"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
      {/* ═══ Created Credentials Dialog ═══ */}
      <Dialog
        open={!!createdCredentials}
        onOpenChange={(v) => {
          if (!v) {
            setCreatedCredentials(null);
            setCopied(false);
          }
        }}
      >
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>User Account Created</DialogTitle>
            <DialogDescription>
              Save these login credentials. The password cannot be retrieved later.
            </DialogDescription>
          </DialogHeader>
          <div className="rounded-lg border border-primary/20 bg-primary/5 p-4 space-y-2">
            <div>
              <span className="text-xs font-medium text-muted-foreground">Email</span>
              <p className="font-mono text-sm">{createdCredentials?.email}</p>
            </div>
            <div>
              <span className="text-xs font-medium text-muted-foreground">Password</span>
              <p className="font-mono text-sm select-all">{createdCredentials?.password}</p>
            </div>
            <div>
              <span className="text-xs font-medium text-muted-foreground">System Role</span>
              <p className="text-sm font-semibold">
                {createdCredentials?.role && SYSTEM_ROLE_LABELS[createdCredentials.role]
                  ? SYSTEM_ROLE_LABELS[createdCredentials.role]
                  : createdCredentials?.role}
              </p>
            </div>
            <Button
              variant="outline"
              size="sm"
              className="mt-2 gap-1.5"
              onClick={() => {
                if (!createdCredentials) return;
                navigator.clipboard.writeText(
                  `Email: ${createdCredentials.email}\nPassword: ${createdCredentials.password}`
                );
                setCopied(true);
                setTimeout(() => setCopied(false), 2000);
              }}
            >
              {copied ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
              {copied ? "Copied" : "Copy Credentials"}
            </Button>
          </div>
          <DialogFooter>
            <Button
              size="sm"
              onClick={() => {
                setCreatedCredentials(null);
                setCopied(false);
              }}
            >
              Done
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
