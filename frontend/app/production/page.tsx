"use client";

import { useState, useMemo, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useJobs, useJobMutations, useStations } from "@/hooks/api/use-jobs";
import type { JobResponse, JobStatus, CreateJobRequest } from "@/types/api";
import { formatDate, formatDateTime, daysUntil, formatNumber, humanizeStatus } from "@/lib/format";
import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/shared/kpi-card";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { SkeletonCard } from "@/components/shared/skeleton-variants";
import {
  DomainStatusBadge,
  getJobStatusVariant,
  getPriorityVariant,
} from "@/components/shared/domain-status-badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Plus,
  Eye,
  ArrowRightLeft,
  Trash2,
  Factory,
  AlertCircle,
  RefreshCw,
} from "lucide-react";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

// ─── Job Statuses for filtering ────────────────────────
const JOB_STATUSES: JobStatus[] = [
  "DRAFT",
  "RELEASED",
  "IN_PRODUCTION",
  "ON_HOLD",
  "COMPLETED",
  "CANCELLED",
];

const PRIORITIES = [1, 2, 3, 4, 5];

// ─── Create Job Schema ─────────────────────────────────
const createJobSchema = z.object({
  jobNumber: z.string().min(1, "Job number is required"),
  title: z.string().min(1, "Title is required"),
  customerId: z.string().optional(),
  priority: z.coerce.number().min(1).max(5),
  quantity: z.coerce.number().min(1, "Quantity must be at least 1"),
  deadline: z.string().optional().refine(
    (val) => {
      if (!val) return true;
      return new Date(val) > new Date();
    },
    { message: "Deadline must be in the future" }
  ),
  notes: z.string().optional(),
});

type CreateJobFormData = z.infer<typeof createJobSchema>;

// ─── Change Status Schema ───────────────────────────────
const VALID_TRANSITIONS: Record<JobStatus, JobStatus[]> = {
  DRAFT: ["RELEASED", "CANCELLED"],
  RELEASED: ["IN_PRODUCTION", "ON_HOLD", "CANCELLED"],
  IN_PRODUCTION: ["ON_HOLD", "COMPLETED", "CANCELLED"],
  ON_HOLD: ["IN_PRODUCTION", "CANCELLED"],
  COMPLETED: [],
  CANCELLED: [],
};

export default function ProductionOverviewPage() {
  const router = useRouter();
  const { data: jobs, loading: jobsLoading, error: jobsError, refetch } = useJobs();
  const { data: stations } = useStations();
  const { createJob, changeStatus, deleteJob, loading: mutating } = useJobMutations();

  // Dialog state
  const [createOpen, setCreateOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<JobResponse | null>(null);
  const [bulkDeleteIds, setBulkDeleteIds] = useState<string[] | null>(null);
  const [statusTarget, setStatusTarget] = useState<JobResponse | null>(null);
  const [selectedNewStatus, setSelectedNewStatus] = useState<JobStatus | "">("");
  const [statusReason, setStatusReason] = useState("");

  // Filters
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [priorityFilter, setPriorityFilter] = useState<string>("all");

  // Form
  const form = useForm<CreateJobFormData>({
    resolver: zodResolver(createJobSchema) as any,
    defaultValues: {
      jobNumber: `JOB-${Date.now()}`,
      title: "",
      customerId: "",
      priority: 3,
      quantity: 1,
      deadline: "",
      notes: "",
    },
  });

  // ─── Filtered jobs ────────────────────────────────────
  const filteredJobs = useMemo(() => {
    if (!jobs) return [];
    let result = jobs;
    if (statusFilter !== "all") {
      result = result.filter((j) => j.status === statusFilter);
    }
    if (priorityFilter !== "all") {
      result = result.filter((j) => j.priority === Number(priorityFilter));
    }
    return result;
  }, [jobs, statusFilter, priorityFilter]);

  // ─── KPI Calculations ────────────────────────────────
  const kpis = useMemo(() => {
    if (!jobs) return null;
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);

    const openToday = jobs.filter(
      (j) =>
        (j.status === "DRAFT" || j.status === "RELEASED") &&
        new Date(j.createdAt) >= today
    ).length;

    const inProduction = jobs.filter((j) => j.status === "IN_PRODUCTION").length;

    const completedThisWeek = jobs.filter(
      (j) => j.status === "COMPLETED" && j.completedAt && new Date(j.completedAt) >= weekAgo
    ).length;

    // Average lead time for completed jobs
    const completedJobs = jobs.filter((j) => j.status === "COMPLETED" && j.completedAt);
    const avgLeadTime =
      completedJobs.length > 0
        ? Math.round(
            completedJobs.reduce((sum, j) => {
              const start = new Date(j.createdAt).getTime();
              const end = new Date(j.completedAt!).getTime();
              return sum + (end - start) / (1000 * 60 * 60 * 24);
            }, 0) / completedJobs.length
          )
        : 0;

    // Sparkline: jobs created per day for the last 7 days
    const sparkData: number[] = [];
    for (let i = 6; i >= 0; i--) {
      const dayStart = new Date(today.getTime() - i * 24 * 60 * 60 * 1000);
      const dayEnd = new Date(dayStart.getTime() + 24 * 60 * 60 * 1000);
      sparkData.push(
        jobs.filter((j) => {
          const d = new Date(j.createdAt);
          return d >= dayStart && d < dayEnd;
        }).length
      );
    }

    return { openToday, inProduction, completedThisWeek, avgLeadTime, sparkData };
  }, [jobs]);

  // ─── Station lookup ───────────────────────────────────
  const stationMap = useMemo(() => {
    if (!stations) return new Map<string, string>();
    return new Map(stations.map((s) => [s.id, s.name]));
  }, [stations]);

  // ─── Handlers ─────────────────────────────────────────
  const handleCreateJob = useCallback(
    async (data: CreateJobFormData) => {
      const req: CreateJobRequest = {
        jobNumber: data.jobNumber,
        title: data.title,
        priority: data.priority,
        quantity: data.quantity,
        ...(data.customerId ? { customerId: data.customerId } : {}),
        ...(data.deadline ? { deadline: data.deadline } : {}),
        ...(data.notes ? { notes: data.notes } : {}),
      };
      const result = await createJob(req);
      if (result) {
        setCreateOpen(false);
        form.reset({ jobNumber: `JOB-${Date.now()}`, title: "", customerId: "", priority: 3, quantity: 1, deadline: "", notes: "" });
        refetch();
      }
    },
    [createJob, form, refetch]
  );

  const handleDeleteJob = useCallback(async () => {
    if (!deleteTarget) return;
    await deleteJob(deleteTarget.id);
    setDeleteTarget(null);
    refetch();
  }, [deleteTarget, deleteJob, refetch]);

  const handleBulkDelete = useCallback(async () => {
    if (!bulkDeleteIds) return;
    for (const id of bulkDeleteIds) {
      await deleteJob(id);
    }
    setBulkDeleteIds(null);
    refetch();
  }, [bulkDeleteIds, deleteJob, refetch]);

  const handleChangeStatus = useCallback(async () => {
    if (!statusTarget || !selectedNewStatus) return;
    await changeStatus(statusTarget.id, {
      newStatus: selectedNewStatus as JobStatus,
      reason: statusReason || undefined,
    });
    setStatusTarget(null);
    setSelectedNewStatus("");
    setStatusReason("");
    refetch();
  }, [statusTarget, selectedNewStatus, statusReason, changeStatus, refetch]);

  // ─── Columns ──────────────────────────────────────────
  const columns: ColumnDef<JobResponse>[] = useMemo(
    () => [
      {
        id: "jobNumber",
        header: "Job #",
        accessorKey: "jobNumber",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs font-semibold text-foreground">
            {row.jobNumber}
          </span>
        ),
      },
      {
        id: "title",
        header: "Title",
        accessorKey: "title",
        sortable: true,
        cell: (row) => (
          <span className="max-w-[200px] truncate text-sm">{row.title}</span>
        ),
      },
      {
        id: "customerId",
        header: "Customer",
        accessorKey: "customerId",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {row.customerId ? `${row.customerId.slice(0, 8)}...` : "-"}
          </span>
        ),
      },
      {
        id: "status",
        header: "Status",
        accessorKey: "status",
        sortable: true,
        cell: (row) => (
          <DomainStatusBadge
            variant={getJobStatusVariant(row.status)}
            pulse={row.status === "IN_PRODUCTION"}
          >
            {humanizeStatus(row.status)}
          </DomainStatusBadge>
        ),
      },
      {
        id: "priority",
        header: "Priority",
        accessorKey: "priority",
        sortable: true,
        sortFn: (a, b) => a.priority - b.priority,
        cell: (row) => (
          <DomainStatusBadge variant={getPriorityVariant(row.priority)}>
            P{row.priority}
          </DomainStatusBadge>
        ),
      },
      {
        id: "station",
        header: "Station",
        cell: (row) => (
          <span className="text-sm text-muted-foreground">
            {row.assignedStationId
              ? stationMap.get(row.assignedStationId) ?? "Unknown"
              : "-"}
          </span>
        ),
      },
      {
        id: "deadline",
        header: "Deadline",
        accessorKey: "deadline",
        sortable: true,
        cell: (row) => {
          const days = daysUntil(row.deadline);
          const isUrgent = days !== null && days < 3 && days >= 0;
          const isOverdue = days !== null && days < 0;
          return (
            <span
              className={`font-mono text-xs ${
                isOverdue
                  ? "font-semibold text-red-500"
                  : isUrgent
                    ? "font-semibold text-amber-500"
                    : "text-muted-foreground"
              }`}
            >
              {formatDate(row.deadline)}
              {days !== null && (
                <span className="ml-1 text-[10px]">
                  ({days < 0 ? `${Math.abs(days)}d overdue` : `${days}d`})
                </span>
              )}
            </span>
          );
        },
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
    [stationMap]
  );

  // ─── Error state ──────────────────────────────────────
  if (jobsError) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <AlertCircle className="h-10 w-10 text-destructive/60" />
        <p className="text-sm text-muted-foreground">{jobsError}</p>
        <Button variant="outline" size="sm" onClick={refetch} className="gap-2">
          <RefreshCw className="h-3.5 w-3.5" />
          Retry
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* ─── Header ──────────────────────────────────────── */}
      <PageHeader
        title="Production"
        description="Manage production jobs, track status, and monitor progress"
        actions={
          <Button size="sm" className="gap-1.5" onClick={() => setCreateOpen(true)}>
            <Plus className="h-3.5 w-3.5" />
            New Job
          </Button>
        }
      />

      {/* ─── KPI Cards ───────────────────────────────────── */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {jobsLoading || !kpis ? (
          Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)
        ) : (
          <>
            <KpiCard
              label="Open Jobs Today"
              value={formatNumber(kpis.openToday)}
              trend={{ direction: kpis.openToday > 0 ? "up" : "neutral", value: "today" }}
            />
            <KpiCard
              label="In Production"
              value={formatNumber(kpis.inProduction)}
              trend={{ direction: kpis.inProduction > 0 ? "up" : "neutral", value: "active" }}
              sparkline={{ data: kpis.sparkData }}
            />
            <KpiCard
              label="Completed This Week"
              value={formatNumber(kpis.completedThisWeek)}
              trend={{ direction: "up", value: "7 days" }}
            />
            <KpiCard
              label="Avg Lead Time"
              value={String(kpis.avgLeadTime)}
              unit="days"
              trend={{
                direction: kpis.avgLeadTime > 5 ? "down" : "up",
                value: kpis.avgLeadTime > 5 ? "above target" : "on target",
              }}
            />
          </>
        )}
      </div>

      {/* ─── Jobs Table ──────────────────────────────────── */}
      <DataTable<JobResponse>
        data={filteredJobs}
        columns={columns}
        searchPlaceholder="Search jobs..."
        searchKey="jobNumber"
        loading={jobsLoading}
        selectable
        showAgentAction
        pageSize={15}
        onRowClick={(row) => router.push(`/production/jobs/${row.id}`)}
        filterSlots={
          <>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger size="sm" className="w-[150px]">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Statuses</SelectItem>
                {JOB_STATUSES.map((s) => (
                  <SelectItem key={s} value={s}>
                    {humanizeStatus(s)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select value={priorityFilter} onValueChange={setPriorityFilter}>
              <SelectTrigger size="sm" className="w-[130px]">
                <SelectValue placeholder="Priority" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Priorities</SelectItem>
                {PRIORITIES.map((p) => (
                  <SelectItem key={p} value={String(p)}>
                    Priority {p}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </>
        }
        primaryAction={{
          label: "View",
          icon: <Eye className="h-3 w-3" />,
          onClick: (row) => router.push(`/production/jobs/${row.id}`),
        }}
        rowActions={[
          {
            label: "Change Status",
            icon: <ArrowRightLeft className="h-3.5 w-3.5" />,
            onClick: (row) => setStatusTarget(row),
          },
          {
            label: "Delete",
            icon: <Trash2 className="h-3.5 w-3.5" />,
            variant: "destructive",
            onClick: (row) => {
              if (row.status === "DRAFT") {
                setDeleteTarget(row);
              }
            },
          },
        ]}
        bulkActions={[
          {
            label: "Delete Selected",
            icon: <Trash2 className="h-3.5 w-3.5" />,
            variant: "destructive",
            onClick: (ids) => {
              const allDraft = ids.every((id) => {
                const job = jobs?.find((j) => j.id === id);
                return job?.status === "DRAFT";
              });
              if (allDraft) {
                setBulkDeleteIds(ids);
              }
            },
          },
        ]}
        emptyState={{
          icon: <Factory className="h-8 w-8 text-muted-foreground/40" />,
          title: "No jobs found",
          description: "Create your first production job to get started.",
          action: (
            <Button variant="outline" size="sm" onClick={() => setCreateOpen(true)} className="mt-2 gap-1.5">
              <Plus className="h-3.5 w-3.5" />
              New Job
            </Button>
          ),
        }}
      />

      {/* ─── Create Job Dialog ───────────────────────────── */}
      <Dialog
        open={createOpen}
        onOpenChange={(v) => {
          if (!v) {
            setCreateOpen(false);
            form.reset({ jobNumber: `JOB-${Date.now()}`, title: "", customerId: "", priority: 3, quantity: 1, deadline: "", notes: "" });
          }
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Create New Job</DialogTitle>
            <DialogDescription>
              Fill in the details to create a new production job.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={form.handleSubmit(handleCreateJob as any)} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="jobNumber">Job Number</Label>
                <Input
                  id="jobNumber"
                  {...form.register("jobNumber")}
                  className="font-mono text-xs"
                />
                {form.formState.errors.jobNumber && (
                  <p className="text-xs text-destructive">{form.formState.errors.jobNumber.message}</p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="priority">Priority</Label>
                <Select
                  value={String(form.watch("priority"))}
                  onValueChange={(v) => form.setValue("priority", Number(v))}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {PRIORITIES.map((p) => (
                      <SelectItem key={p} value={String(p)}>
                        P{p} - {p === 1 ? "Low" : p === 2 ? "Normal" : p === 3 ? "Medium" : p === 4 ? "High" : "Critical"}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="title">Title</Label>
              <Input id="title" placeholder="Job title..." {...form.register("title")} />
              {form.formState.errors.title && (
                <p className="text-xs text-destructive">{form.formState.errors.title.message}</p>
              )}
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="customerId">Customer ID</Label>
                <Input id="customerId" placeholder="Optional" {...form.register("customerId")} className="font-mono text-xs" />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="quantity">Quantity</Label>
                <Input id="quantity" type="number" min={1} {...form.register("quantity")} className="font-mono" />
                {form.formState.errors.quantity && (
                  <p className="text-xs text-destructive">{form.formState.errors.quantity.message}</p>
                )}
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="deadline">Deadline</Label>
              <Input id="deadline" type="date" {...form.register("deadline")} className="font-mono text-xs" />
              {form.formState.errors.deadline && (
                <p className="text-xs text-destructive">{form.formState.errors.deadline.message}</p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="notes">Notes</Label>
              <Textarea id="notes" placeholder="Additional notes..." rows={3} {...form.register("notes")} />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" size="sm" onClick={() => setCreateOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" size="sm" disabled={mutating} className="gap-1.5">
                {mutating ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <Plus className="h-3.5 w-3.5" />}
                Create Job
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* ─── Change Status Dialog ────────────────────────── */}
      <Dialog
        open={!!statusTarget}
        onOpenChange={(v) => {
          if (!v) {
            setStatusTarget(null);
            setSelectedNewStatus("");
            setStatusReason("");
          }
        }}
      >
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Change Job Status</DialogTitle>
            <DialogDescription>
              {statusTarget && (
                <span>
                  Current status:{" "}
                  <span className="font-mono font-semibold">{humanizeStatus(statusTarget.status)}</span>
                </span>
              )}
            </DialogDescription>
          </DialogHeader>
          {statusTarget && (
            <div className="space-y-4">
              <div className="space-y-1.5">
                <Label>New Status</Label>
                <Select value={selectedNewStatus} onValueChange={(v) => setSelectedNewStatus(v as JobStatus)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select status..." />
                  </SelectTrigger>
                  <SelectContent>
                    {VALID_TRANSITIONS[statusTarget.status]?.map((s) => (
                      <SelectItem key={s} value={s}>
                        {humanizeStatus(s)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {VALID_TRANSITIONS[statusTarget.status]?.length === 0 && (
                  <p className="text-xs text-muted-foreground">No transitions available from this status.</p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label>Reason (optional)</Label>
                <Input
                  placeholder="Reason for status change..."
                  value={statusReason}
                  onChange={(e) => setStatusReason(e.target.value)}
                />
              </div>
              <DialogFooter>
                <Button variant="outline" size="sm" onClick={() => setStatusTarget(null)}>
                  Cancel
                </Button>
                <Button
                  size="sm"
                  disabled={!selectedNewStatus || mutating}
                  onClick={handleChangeStatus}
                >
                  Confirm
                </Button>
              </DialogFooter>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* ─── Delete Confirmation ─────────────────────────── */}
      <ConfirmationDialog
        open={!!deleteTarget}
        title="Delete Job"
        description={`Are you sure you want to delete job "${deleteTarget?.jobNumber}"? This action cannot be undone.`}
        variant="destructive"
        confirmLabel="Delete"
        onConfirm={handleDeleteJob}
        onCancel={() => setDeleteTarget(null)}
      />

      {/* ─── Bulk Delete Confirmation ────────────────────── */}
      <ConfirmationDialog
        open={!!bulkDeleteIds}
        title="Delete Selected Jobs"
        description={`Are you sure you want to delete ${bulkDeleteIds?.length ?? 0} selected jobs? Only DRAFT jobs can be deleted. This action cannot be undone.`}
        variant="destructive"
        confirmLabel="Delete All"
        onConfirm={handleBulkDelete}
        onCancel={() => setBulkDeleteIds(null)}
      />
    </div>
  );
}
