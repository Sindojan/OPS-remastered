"use client";

import { useState } from "react";
import { useRouter, useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  ArrowLeft,
  AlertTriangle,
  ListTree,
  Plus,
  CheckCircle,
  Clock,
  Settings,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import { DomainStatusBadge, getVersionStatusVariant } from "@/components/shared/domain-status-badge";
import { SkeletonCard, SkeletonTable } from "@/components/shared/skeleton-variants";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

import {
  useProcessSteps,
  useProcessPlanMutations,
} from "@/hooks/api/use-bom";
import { useApi } from "@/hooks/api/use-api";
import type { ProcessPlanResponse, ProcessStepResponse, ProcessStepRequest } from "@/types/api";
import { formatDate, formatNumber, humanizeStatus } from "@/lib/format";
import { toast } from "sonner";

// ─── Schema ─────────────────────────────────────────────

const stepSchema = z.object({
  stepNumber: z.number().min(1, "Step number is required"),
  name: z.string().min(1, "Name is required"),
  description: z.string().optional(),
  stationId: z.string().optional(),
  machineId: z.string().optional(),
  setupTimeMinutes: z.number().min(0, "Setup time must be >= 0"),
  processingTimeMinutes: z.number().min(0, "Processing time must be >= 0"),
  notes: z.string().optional(),
});
type StepFormValues = z.infer<typeof stepSchema>;

// ─── Step Columns ───────────────────────────────────────

const stepColumns: ColumnDef<ProcessStepResponse>[] = [
  {
    id: "stepNumber",
    header: "Schritt-Nr.",
    cell: (row) => (
      <span className="font-mono text-xs font-semibold">{row.stepNumber}</span>
    ),
  },
  {
    id: "name",
    header: "Name",
    accessorKey: "name",
    cell: (row) => <span className="font-medium">{row.name}</span>,
  },
  {
    id: "station",
    header: "Station",
    cell: (row) => (
      <span className="text-xs text-muted-foreground">
        {row.stationId ? row.stationId.slice(0, 8) + "..." : "–"}
      </span>
    ),
    sortable: false,
  },
  {
    id: "machine",
    header: "Maschine",
    cell: (row) => (
      <span className="text-xs text-muted-foreground">
        {row.machineId ? row.machineId.slice(0, 8) + "..." : "–"}
      </span>
    ),
    sortable: false,
  },
  {
    id: "setupTime",
    header: "Rüstzeit (min)",
    cell: (row) => (
      <span className="font-mono text-xs">{formatNumber(row.setupTimeMinutes)}</span>
    ),
  },
  {
    id: "processingTime",
    header: "Taktzeit (min)",
    cell: (row) => (
      <span className="font-mono text-xs">{formatNumber(row.processingTimeMinutes)}</span>
    ),
  },
  {
    id: "notes",
    header: "Notizen",
    cell: (row) => (
      <span className="max-w-[200px] truncate text-xs text-muted-foreground">
        {row.notes ?? "–"}
      </span>
    ),
    sortable: false,
  },
];

// ─── Component ──────────────────────────────────────────

export default function ProcessPlanDetailPage() {
  const router = useRouter();
  const params = useParams();
  const planId = params.id as string;

  const { data: plan, loading: planLoading, error: planError, refetch: refetchPlan } =
    useApi<ProcessPlanResponse>(planId ? `/api/process-plans/${planId}` : null);
  const { data: steps, loading: stepsLoading, refetch: refetchSteps } = useProcessSteps(planId);
  const planMutations = useProcessPlanMutations();

  const [stepDialogOpen, setStepDialogOpen] = useState(false);

  const stepForm = useForm<StepFormValues>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(stepSchema) as any,
    defaultValues: {
      stepNumber: 1,
      name: "",
      description: "",
      stationId: "",
      machineId: "",
      setupTimeMinutes: 0,
      processingTimeMinutes: 0,
      notes: "",
    },
  });

  const handleAddStep = async (values: StepFormValues) => {
    const req: ProcessStepRequest = {
      stepNumber: values.stepNumber,
      name: values.name,
      description: values.description || undefined,
      stationId: values.stationId || undefined,
      machineId: values.machineId || undefined,
      setupTimeMinutes: values.setupTimeMinutes,
      processingTimeMinutes: values.processingTimeMinutes,
      notes: values.notes || undefined,
    };
    try {
      await planMutations.addStep(planId, req);
      toast.success("Schritt hinzugefügt");
      setStepDialogOpen(false);
      stepForm.reset({
        stepNumber: (steps?.length ?? 0) + 2,
        name: "",
        description: "",
        stationId: "",
        machineId: "",
        setupTimeMinutes: 0,
        processingTimeMinutes: 0,
        notes: "",
      });
      refetchSteps();
    } catch (err) {
      toast.error("Schritt konnte nicht hinzugefügt werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  const handleActivate = async () => {
    try {
      await planMutations.activatePlan(planId);
      toast.success("Arbeitsplan aktiviert");
      refetchPlan();
    } catch (err) {
      toast.error("Aktivierung fehlgeschlagen", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  // Time totals
  const totalSetup = steps?.reduce((sum, s) => sum + s.setupTimeMinutes, 0) ?? 0;
  const totalProcessing = steps?.reduce((sum, s) => sum + s.processingTimeMinutes, 0) ?? 0;
  const totalTime = totalSetup + totalProcessing;

  if (planLoading) {
    return (
      <div className="space-y-6">
        <SkeletonCard className="h-12 w-48" />
        <SkeletonTable rows={3} columns={7} />
      </div>
    );
  }

  if (planError || !plan) {
    return (
      <div className="flex flex-col items-center gap-4 py-16">
        <AlertTriangle className="h-10 w-10 text-destructive/60" />
        <p className="text-sm text-muted-foreground">{planError ?? "Arbeitsplan nicht gefunden"}</p>
        <Button variant="outline" size="sm" onClick={() => router.back()}>
          <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
          Back
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={plan.name}
        breadcrumb={["Teile & Prozesse", "Arbeitspläne", plan.name]}
        actions={
          <Button variant="outline" size="sm" onClick={() => router.back()}>
            <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
            Zurück
          </Button>
        }
      />

      {/* Plan Info */}
      <Card className="relative overflow-hidden p-4">
        <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <ListTree className="h-4 w-4 text-muted-foreground" />
            <span className="font-mono text-xs">Part: {plan.partId.slice(0, 8)}...</span>
            <span className="font-mono text-xs text-muted-foreground">v{plan.versionNumber}</span>
            <DomainStatusBadge variant={getVersionStatusVariant(plan.status)}>
              {humanizeStatus(plan.status)}
            </DomainStatusBadge>
            <span className="font-mono text-xs text-muted-foreground">
              {formatDate(plan.createdAt)}
            </span>
          </div>
          <div className="flex gap-2">
            {plan.status === "DRAFT" && (
              <Button
                size="sm"
                variant="outline"
                className="gap-1.5"
                onClick={handleActivate}
                disabled={planMutations.loading}
              >
                <CheckCircle className="h-3.5 w-3.5" />
                Plan aktivieren
              </Button>
            )}
          </div>
        </div>
      </Card>

      {/* Time Summary */}
      {steps && steps.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Card className="relative overflow-hidden p-4">
            <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
            <div className="flex items-center gap-2">
              <Settings className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">Rüstzeit gesamt</p>
                <p className="font-mono text-lg font-bold">{formatNumber(totalSetup)} min</p>
              </div>
            </div>
          </Card>
          <Card className="relative overflow-hidden p-4">
            <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">Bearbeitungszeit gesamt</p>
                <p className="font-mono text-lg font-bold">{formatNumber(totalProcessing)} min</p>
              </div>
            </div>
          </Card>
          <Card className="relative overflow-hidden p-4">
            <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 text-primary" />
              <div>
                <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">Gesamtzeit</p>
                <p className="font-mono text-lg font-bold text-primary">{formatNumber(totalTime)} min</p>
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* Process Steps Table */}
      {stepsLoading ? (
        <SkeletonTable rows={3} columns={7} />
      ) : (
        <DataTable<ProcessStepResponse>
          data={steps ?? []}
          columns={stepColumns}
          searchPlaceholder="Schritte suchen..."
          searchKey="name"
          filterSlots={
            <Button
              size="sm"
              className="gap-1.5"
              onClick={() => {
                stepForm.reset({
                  stepNumber: (steps?.length ?? 0) + 1,
                  name: "",
                  description: "",
                  stationId: "",
                  machineId: "",
                  setupTimeMinutes: 0,
                  processingTimeMinutes: 0,
                  notes: "",
                });
                setStepDialogOpen(true);
              }}
            >
              <Plus className="h-3.5 w-3.5" />
              Schritt hinzufügen
            </Button>
          }
          emptyState={{
            icon: <ListTree className="h-8 w-8 text-muted-foreground/40" />,
            title: "Keine Prozessschritte",
            description: "Fügen Sie Fertigungsschritte zu diesem Arbeitsplan hinzu.",
            action: (
              <Button
                size="sm"
                variant="outline"
                className="mt-2"
                onClick={() => setStepDialogOpen(true)}
              >
                <Plus className="mr-1.5 h-3.5 w-3.5" />
                Schritt hinzufügen
              </Button>
            ),
          }}
        />
      )}

      {/* ─── Add Step Dialog ─── */}
      <Dialog open={stepDialogOpen} onOpenChange={setStepDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Prozessschritt hinzufügen</DialogTitle>
            <DialogDescription>
              Fertigungsschritt zu {plan.name} hinzufügen.
            </DialogDescription>
          </DialogHeader>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          <form onSubmit={stepForm.handleSubmit(handleAddStep as any)} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="stepNum">Schrittnummer</Label>
                <Input
                  id="stepNum"
                  type="number"
                  min={1}
                  className="font-mono"
                  {...stepForm.register("stepNumber", { valueAsNumber: true })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="stepName">Name</Label>
                <Input id="stepName" {...stepForm.register("name")} />
                {stepForm.formState.errors.name && (
                  <p className="text-xs text-destructive">
                    {stepForm.formState.errors.name.message}
                  </p>
                )}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="stepDesc">Beschreibung</Label>
              <Textarea id="stepDesc" rows={2} {...stepForm.register("description")} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="stationId">Stations-ID</Label>
                <Input
                  id="stationId"
                  className="font-mono text-xs"
                  placeholder="Optional"
                  {...stepForm.register("stationId")}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="machineId">Maschinen-ID</Label>
                <Input
                  id="machineId"
                  className="font-mono text-xs"
                  placeholder="Optional"
                  {...stepForm.register("machineId")}
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="setupTime">Rüstzeit (min)</Label>
                <Input
                  id="setupTime"
                  type="number"
                  min={0}
                  className="font-mono"
                  {...stepForm.register("setupTimeMinutes", { valueAsNumber: true })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="processTime">Bearbeitungszeit (min)</Label>
                <Input
                  id="processTime"
                  type="number"
                  min={0}
                  className="font-mono"
                  {...stepForm.register("processingTimeMinutes", { valueAsNumber: true })}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="stepNotes">Notizen</Label>
              <Textarea id="stepNotes" rows={2} {...stepForm.register("notes")} />
            </div>
            {planMutations.error && (
              <p className="text-xs text-destructive">{planMutations.error}</p>
            )}
            <DialogFooter>
              <Button type="button" variant="outline" size="sm" onClick={() => setStepDialogOpen(false)}>
                Abbrechen
              </Button>
              <Button type="submit" size="sm" disabled={planMutations.loading}>
                {planMutations.loading ? "Wird hinzugefügt..." : "Schritt hinzufügen"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
