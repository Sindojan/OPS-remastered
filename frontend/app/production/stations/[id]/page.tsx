"use client";

import { useState, useMemo, useCallback } from "react";
import { useRouter, useParams } from "next/navigation";
import { useStation, useJobs } from "@/hooks/api/use-jobs";
import type { JobResponse } from "@/types/api";
import { formatDate, formatDateTime, daysUntil, formatNumber, humanizeStatus } from "@/lib/format";
import { PageHeader } from "@/components/shared/page-header";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import { SkeletonCard, SkeletonTable } from "@/components/shared/skeleton-variants";
import {
  DomainStatusBadge,
  getJobStatusVariant,
  getPriorityVariant,
} from "@/components/shared/domain-status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  ArrowLeft,
  Pencil,
  Save,
  X,
  Eye,
  Factory,
  Hash,
  Info,
  Gauge,
  AlertCircle,
  RefreshCw,
} from "lucide-react";

const STATION_STATUSES = ["ACTIVE", "INACTIVE", "MAINTENANCE"];

export default function StationDetailPage() {
  const router = useRouter();
  const params = useParams();
  const stationId = params.id as string;

  const { data: station, loading, error, refetch } = useStation(stationId);
  const { data: allJobs, loading: jobsLoading } = useJobs();

  const [editing, setEditing] = useState(false);
  const [editCapacity, setEditCapacity] = useState<number>(0);
  const [editStatus, setEditStatus] = useState<string>("");

  // Jobs assigned to this station
  const stationJobs = useMemo(() => {
    if (!allJobs || !stationId) return [];
    return allJobs.filter((j) => j.assignedStationId === stationId);
  }, [allJobs, stationId]);

  // Start editing
  const startEdit = useCallback(() => {
    if (!station) return;
    setEditCapacity(station.capacityPerShift ?? 0);
    setEditStatus(station.status);
    setEditing(true);
  }, [station]);

  // Columns for station jobs
  const columns: ColumnDef<JobResponse>[] = useMemo(
    () => [
      {
        id: "jobNumber",
        header: "Job #",
        accessorKey: "jobNumber",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs font-semibold text-foreground">{row.jobNumber}</span>
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
        id: "status",
        header: "Status",
        accessorKey: "status",
        sortable: true,
        cell: (row) => (
          <DomainStatusBadge variant={getJobStatusVariant(row.status)} pulse={row.status === "IN_PRODUCTION"}>
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
          <span className="font-mono text-xs text-muted-foreground">{formatDate(row.createdAt)}</span>
        ),
      },
    ],
    []
  );

  // ─── Loading ──────────────────────────────────────────
  if (loading) {
    return (
      <div className="space-y-6">
        <SkeletonCard className="h-12 w-64" />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
        <SkeletonTable rows={5} columns={5} />
      </div>
    );
  }

  // ─── Error ────────────────────────────────────────────
  if (error || !station) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <AlertCircle className="h-10 w-10 text-destructive/60" />
        <p className="text-sm text-muted-foreground">{error ?? "Station not found"}</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => router.push("/production/planner")} className="gap-2">
            <ArrowLeft className="h-3.5 w-3.5" />
            Back
          </Button>
          <Button variant="outline" size="sm" onClick={refetch} className="gap-2">
            <RefreshCw className="h-3.5 w-3.5" />
            Retry
          </Button>
        </div>
      </div>
    );
  }

  const activeJobs = stationJobs.filter((j) => j.status !== "COMPLETED" && j.status !== "CANCELLED");
  const capacity = station.capacityPerShift ?? 10;
  const utilization = Math.min(Math.round((activeJobs.length / capacity) * 100), 100);

  return (
    <div className="space-y-6">
      {/* ─── Header ──────────────────────────────────────── */}
      <PageHeader
        title={station.name}
        description={station.description ?? undefined}
        breadcrumb={["Production", "Stations", station.name]}
        actions={
          <div className="flex items-center gap-2">
            <DomainStatusBadge
              variant={
                station.status === "ACTIVE"
                  ? "success"
                  : station.status === "MAINTENANCE"
                    ? "warning"
                    : "neutral"
              }
            >
              {station.status}
            </DomainStatusBadge>
            <Button variant="outline" size="sm" onClick={() => router.push("/production/planner")} className="gap-1.5">
              <ArrowLeft className="h-3.5 w-3.5" />
              Back
            </Button>
            {!editing && (
              <Button variant="outline" size="sm" onClick={startEdit} className="gap-1.5">
                <Pencil className="h-3.5 w-3.5" />
                Edit
              </Button>
            )}
          </div>
        }
      />

      {/* ─── Edit Form or Key Data ───────────────────────── */}
      {editing ? (
        <Card className="p-4 space-y-4">
          <p className="text-sm font-semibold">Edit Station</p>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>Capacity Per Shift</Label>
              <Input
                type="number"
                min={1}
                value={editCapacity}
                onChange={(e) => setEditCapacity(Number(e.target.value))}
                className="font-mono"
              />
            </div>
            <div className="space-y-1.5">
              <Label>Status</Label>
              <Select value={editStatus} onValueChange={setEditStatus}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {STATION_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {s}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="flex gap-2">
            <Button size="sm" onClick={() => setEditing(false)} className="gap-1.5">
              <Save className="h-3.5 w-3.5" />
              Save
            </Button>
            <Button variant="outline" size="sm" onClick={() => setEditing(false)} className="gap-1.5">
              <X className="h-3.5 w-3.5" />
              Cancel
            </Button>
          </div>
          <p className="text-[10px] text-muted-foreground">
            Note: Station update API integration will be connected when the endpoint is available.
          </p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Card className="p-4">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Factory className="h-4 w-4" />
              <p className="text-[11px] font-medium uppercase tracking-wider">Name</p>
            </div>
            <p className="mt-2 text-sm font-semibold">{station.name}</p>
          </Card>
          <Card className="p-4">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Info className="h-4 w-4" />
              <p className="text-[11px] font-medium uppercase tracking-wider">Description</p>
            </div>
            <p className="mt-2 text-sm text-foreground/80">{station.description ?? "No description"}</p>
          </Card>
          <Card className="p-4">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Gauge className="h-4 w-4" />
              <p className="text-[11px] font-medium uppercase tracking-wider">Capacity / Shift</p>
            </div>
            <p className="mt-2 font-mono text-sm font-semibold">{formatNumber(station.capacityPerShift ?? 0)}</p>
          </Card>
          <Card className="p-4">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Hash className="h-4 w-4" />
              <p className="text-[11px] font-medium uppercase tracking-wider">Utilization</p>
            </div>
            <div className="mt-2 space-y-1.5">
              <p className="font-mono text-sm font-semibold">
                {activeJobs.length}/{capacity} ({utilization}%)
              </p>
              <div className="relative h-2 w-full overflow-hidden rounded-full bg-muted">
                <div
                  className={`h-full rounded-full transition-all ${
                    utilization > 90
                      ? "bg-red-500"
                      : utilization > 70
                        ? "bg-amber-500"
                        : "bg-emerald-500"
                  }`}
                  style={{ width: `${utilization}%` }}
                />
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* ─── Jobs Table ──────────────────────────────────── */}
      <div>
        <h2 className="mb-3 text-sm font-semibold">Assigned Jobs</h2>
        <DataTable<JobResponse>
          data={stationJobs}
          columns={columns}
          searchPlaceholder="Search jobs..."
          searchKey="jobNumber"
          loading={jobsLoading}
          pageSize={10}
          onRowClick={(row) => router.push(`/production/jobs/${row.id}`)}
          primaryAction={{
            label: "View",
            icon: <Eye className="h-3 w-3" />,
            onClick: (row) => router.push(`/production/jobs/${row.id}`),
          }}
          emptyState={{
            icon: <Factory className="h-8 w-8 text-muted-foreground/40" />,
            title: "No jobs assigned",
            description: "Assign jobs to this station from the job detail page.",
          }}
        />
      </div>
    </div>
  );
}
