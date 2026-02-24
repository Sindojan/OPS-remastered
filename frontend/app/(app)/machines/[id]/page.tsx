"use client";

import { useState, useMemo, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  Cog,
  ArrowLeft,
  Wrench,
  AlertTriangle,
  Calendar,
  Clock,
  CheckCircle2,
  XCircle,
  Timer,
  Factory,
  Settings2,
  Plus,
  Shield,
  Activity,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import {
  DomainStatusBadge,
  getMachineStatusVariant,
  getSeverityVariant,
} from "@/components/shared/domain-status-badge";
import { SkeletonCard, SkeletonTable } from "@/components/shared/skeleton-variants";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
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
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import { Separator } from "@/components/ui/separator";

import {
  useMachine,
  useMaintenanceIntervals,
  useMaintenanceRecords,
  useMachineIncidents,
  useMachineMutations,
} from "@/hooks/api/use-machines";
import type {
  MachineStatus,
  MachineIncidentResponse,
  MaintenanceIntervalResponse,
  MaintenanceRecordResponse,
  MaintenanceType,
  SeverityLevel,
} from "@/types/api";
import { toast } from "sonner";
import {
  formatDate,
  formatDateTime,
  formatRelativeDate,
  daysUntil,
  humanizeStatus,
} from "@/lib/format";
import { cn } from "@/lib/utils";

// ─── Constants ──────────────────────────────────────────

const MACHINE_STATUSES: MachineStatus[] = [
  "AVAILABLE",
  "IN_USE",
  "MAINTENANCE",
  "BLOCKED",
  "DECOMMISSIONED",
];

const SEVERITY_LEVELS: SeverityLevel[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

// ─── Component ──────────────────────────────────────────

export default function MachineDetailPage() {
  const params = useParams();
  const router = useRouter();
  const machineId = params.id as string;

  const { data: machine, loading, error, refetch } = useMachine(machineId);
  const {
    data: intervals,
    loading: intervalsLoading,
    refetch: refetchIntervals,
  } = useMaintenanceIntervals(machineId);
  const {
    data: records,
    loading: recordsLoading,
    refetch: refetchRecords,
  } = useMaintenanceRecords(machineId);
  const {
    data: incidents,
    loading: incidentsLoading,
    refetch: refetchIncidents,
  } = useMachineIncidents(machineId);

  const mutations = useMachineMutations();

  // ─── Dialog States ─────────────────────────────────────

  const [statusDialogOpen, setStatusDialogOpen] = useState(false);
  const [newStatus, setNewStatus] = useState<MachineStatus>("AVAILABLE");

  const [editMode, setEditMode] = useState(false);
  const [editName, setEditName] = useState("");
  const [editType, setEditType] = useState("");
  const [editCapacity, setEditCapacity] = useState("");
  const [editManufacturer, setEditManufacturer] = useState("");
  const [editModel, setEditModel] = useState("");
  const [editSerial, setEditSerial] = useState("");

  const [intervalDialogOpen, setIntervalDialogOpen] = useState(false);
  const [intervalType, setIntervalType] = useState<MaintenanceType>("TIME_BASED");
  const [intervalDays, setIntervalDays] = useState("");
  const [intervalHours, setIntervalHours] = useState("");
  const [intervalDescription, setIntervalDescription] = useState("");
  const [intervalNextDue, setIntervalNextDue] = useState("");

  const [performDialogOpen, setPerformDialogOpen] = useState(false);
  const [performIntervalId, setPerformIntervalId] = useState<string | null>(null);
  const [performDuration, setPerformDuration] = useState("");
  const [performNotes, setPerformNotes] = useState("");

  const [incidentDialogOpen, setIncidentDialogOpen] = useState(false);
  const [incidentType, setIncidentType] = useState("");
  const [incidentDescription, setIncidentDescription] = useState("");
  const [incidentSeverity, setIncidentSeverity] = useState<SeverityLevel>("MEDIUM");

  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
  const [resolveTarget, setResolveTarget] = useState<MachineIncidentResponse | null>(null);
  const [resolveNotes, setResolveNotes] = useState("");

  // ─── Handlers ──────────────────────────────────────────

  const handleChangeStatus = useCallback(async () => {
    if (!machine) return;
    try {
      const result = await mutations.changeStatus(machine.id, newStatus);
      if (result) {
        toast.success("Machine status updated");
        setStatusDialogOpen(false);
        refetch();
      }
    } catch (err) {
      toast.error("Failed to change status", { description: err instanceof Error ? err.message : "Unknown error" });
    }
  }, [mutations, machine, newStatus, refetch]);

  const handleSaveEdit = useCallback(async () => {
    if (!machine) return;
    try {
      const result = await mutations.updateMachine(machine.id, {
        name: editName || machine.name,
        machineNumber: machine.machineNumber,
        type: editType || undefined,
        capacityPerHour: editCapacity ? Number(editCapacity) : undefined,
        manufacturer: editManufacturer || undefined,
        model: editModel || undefined,
        serialNumber: editSerial || undefined,
      });
      if (result) {
        toast.success("Machine updated");
        setEditMode(false);
        refetch();
      }
    } catch (err) {
      toast.error("Failed to update machine", { description: err instanceof Error ? err.message : "Unknown error" });
    }
  }, [
    mutations,
    machine,
    editName,
    editType,
    editCapacity,
    editManufacturer,
    editModel,
    editSerial,
    refetch,
  ]);

  const startEdit = useCallback(() => {
    if (!machine) return;
    setEditName(machine.name);
    setEditType(machine.type || "");
    setEditCapacity(machine.capacityPerHour != null ? String(machine.capacityPerHour) : "");
    setEditManufacturer(machine.manufacturer || "");
    setEditModel(machine.model || "");
    setEditSerial(machine.serialNumber || "");
    setEditMode(true);
  }, [machine]);

  const handleCreateInterval = useCallback(async () => {
    if (!machine) return;
    try {
      const result = await mutations.createInterval({
        machineId: machine.id,
        type: intervalType,
        intervalDays: intervalType === "TIME_BASED" && intervalDays ? Number(intervalDays) : undefined,
        intervalHours: intervalType === "HOURS_BASED" && intervalHours ? Number(intervalHours) : undefined,
        description: intervalDescription || undefined,
        nextDueAt: intervalNextDue || undefined,
      });
      if (result) {
        toast.success("Maintenance interval created");
        setIntervalDialogOpen(false);
        setIntervalDays("");
        setIntervalHours("");
        setIntervalDescription("");
        setIntervalNextDue("");
        refetchIntervals();
      }
    } catch (err) {
      toast.error("Failed to create interval", { description: err instanceof Error ? err.message : "Unknown error" });
    }
  }, [
    mutations,
    machine,
    intervalType,
    intervalDays,
    intervalHours,
    intervalDescription,
    intervalNextDue,
    refetchIntervals,
  ]);

  const handlePerformMaintenance = useCallback(async () => {
    if (!machine) return;
    try {
      const result = await mutations.performMaintenance({
        machineId: machine.id,
        intervalId: performIntervalId || undefined,
        performedBy: "current-user",
        durationMinutes: Number(performDuration) || 0,
        notes: performNotes || undefined,
      });
      if (result) {
        toast.success("Maintenance recorded");
        setPerformDialogOpen(false);
        setPerformIntervalId(null);
        setPerformDuration("");
        setPerformNotes("");
        refetchIntervals();
        refetchRecords();
      }
    } catch (err) {
      toast.error("Failed to record maintenance", { description: err instanceof Error ? err.message : "Unknown error" });
    }
  }, [
    mutations,
    machine,
    performIntervalId,
    performDuration,
    performNotes,
    refetchIntervals,
    refetchRecords,
  ]);

  const handleReportIncident = useCallback(async () => {
    if (!machine) return;
    try {
      const result = await mutations.reportIncident(machine.id, {
        type: incidentType,
        description: incidentDescription || undefined,
        severity: incidentSeverity,
      });
      if (result) {
        toast.success("Incident reported");
        setIncidentDialogOpen(false);
        setIncidentType("");
        setIncidentDescription("");
        setIncidentSeverity("MEDIUM");
        refetchIncidents();
      }
    } catch (err) {
      toast.error("Failed to report incident", { description: err instanceof Error ? err.message : "Unknown error" });
    }
  }, [
    mutations,
    machine,
    incidentType,
    incidentDescription,
    incidentSeverity,
    refetchIncidents,
  ]);

  const handleResolveIncident = useCallback(async () => {
    if (!machine || !resolveTarget) return;
    try {
      const result = await mutations.resolveIncident(
        machine.id,
        resolveTarget.id,
        resolveNotes || undefined
      );
      if (result) {
        toast.success("Incident resolved");
        setResolveDialogOpen(false);
        setResolveTarget(null);
        setResolveNotes("");
        refetchIncidents();
      }
    } catch (err) {
      toast.error("Failed to resolve incident", { description: err instanceof Error ? err.message : "Unknown error" });
    }
  }, [mutations, machine, resolveTarget, resolveNotes, refetchIncidents]);

  // ─── Derived Data ──────────────────────────────────────

  const openIncidents = useMemo(
    () => incidents?.filter((i) => !i.resolvedAt) ?? [],
    [incidents]
  );

  // ─── Interval Columns ─────────────────────────────────

  const intervalColumns: ColumnDef<MaintenanceIntervalResponse>[] = useMemo(
    () => [
      {
        id: "type",
        header: "Type",
        accessorKey: "type",
        cell: (row) => (
          <DomainStatusBadge variant={row.type === "TIME_BASED" ? "info" : "primary"}>
            {row.type === "TIME_BASED" ? "Time Based" : "Hours Based"}
          </DomainStatusBadge>
        ),
      },
      {
        id: "interval",
        header: "Interval",
        cell: (row) => (
          <span className="font-mono text-xs">
            {row.intervalDays != null
              ? `${row.intervalDays} days`
              : row.intervalHours != null
                ? `${row.intervalHours} hours`
                : "–"}
          </span>
        ),
      },
      {
        id: "lastPerformed",
        header: "Last Performed",
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {formatDate(row.lastPerformedAt)}
          </span>
        ),
      },
      {
        id: "nextDue",
        header: "Next Due",
        cell: (row) => {
          const days = daysUntil(row.nextDueAt);
          const isOverdue = days !== null && days < 0;
          const isSoon = days !== null && days >= 0 && days <= 7;
          return (
            <span
              className={cn(
                "font-mono text-xs font-medium",
                isOverdue && "text-red-500",
                isSoon && !isOverdue && "text-amber-500",
                !isOverdue && !isSoon && "text-muted-foreground"
              )}
            >
              {row.nextDueAt ? formatRelativeDate(row.nextDueAt) : "–"}
            </span>
          );
        },
      },
      {
        id: "description",
        header: "Description",
        accessorKey: "description",
        cell: (row) => (
          <span className="text-xs text-muted-foreground">
            {row.description || "–"}
          </span>
        ),
      },
    ],
    []
  );

  // ─── Incident Columns ─────────────────────────────────

  const incidentColumns: ColumnDef<MachineIncidentResponse>[] = useMemo(
    () => [
      {
        id: "reportedAt",
        header: "Date",
        accessorKey: "reportedAt",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs">
            {formatDateTime(row.reportedAt)}
          </span>
        ),
      },
      {
        id: "type",
        header: "Type",
        accessorKey: "type",
        sortable: true,
      },
      {
        id: "severity",
        header: "Severity",
        accessorKey: "severity",
        sortable: true,
        cell: (row) => (
          <DomainStatusBadge variant={getSeverityVariant(row.severity)}>
            {row.severity}
          </DomainStatusBadge>
        ),
      },
      {
        id: "status",
        header: "Status",
        cell: (row) =>
          row.resolvedAt ? (
            <DomainStatusBadge variant="success">Resolved</DomainStatusBadge>
          ) : (
            <DomainStatusBadge variant="error" pulse>
              Open
            </DomainStatusBadge>
          ),
      },
      {
        id: "reportedBy",
        header: "Reported By",
        accessorKey: "reportedBy",
        cell: (row) => (
          <span className="text-xs text-muted-foreground">
            {row.reportedBy || "–"}
          </span>
        ),
      },
      {
        id: "resolvedAt",
        header: "Resolved At",
        accessorKey: "resolvedAt",
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {formatDateTime(row.resolvedAt)}
          </span>
        ),
      },
    ],
    []
  );

  // ─── Error / Loading ──────────────────────────────────

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

  if (loading || !machine) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-3">
          <SkeletonCard className="w-full" />
        </div>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
        <SkeletonTable rows={5} columns={5} />
      </div>
    );
  }

  // ─── Render ────────────────────────────────────────────

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title={`${machine.machineNumber} – ${machine.name}`}
        breadcrumb={["Machines", machine.machineNumber]}
        actions={
          <div className="flex items-center gap-2">
            <DomainStatusBadge
              variant={getMachineStatusVariant(machine.status)}
              pulse={machine.status === "MAINTENANCE"}
            >
              {humanizeStatus(machine.status)}
            </DomainStatusBadge>
            <Button
              variant="outline"
              size="sm"
              onClick={() => router.push("/machines")}
            >
              <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
              Back
            </Button>
          </div>
        }
      />

      {/* Tabs */}
      <Tabs defaultValue="overview" className="space-y-4">
        <TabsList>
          <TabsTrigger value="overview" className="gap-1.5">
            <Cog className="h-3.5 w-3.5" />
            Overview
          </TabsTrigger>
          <TabsTrigger value="maintenance" className="gap-1.5">
            <Wrench className="h-3.5 w-3.5" />
            Maintenance
          </TabsTrigger>
          <TabsTrigger value="incidents" className="gap-1.5">
            <AlertTriangle className="h-3.5 w-3.5" />
            Incidents
            {openIncidents.length > 0 && (
              <span className="ml-1 inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-bold text-destructive-foreground">
                {openIncidents.length}
              </span>
            )}
          </TabsTrigger>
          <TabsTrigger value="availability" className="gap-1.5">
            <Calendar className="h-3.5 w-3.5" />
            Availability
          </TabsTrigger>
        </TabsList>

        {/* ═══════════════════════ Overview Tab ═══════════════════════ */}
        <TabsContent value="overview" className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Machine Details
            </h3>
            <div className="flex gap-2">
              {!editMode ? (
                <>
                  <Button variant="outline" size="sm" onClick={startEdit}>
                    Edit
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setNewStatus(machine.status);
                      setStatusDialogOpen(true);
                    }}
                  >
                    <Settings2 className="mr-1.5 h-3.5 w-3.5" />
                    Change Status
                  </Button>
                </>
              ) : (
                <>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setEditMode(false)}
                  >
                    Cancel
                  </Button>
                  <Button
                    size="sm"
                    onClick={handleSaveEdit}
                    disabled={mutations.loading}
                  >
                    {mutations.loading ? "Saving..." : "Save Changes"}
                  </Button>
                </>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {/* Type */}
            <Card className="p-4">
              <div className="flex items-center gap-2 text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                <Factory className="h-3.5 w-3.5" />
                Type
              </div>
              {editMode ? (
                <Input
                  className="mt-2 text-sm"
                  value={editType}
                  onChange={(e) => setEditType(e.target.value)}
                />
              ) : (
                <p className="mt-1.5 font-mono text-sm font-semibold">
                  {machine.type || "–"}
                </p>
              )}
            </Card>

            {/* Station */}
            <Card className="p-4">
              <div className="flex items-center gap-2 text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                <Activity className="h-3.5 w-3.5" />
                Station
              </div>
              <p className="mt-1.5 font-mono text-xs font-semibold">
                {machine.stationId
                  ? machine.stationId.slice(0, 8) + "..."
                  : "–"}
              </p>
            </Card>

            {/* Capacity */}
            <Card className="p-4">
              <div className="flex items-center gap-2 text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                <Timer className="h-3.5 w-3.5" />
                Capacity / Hour
              </div>
              {editMode ? (
                <Input
                  type="number"
                  className="mt-2 font-mono text-sm"
                  value={editCapacity}
                  onChange={(e) => setEditCapacity(e.target.value)}
                />
              ) : (
                <p className="mt-1.5 font-mono text-2xl font-bold">
                  {machine.capacityPerHour != null
                    ? machine.capacityPerHour
                    : "–"}
                  {machine.capacityPerHour != null && (
                    <span className="ml-1 text-xs font-normal text-muted-foreground">
                      units/h
                    </span>
                  )}
                </p>
              )}
            </Card>

            {/* Manufacturer */}
            <Card className="p-4">
              <div className="flex items-center gap-2 text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                <Shield className="h-3.5 w-3.5" />
                Manufacturer
              </div>
              {editMode ? (
                <Input
                  className="mt-2 text-sm"
                  value={editManufacturer}
                  onChange={(e) => setEditManufacturer(e.target.value)}
                />
              ) : (
                <p className="mt-1.5 text-sm font-semibold">
                  {machine.manufacturer || "–"}
                </p>
              )}
            </Card>

            {/* Model */}
            <Card className="p-4">
              <div className="flex items-center gap-2 text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                <Cog className="h-3.5 w-3.5" />
                Model
              </div>
              {editMode ? (
                <Input
                  className="mt-2 text-sm"
                  value={editModel}
                  onChange={(e) => setEditModel(e.target.value)}
                />
              ) : (
                <p className="mt-1.5 text-sm font-semibold">
                  {machine.model || "–"}
                </p>
              )}
            </Card>

            {/* Serial Number */}
            <Card className="p-4">
              <div className="flex items-center gap-2 text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                <Settings2 className="h-3.5 w-3.5" />
                Serial Number
              </div>
              {editMode ? (
                <Input
                  className="mt-2 font-mono text-xs"
                  value={editSerial}
                  onChange={(e) => setEditSerial(e.target.value)}
                />
              ) : (
                <p className="mt-1.5 font-mono text-xs font-semibold">
                  {machine.serialNumber || "–"}
                </p>
              )}
            </Card>

            {/* Purchase Date */}
            <Card className="p-4">
              <div className="flex items-center gap-2 text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                <Calendar className="h-3.5 w-3.5" />
                Purchase Date
              </div>
              <p className="mt-1.5 font-mono text-sm font-semibold">
                {formatDate(machine.purchaseDate)}
              </p>
            </Card>

            {/* Name (editable) */}
            {editMode && (
              <Card className="p-4">
                <div className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                  Machine Name
                </div>
                <Input
                  className="mt-2 text-sm"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                />
              </Card>
            )}
          </div>
        </TabsContent>

        {/* ═══════════════════════ Maintenance Tab ═══════════════════════ */}
        <TabsContent value="maintenance" className="space-y-6">
          {/* Intervals Section */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Maintenance Intervals
              </h3>
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5"
                onClick={() => setIntervalDialogOpen(true)}
              >
                <Plus className="h-3.5 w-3.5" />
                New Interval
              </Button>
            </div>

            <DataTable<MaintenanceIntervalResponse>
              data={intervals ?? []}
              columns={intervalColumns}
              loading={intervalsLoading}
              pageSize={10}
              rowActions={[
                {
                  label: "Perform Maintenance",
                  icon: <Wrench className="h-3.5 w-3.5" />,
                  onClick: (row) => {
                    setPerformIntervalId(row.id);
                    setPerformDialogOpen(true);
                  },
                },
              ]}
              emptyState={{
                icon: <Wrench className="h-8 w-8 text-muted-foreground/40" />,
                title: "No maintenance intervals",
                description:
                  "Define maintenance intervals to track recurring service.",
              }}
            />
          </div>

          <Separator />

          {/* Maintenance History */}
          <div className="space-y-3">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Maintenance History
            </h3>

            {recordsLoading ? (
              <SkeletonTable rows={4} columns={4} />
            ) : !records || records.length === 0 ? (
              <Card className="flex flex-col items-center justify-center gap-2 p-8 text-center">
                <Clock className="h-6 w-6 text-muted-foreground/40" />
                <p className="text-sm text-muted-foreground">
                  No maintenance records yet.
                </p>
              </Card>
            ) : (
              <div className="space-y-3">
                {records.map((record) => (
                  <MaintenanceRecordCard key={record.id} record={record} />
                ))}
              </div>
            )}
          </div>
        </TabsContent>

        {/* ═══════════════════════ Incidents Tab ═══════════════════════ */}
        <TabsContent value="incidents" className="space-y-6">
          {/* Open Incidents */}
          {openIncidents.length > 0 && (
            <div className="space-y-3">
              <h3 className="text-sm font-semibold uppercase tracking-wider text-destructive">
                Open Incidents ({openIncidents.length})
              </h3>
              <div className="space-y-2">
                {openIncidents.map((incident) => (
                  <Card
                    key={incident.id}
                    className="border-destructive/30 bg-destructive/5 p-4"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1 space-y-1">
                        <div className="flex items-center gap-2">
                          <AlertTriangle className="h-4 w-4 text-destructive" />
                          <span className="text-sm font-semibold">
                            {incident.type}
                          </span>
                          <DomainStatusBadge
                            variant={getSeverityVariant(incident.severity)}
                          >
                            {incident.severity}
                          </DomainStatusBadge>
                        </div>
                        {incident.description && (
                          <p className="text-xs text-muted-foreground">
                            {incident.description}
                          </p>
                        )}
                        <p className="font-mono text-[11px] text-muted-foreground">
                          Reported {formatDateTime(incident.reportedAt)}
                          {incident.reportedBy
                            ? ` by ${incident.reportedBy}`
                            : ""}
                        </p>
                      </div>
                      <Button
                        variant="outline"
                        size="sm"
                        className="gap-1.5 border-destructive/30 text-destructive hover:bg-destructive/10"
                        onClick={() => {
                          setResolveTarget(incident);
                          setResolveDialogOpen(true);
                        }}
                      >
                        <CheckCircle2 className="h-3.5 w-3.5" />
                        Resolve
                      </Button>
                    </div>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {/* All Incidents Table */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                All Incidents
              </h3>
              <Button
                variant="destructive"
                size="sm"
                className="gap-1.5"
                onClick={() => setIncidentDialogOpen(true)}
              >
                <AlertTriangle className="h-3.5 w-3.5" />
                Report Incident
              </Button>
            </div>

            <DataTable<MachineIncidentResponse>
              data={incidents ?? []}
              columns={incidentColumns}
              loading={incidentsLoading}
              pageSize={15}
              rowActions={[
                {
                  label: "Resolve",
                  icon: <CheckCircle2 className="h-3.5 w-3.5" />,
                  onClick: (row) => {
                    if (!row.resolvedAt) {
                      setResolveTarget(row);
                      setResolveDialogOpen(true);
                    }
                  },
                },
              ]}
              emptyState={{
                icon: (
                  <CheckCircle2 className="h-8 w-8 text-muted-foreground/40" />
                ),
                title: "No incidents recorded",
                description: "This machine has a clean incident record.",
              }}
            />
          </div>
        </TabsContent>

        {/* ═══════════════════════ Availability Tab ═══════════════════════ */}
        <TabsContent value="availability">
          <Card className="flex flex-col items-center justify-center gap-4 p-12 text-center">
            <Calendar className="h-10 w-10 text-muted-foreground/30" />
            <div>
              <p className="text-sm font-semibold text-foreground/70">
                Availability schedule coming soon
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                Track machine availability windows, shift assignments, and planned downtime.
              </p>
            </div>
          </Card>
        </TabsContent>
      </Tabs>

      {/* ═══════════════════════ Dialogs ═══════════════════════ */}

      {/* Change Status */}
      <Dialog open={statusDialogOpen} onOpenChange={setStatusDialogOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Change Machine Status</DialogTitle>
            <DialogDescription>
              Update status for {machine.machineNumber} – {machine.name}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label>Current Status</Label>
              <div>
                <DomainStatusBadge
                  variant={getMachineStatusVariant(machine.status)}
                >
                  {humanizeStatus(machine.status)}
                </DomainStatusBadge>
              </div>
            </div>
            <div className="space-y-1.5">
              <Label>New Status</Label>
              <Select
                value={newStatus}
                onValueChange={(v) => setNewStatus(v as MachineStatus)}
              >
                <SelectTrigger className="text-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {MACHINE_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {humanizeStatus(s)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setStatusDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleChangeStatus}
              disabled={mutations.loading}
            >
              {mutations.loading ? "Updating..." : "Update Status"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* New Maintenance Interval */}
      <Dialog open={intervalDialogOpen} onOpenChange={setIntervalDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Wrench className="h-4 w-4 text-primary" />
              New Maintenance Interval
            </DialogTitle>
            <DialogDescription>
              Define a recurring maintenance schedule for this machine.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Interval Type</Label>
              <Select
                value={intervalType}
                onValueChange={(v) => setIntervalType(v as MaintenanceType)}
              >
                <SelectTrigger className="text-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="TIME_BASED">Time Based (Days)</SelectItem>
                  <SelectItem value="HOURS_BASED">
                    Hours Based (Operating Hours)
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            {intervalType === "TIME_BASED" ? (
              <div className="space-y-1.5">
                <Label>Interval (Days)</Label>
                <Input
                  type="number"
                  placeholder="30"
                  className="font-mono"
                  value={intervalDays}
                  onChange={(e) => setIntervalDays(e.target.value)}
                />
              </div>
            ) : (
              <div className="space-y-1.5">
                <Label>Interval (Operating Hours)</Label>
                <Input
                  type="number"
                  placeholder="500"
                  className="font-mono"
                  value={intervalHours}
                  onChange={(e) => setIntervalHours(e.target.value)}
                />
              </div>
            )}

            <div className="space-y-1.5">
              <Label>Description</Label>
              <Input
                placeholder="Oil change, filter replacement, etc."
                value={intervalDescription}
                onChange={(e) => setIntervalDescription(e.target.value)}
              />
            </div>

            <div className="space-y-1.5">
              <Label>Next Due Date</Label>
              <Input
                type="datetime-local"
                value={intervalNextDue}
                onChange={(e) => setIntervalNextDue(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIntervalDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleCreateInterval}
              disabled={mutations.loading}
            >
              {mutations.loading ? "Creating..." : "Create Interval"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Perform Maintenance */}
      <Dialog open={performDialogOpen} onOpenChange={setPerformDialogOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Wrench className="h-4 w-4 text-primary" />
              Perform Maintenance
            </DialogTitle>
            <DialogDescription>
              Record a maintenance activity for this machine.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Duration (minutes)</Label>
              <Input
                type="number"
                placeholder="60"
                className="font-mono"
                value={performDuration}
                onChange={(e) => setPerformDuration(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label>Notes</Label>
              <textarea
                className="flex min-h-[80px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                placeholder="What was done during maintenance..."
                value={performNotes}
                onChange={(e) => setPerformNotes(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPerformDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handlePerformMaintenance}
              disabled={mutations.loading}
            >
              {mutations.loading ? "Recording..." : "Record Maintenance"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Report Incident */}
      <Dialog open={incidentDialogOpen} onOpenChange={setIncidentDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-destructive">
              <AlertTriangle className="h-4 w-4" />
              Report Incident
            </DialogTitle>
            <DialogDescription>
              Report a new incident for {machine.machineNumber} –{" "}
              {machine.name}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Incident Type *</Label>
              <Input
                placeholder="Mechanical Failure, Electrical, etc."
                value={incidentType}
                onChange={(e) => setIncidentType(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label>Description</Label>
              <textarea
                className="flex min-h-[80px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                placeholder="Describe what happened..."
                value={incidentDescription}
                onChange={(e) => setIncidentDescription(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label>Severity</Label>
              <Select
                value={incidentSeverity}
                onValueChange={(v) => setIncidentSeverity(v as SeverityLevel)}
              >
                <SelectTrigger className="text-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SEVERITY_LEVELS.map((s) => (
                    <SelectItem key={s} value={s}>
                      {s}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIncidentDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              size="sm"
              onClick={handleReportIncident}
              disabled={mutations.loading || !incidentType.trim()}
            >
              {mutations.loading ? "Reporting..." : "Report Incident"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Resolve Incident */}
      <Dialog open={resolveDialogOpen} onOpenChange={setResolveDialogOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-emerald-500" />
              Resolve Incident
            </DialogTitle>
            <DialogDescription>
              {resolveTarget
                ? `Resolve incident: ${resolveTarget.type}`
                : ""}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Resolution Notes</Label>
              <textarea
                className="flex min-h-[80px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                placeholder="How was the incident resolved..."
                value={resolveNotes}
                onChange={(e) => setResolveNotes(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setResolveDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleResolveIncident}
              disabled={mutations.loading}
            >
              {mutations.loading ? "Resolving..." : "Resolve Incident"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ─── Sub-components ─────────────────────────────────────

function MaintenanceRecordCard({
  record,
}: {
  record: MaintenanceRecordResponse;
}) {
  return (
    <Card className="p-4">
      <div className="flex items-start gap-3">
        <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary/10">
          <Wrench className="h-4 w-4 text-primary" />
        </div>
        <div className="flex-1 space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold">
              Maintenance Performed
            </span>
            <DomainStatusBadge
              variant={
                record.status === "DONE"
                  ? "success"
                  : record.status === "IN_PROGRESS"
                    ? "primary"
                    : record.status === "PLANNED"
                      ? "info"
                      : "neutral"
              }
            >
              {humanizeStatus(record.status)}
            </DomainStatusBadge>
          </div>
          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
            <span className="inline-flex items-center gap-1">
              <Clock className="h-3 w-3" />
              {formatDateTime(record.performedAt)}
            </span>
            {record.durationMinutes != null && (
              <span className="inline-flex items-center gap-1 font-mono">
                <Timer className="h-3 w-3" />
                {record.durationMinutes} min
              </span>
            )}
            {record.performedBy && (
              <span>by {record.performedBy}</span>
            )}
          </div>
          {record.notes && (
            <p className="mt-1 text-xs text-muted-foreground/80">
              {record.notes}
            </p>
          )}
        </div>
      </div>
    </Card>
  );
}
