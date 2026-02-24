"use client";

import { useState, useMemo, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Plus,
  Cog,
  Eye,
  AlertTriangle,
  Settings2,
  Factory,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/shared/kpi-card";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import {
  DomainStatusBadge,
  getMachineStatusVariant,
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
import { useMachines, useMachineMutations } from "@/hooks/api/use-machines";
import type { MachineResponse, MachineStatus } from "@/types/api";
import { formatDate, humanizeStatus } from "@/lib/format";

// ─── Schemas ────────────────────────────────────────────

const createMachineSchema = z.object({
  name: z.string().min(1, "Name ist erforderlich"),
  machineNumber: z.string().min(1, "Maschinennummer ist erforderlich"),
  type: z.string().optional(),
  stationId: z.string().optional(),
  capacityPerHour: z.string().optional(),
  manufacturer: z.string().optional(),
  model: z.string().optional(),
  serialNumber: z.string().optional(),
  purchaseDate: z.string().optional(),
});

type CreateMachineFormData = z.infer<typeof createMachineSchema>;

const MACHINE_STATUSES: MachineStatus[] = [
  "AVAILABLE",
  "IN_USE",
  "MAINTENANCE",
  "BLOCKED",
  "DECOMMISSIONED",
];

// ─── Component ──────────────────────────────────────────

export default function MachinesPage() {
  const router = useRouter();
  const { data: machines, loading, error, refetch } = useMachines();
  const mutations = useMachineMutations();

  const [createOpen, setCreateOpen] = useState(false);
  const [statusDialogOpen, setStatusDialogOpen] = useState(false);
  const [statusTarget, setStatusTarget] = useState<MachineResponse | null>(null);
  const [newStatus, setNewStatus] = useState<MachineStatus>("AVAILABLE");
  const [incidentDialogOpen, setIncidentDialogOpen] = useState(false);
  const [incidentTarget, setIncidentTarget] = useState<MachineResponse | null>(null);
  const [incidentType, setIncidentType] = useState("");
  const [incidentDescription, setIncidentDescription] = useState("");
  const [incidentSeverity, setIncidentSeverity] = useState<string>("MEDIUM");

  // Filters
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [typeFilter, setTypeFilter] = useState("");

  const form = useForm<CreateMachineFormData>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(createMachineSchema) as any,
    defaultValues: {
      name: "",
      machineNumber: `M-${Date.now().toString(36).toUpperCase().slice(-6)}`,
      type: "",
      stationId: "",
      capacityPerHour: "",
      manufacturer: "",
      model: "",
      serialNumber: "",
      purchaseDate: "",
    },
  });

  // ─── KPI Computations ──────────────────────────────────

  const kpis = useMemo(() => {
    if (!machines) return null;
    const total = machines.length;
    const available = machines.filter((m) => m.status === "AVAILABLE").length;
    const maintenanceBlocked = machines.filter(
      (m) => m.status === "MAINTENANCE" || m.status === "BLOCKED"
    ).length;
    return { total, available, maintenanceBlocked };
  }, [machines]);

  // ─── Filtered Data ─────────────────────────────────────

  const filteredMachines = useMemo(() => {
    if (!machines) return [];
    let result = machines;
    if (statusFilter !== "ALL") {
      result = result.filter((m) => m.status === statusFilter);
    }
    if (typeFilter.trim()) {
      const term = typeFilter.toLowerCase();
      result = result.filter((m) =>
        m.type?.toLowerCase().includes(term)
      );
    }
    return result;
  }, [machines, statusFilter, typeFilter]);

  // ─── Table Columns ─────────────────────────────────────

  const columns: ColumnDef<MachineResponse>[] = useMemo(
    () => [
      {
        id: "machineNumber",
        header: "Maschinen-Nr.",
        accessorKey: "machineNumber",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs font-semibold text-foreground">
            {row.machineNumber}
          </span>
        ),
      },
      {
        id: "name",
        header: "Name",
        accessorKey: "name",
        sortable: true,
      },
      {
        id: "type",
        header: "Typ",
        accessorKey: "type",
        sortable: true,
        cell: (row) => (
          <span className="text-muted-foreground">{row.type || "–"}</span>
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
            variant={getMachineStatusVariant(row.status)}
            pulse={row.status === "MAINTENANCE"}
          >
            {humanizeStatus(row.status)}
          </DomainStatusBadge>
        ),
      },
      {
        id: "capacityPerHour",
        header: "Kap./Stunde",
        accessorKey: "capacityPerHour",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs">
            {row.capacityPerHour != null ? row.capacityPerHour : "–"}
          </span>
        ),
      },
      {
        id: "manufacturer",
        header: "Hersteller",
        accessorKey: "manufacturer",
        sortable: true,
        cell: (row) => (
          <span className="text-muted-foreground">
            {row.manufacturer || "–"}
          </span>
        ),
      },
    ],
    []
  );

  // ─── Handlers ──────────────────────────────────────────

  const onCreateSubmit = useCallback(
    async (data: CreateMachineFormData) => {
      try {
        const payload = {
          name: data.name,
          machineNumber: data.machineNumber,
          type: data.type || undefined,
          stationId: data.stationId || undefined,
          capacityPerHour:
            data.capacityPerHour && data.capacityPerHour.trim()
              ? Number(data.capacityPerHour)
              : undefined,
          manufacturer: data.manufacturer || undefined,
          model: data.model || undefined,
          serialNumber: data.serialNumber || undefined,
          purchaseDate: data.purchaseDate || undefined,
        };
        const result = await mutations.createMachine(payload);
        if (result) {
          toast.success("Maschine erfolgreich erstellt");
          setCreateOpen(false);
          form.reset({
            name: "",
            machineNumber: `M-${Date.now().toString(36).toUpperCase().slice(-6)}`,
            type: "",
            stationId: "",
            capacityPerHour: "",
            manufacturer: "",
            model: "",
            serialNumber: "",
            purchaseDate: "",
          });
          refetch();
        }
      } catch (err) {
        toast.error("Maschine konnte nicht erstellt werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
      }
    },
    [mutations, form, refetch]
  );

  const handleChangeStatus = useCallback(async () => {
    if (!statusTarget) return;
    try {
      const result = await mutations.changeStatus(statusTarget.id, newStatus);
      if (result) {
        toast.success("Maschinenstatus aktualisiert");
        setStatusDialogOpen(false);
        setStatusTarget(null);
        refetch();
      }
    } catch (err) {
      toast.error("Status konnte nicht geändert werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  }, [mutations, statusTarget, newStatus, refetch]);

  const handleReportIncident = useCallback(async () => {
    if (!incidentTarget) return;
    try {
      const result = await mutations.reportIncident(incidentTarget.id, {
        type: incidentType,
        description: incidentDescription || undefined,
        severity: incidentSeverity as "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
      });
      if (result) {
        toast.success("Störung gemeldet");
        setIncidentDialogOpen(false);
        setIncidentTarget(null);
        setIncidentType("");
        setIncidentDescription("");
        setIncidentSeverity("MEDIUM");
        refetch();
      }
    } catch (err) {
      toast.error("Störung konnte nicht gemeldet werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  }, [
    mutations,
    incidentTarget,
    incidentType,
    incidentDescription,
    incidentSeverity,
    refetch,
  ]);

  // ─── Error State ───────────────────────────────────────

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <AlertTriangle className="h-8 w-8 text-destructive" />
        <p className="text-sm text-muted-foreground">{error}</p>
        <Button variant="outline" size="sm" onClick={refetch}>
          Erneut versuchen
        </Button>
      </div>
    );
  }

  // ─── Render ────────────────────────────────────────────

  return (
    <div className="space-y-6">
      <PageHeader
        title="Maschinen"
        description="Maschinenflotte verwalten und überwachen"
        actions={
          <Button
            size="sm"
            className="gap-1.5"
            onClick={() => setCreateOpen(true)}
          >
            <Plus className="h-3.5 w-3.5" />
            Neue Maschine
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
              label="Maschinen gesamt"
              value={String(kpis?.total ?? 0)}
            />
            <KpiCard
              label="Verfügbar"
              value={String(kpis?.available ?? 0)}
              trend={
                kpis && kpis.total > 0
                  ? {
                      direction: kpis.available / kpis.total > 0.7 ? "up" : "down",
                      value: `${Math.round((kpis.available / kpis.total) * 100)}%`,
                    }
                  : undefined
              }
            />
            <KpiCard
              label="Wartung / Blockiert"
              value={String(kpis?.maintenanceBlocked ?? 0)}
              trend={
                kpis && kpis.maintenanceBlocked > 0
                  ? { direction: "down", value: `${kpis.maintenanceBlocked} offline` }
                  : undefined
              }
            />
            <KpiCard label="Wartung fällig" value="–" />
          </>
        )}
      </div>

      {/* Data Table */}
      <DataTable<MachineResponse>
        data={filteredMachines}
        columns={columns}
        searchPlaceholder="Maschinen suchen..."
        searchKey="name"
        loading={loading}
        pageSize={15}
        onRowClick={(row) => router.push(`/machines/${row.id}`)}
        filterSlots={
          <div className="flex items-center gap-2">
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="h-9 w-40 text-sm">
                <SelectValue placeholder="All Statuses" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Alle Status</SelectItem>
                {MACHINE_STATUSES.map((s) => (
                  <SelectItem key={s} value={s}>
                    {humanizeStatus(s)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Input
              placeholder="Nach Typ filtern..."
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="h-9 w-40 text-sm"
            />
          </div>
        }
        rowActions={[
          {
            label: "Details anzeigen",
            icon: <Eye className="h-3.5 w-3.5" />,
            onClick: (row) => router.push(`/machines/${row.id}`),
          },
          {
            label: "Status ändern",
            icon: <Settings2 className="h-3.5 w-3.5" />,
            onClick: (row) => {
              setStatusTarget(row);
              setNewStatus(row.status);
              setStatusDialogOpen(true);
            },
          },
          {
            label: "Störung melden",
            icon: <AlertTriangle className="h-3.5 w-3.5" />,
            onClick: (row) => {
              setIncidentTarget(row);
              setIncidentDialogOpen(true);
            },
            variant: "destructive",
          },
        ]}
        emptyState={{
          icon: <Factory className="h-8 w-8 text-muted-foreground/40" />,
          title: "Keine Maschinen gefunden",
          description: "Fügen Sie Ihre erste Maschine hinzu.",
          action: (
            <Button
              variant="outline"
              size="sm"
              className="mt-2"
              onClick={() => setCreateOpen(true)}
            >
              <Plus className="mr-1.5 h-3.5 w-3.5" />
              Neue Maschine
            </Button>
          ),
        }}
      />

      {/* ═══ Create Machine Dialog ═══ */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Cog className="h-4 w-4 text-primary" />
              Neue Maschine
            </DialogTitle>
            <DialogDescription>
              Neue Maschine in der Flotte registrieren.
            </DialogDescription>
          </DialogHeader>
          <form
            onSubmit={form.handleSubmit(onCreateSubmit as any)}
            className="space-y-4"
          >
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="name">Name *</Label>
                <Input
                  id="name"
                  placeholder="CNC Milling A1"
                  {...form.register("name")}
                />
                {form.formState.errors.name && (
                  <p className="text-xs text-destructive">
                    {form.formState.errors.name.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="machineNumber">Maschinen-Nr. *</Label>
                <Input
                  id="machineNumber"
                  className="font-mono"
                  {...form.register("machineNumber")}
                />
                {form.formState.errors.machineNumber && (
                  <p className="text-xs text-destructive">
                    {form.formState.errors.machineNumber.message}
                  </p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="type">Typ</Label>
                <Input
                  id="type"
                  placeholder="CNC, Press, Sewing..."
                  {...form.register("type")}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="stationId">Stations-ID</Label>
                <Input
                  id="stationId"
                  placeholder="Station UUID"
                  className="font-mono text-xs"
                  {...form.register("stationId")}
                />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="capacityPerHour">Kapazität/Stunde</Label>
                <Input
                  id="capacityPerHour"
                  type="number"
                  placeholder="0"
                  className="font-mono"
                  {...form.register("capacityPerHour")}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="manufacturer">Hersteller</Label>
                <Input
                  id="manufacturer"
                  placeholder="Siemens, Haas..."
                  {...form.register("manufacturer")}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="model">Modell</Label>
                <Input
                  id="model"
                  placeholder="Model X200"
                  {...form.register("model")}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="serialNumber">Seriennummer</Label>
                <Input
                  id="serialNumber"
                  placeholder="SN-123456"
                  className="font-mono text-xs"
                  {...form.register("serialNumber")}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="purchaseDate">Kaufdatum</Label>
                <Input
                  id="purchaseDate"
                  type="date"
                  {...form.register("purchaseDate")}
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
                Abbrechen
              </Button>
              <Button type="submit" size="sm" disabled={mutations.loading}>
                {mutations.loading ? "Wird erstellt..." : "Maschine erstellen"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* ═══ Change Status Dialog ═══ */}
      <Dialog open={statusDialogOpen} onOpenChange={setStatusDialogOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Maschinenstatus ändern</DialogTitle>
            <DialogDescription>
              {statusTarget
                ? `Status aktualisieren für ${statusTarget.machineNumber} – ${statusTarget.name}`
                : ""}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label>Aktueller Status</Label>
              {statusTarget && (
                <DomainStatusBadge
                  variant={getMachineStatusVariant(statusTarget.status)}
                >
                  {humanizeStatus(statusTarget.status)}
                </DomainStatusBadge>
              )}
            </div>
            <div className="space-y-1.5">
              <Label>Neuer Status</Label>
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
              Abbrechen
            </Button>
            <Button
              size="sm"
              onClick={handleChangeStatus}
              disabled={mutations.loading}
            >
              {mutations.loading ? "Wird aktualisiert..." : "Status aktualisieren"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ═══ Report Incident Dialog ═══ */}
      <Dialog open={incidentDialogOpen} onOpenChange={setIncidentDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-destructive">
              <AlertTriangle className="h-4 w-4" />
              Störung melden
            </DialogTitle>
            <DialogDescription>
              {incidentTarget
                ? `Störung melden für ${incidentTarget.machineNumber} – ${incidentTarget.name}`
                : ""}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Störungsart *</Label>
              <Input
                placeholder="Mechanischer Defekt, Elektrisch, etc."
                value={incidentType}
                onChange={(e) => setIncidentType(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label>Beschreibung</Label>
              <textarea
                className="flex min-h-[80px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                placeholder="Beschreiben Sie, was passiert ist..."
                value={incidentDescription}
                onChange={(e) => setIncidentDescription(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label>Schweregrad</Label>
              <Select
                value={incidentSeverity}
                onValueChange={setIncidentSeverity}
              >
                <SelectTrigger className="text-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="LOW">Niedrig</SelectItem>
                  <SelectItem value="MEDIUM">Mittel</SelectItem>
                  <SelectItem value="HIGH">Hoch</SelectItem>
                  <SelectItem value="CRITICAL">Kritisch</SelectItem>
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
              Abbrechen
            </Button>
            <Button
              variant="destructive"
              size="sm"
              onClick={handleReportIncident}
              disabled={mutations.loading || !incidentType.trim()}
            >
              {mutations.loading ? "Wird gemeldet..." : "Störung melden"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
