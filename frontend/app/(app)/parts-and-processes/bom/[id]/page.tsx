"use client";

import { useState } from "react";
import { useRouter, useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  ArrowLeft,
  AlertTriangle,
  Layers,
  Plus,
  CheckCircle,
  Archive,
  Calculator,
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
  useBomItems,
  useBomMutations,
  useCalculationMutations,
} from "@/hooks/api/use-bom";
import { useApi } from "@/hooks/api/use-api";
import type { BomItemResponse, BomItemRequest, BomVersionResponse } from "@/types/api";
import { formatDate, formatNumber, formatCurrency, humanizeStatus } from "@/lib/format";
import { toast } from "sonner";

// ─── Schema ─────────────────────────────────────────────

const bomItemSchema = z.object({
  componentPartId: z.string().min(1, "Component Part ID is required"),
  quantity: z.number().min(0.01, "Quantity must be > 0"),
  position: z.number().min(1, "Position must be >= 1"),
  notes: z.string().optional(),
});
type BomItemFormValues = z.infer<typeof bomItemSchema>;

// ─── Columns ────────────────────────────────────────────

const bomItemColumns: ColumnDef<BomItemResponse>[] = [
  {
    id: "position",
    header: "Pos",
    cell: (row) => (
      <span className="font-mono text-xs font-semibold">{row.position}</span>
    ),
  },
  {
    id: "componentPartId",
    header: "Komponententeil",
    cell: (row) => (
      <span className="font-mono text-xs text-primary">
        {row.componentPartId.slice(0, 8)}...
      </span>
    ),
  },
  {
    id: "quantity",
    header: "Menge",
    cell: (row) => (
      <span className="font-mono text-sm font-semibold">{formatNumber(row.quantity)}</span>
    ),
  },
  {
    id: "unit",
    header: "Einheit",
    cell: (row) => (
      <span className="text-xs text-muted-foreground">
        {row.unitId ? row.unitId.slice(0, 8) + "..." : "–"}
      </span>
    ),
    sortable: false,
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

export default function BomVersionDetailPage() {
  const router = useRouter();
  const params = useParams();
  const versionId = params.id as string;

  const { data: version, loading: versionLoading, error: versionError, refetch: refetchVersion } =
    useApi<BomVersionResponse>(versionId ? `/api/bom/versions/${versionId}` : null);
  const { data: items, loading: itemsLoading, refetch: refetchItems } = useBomItems(versionId);
  const bomMutations = useBomMutations();
  const calcMutations = useCalculationMutations();

  const [itemDialogOpen, setItemDialogOpen] = useState(false);
  const [lastCalcResult, setLastCalcResult] = useState<{ materialCost: number; totalCost: number } | null>(null);

  const itemForm = useForm<BomItemFormValues>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(bomItemSchema) as any,
    defaultValues: {
      componentPartId: "",
      quantity: 1,
      position: 1,
      notes: "",
    },
  });

  const handleAddItem = async (values: BomItemFormValues) => {
    const req: BomItemRequest = {
      componentPartId: values.componentPartId,
      quantity: values.quantity,
      position: values.position,
      notes: values.notes || undefined,
    };
    try {
      await bomMutations.addItem(versionId, req);
      toast.success("Position hinzugefügt");
      setItemDialogOpen(false);
      itemForm.reset({ componentPartId: "", quantity: 1, position: 1, notes: "" });
      refetchItems();
    } catch (err) {
      toast.error("Position konnte nicht hinzugefügt werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  const handleActivate = async () => {
    try {
      await bomMutations.activateVersion(versionId);
      toast.success("Stücklistenversion aktiviert");
      refetchVersion();
    } catch (err) {
      toast.error("Aktivierung fehlgeschlagen", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  const handleCalculate = async () => {
    if (!version) return;
    try {
      const result = await calcMutations.calculate({
        partId: version.partId,
        bomVersionId: versionId,
        processPlanId: "", // would need a process plan ID
        quantity: 1,
      });
      if (result) {
        setLastCalcResult({ materialCost: result.materialCost, totalCost: result.totalCost });
        toast.success("Kalkulation abgeschlossen");
      }
    } catch (err) {
      toast.error("Kalkulation fehlgeschlagen", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  if (versionLoading) {
    return (
      <div className="space-y-6">
        <SkeletonCard className="h-12 w-48" />
        <SkeletonTable rows={3} columns={5} />
      </div>
    );
  }

  if (versionError || !version) {
    return (
      <div className="flex flex-col items-center gap-4 py-16">
        <AlertTriangle className="h-10 w-10 text-destructive/60" />
        <p className="text-sm text-muted-foreground">{versionError ?? "Stücklistenversion nicht gefunden"}</p>
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
        title={`BOM Version v${version.versionNumber}`}
        breadcrumb={["Teile & Prozesse", "Stückliste", `v${version.versionNumber}`]}
        actions={
          <Button variant="outline" size="sm" onClick={() => router.back()}>
            <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
            Zurück
          </Button>
        }
      />

      {/* Version Info */}
      <Card className="relative overflow-hidden p-4">
        <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Layers className="h-4 w-4 text-muted-foreground" />
            <span className="font-mono text-xs">Part: {version.partId.slice(0, 8)}...</span>
            <DomainStatusBadge variant={getVersionStatusVariant(version.status)}>
              {humanizeStatus(version.status)}
            </DomainStatusBadge>
            <span className="font-mono text-xs text-muted-foreground">
              {formatDate(version.createdAt)}
            </span>
          </div>
          <div className="flex gap-2">
            {version.status === "DRAFT" && (
              <Button
                size="sm"
                variant="outline"
                className="gap-1.5"
                onClick={handleActivate}
                disabled={bomMutations.loading}
              >
                <CheckCircle className="h-3.5 w-3.5" />
                Aktivieren
              </Button>
            )}
          </div>
        </div>
      </Card>

      {/* BOM Items Table */}
      {itemsLoading ? (
        <SkeletonTable rows={3} columns={5} />
      ) : (
        <DataTable<BomItemResponse>
          data={items ?? []}
          columns={bomItemColumns}
          filterSlots={
            <Button
              size="sm"
              className="gap-1.5"
              onClick={() => setItemDialogOpen(true)}
            >
              <Plus className="h-3.5 w-3.5" />
              Position hinzufügen
            </Button>
          }
          emptyState={{
            icon: <Layers className="h-8 w-8 text-muted-foreground/40" />,
            title: "Keine Positionen in dieser Stücklistenversion",
            description: "Fügen Sie Komponenten hinzu, um die Stückliste aufzubauen.",
            action: (
              <Button
                size="sm"
                variant="outline"
                className="mt-2"
                onClick={() => setItemDialogOpen(true)}
              >
                <Plus className="mr-1.5 h-3.5 w-3.5" />
                Position hinzufügen
              </Button>
            ),
          }}
        />
      )}

      {/* Calculation Panel */}
      <Card className="relative overflow-hidden p-6">
        <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Kostenkalkulation
            </h2>
            {lastCalcResult && (
              <div className="mt-2 flex gap-6">
                <div className="space-y-0.5">
                  <p className="text-[11px] text-muted-foreground">Materialkosten</p>
                  <p className="font-mono text-sm font-semibold">{formatCurrency(lastCalcResult.materialCost)}</p>
                </div>
                <div className="space-y-0.5">
                  <p className="text-[11px] text-muted-foreground">Gesamtkosten</p>
                  <p className="font-mono text-sm font-semibold">{formatCurrency(lastCalcResult.totalCost)}</p>
                </div>
              </div>
            )}
          </div>
          <Button
            size="sm"
            variant="outline"
            className="gap-1.5"
            onClick={handleCalculate}
            disabled={calcMutations.loading}
          >
            <Calculator className="h-3.5 w-3.5" />
            {calcMutations.loading ? "Wird berechnet..." : "Kalkulieren"}
          </Button>
        </div>
      </Card>

      {/* ─── Add Item Dialog ─── */}
      <Dialog open={itemDialogOpen} onOpenChange={setItemDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Stücklistenposition hinzufügen</DialogTitle>
            <DialogDescription>
              Komponente zu Stücklistenversion v{version.versionNumber} hinzufügen.
            </DialogDescription>
          </DialogHeader>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          <form onSubmit={itemForm.handleSubmit(handleAddItem as any)} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="compPartId">Komponententeil-ID</Label>
              <Input
                id="compPartId"
                className="font-mono text-xs"
                placeholder="UUID des Komponententeils"
                {...itemForm.register("componentPartId")}
              />
              {itemForm.formState.errors.componentPartId && (
                <p className="text-xs text-destructive">
                  {itemForm.formState.errors.componentPartId.message}
                </p>
              )}
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="itemQty">Menge</Label>
                <Input
                  id="itemQty"
                  type="number"
                  min={0.01}
                  step={0.01}
                  className="font-mono"
                  {...itemForm.register("quantity", { valueAsNumber: true })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="itemPos">Position</Label>
                <Input
                  id="itemPos"
                  type="number"
                  min={1}
                  className="font-mono"
                  {...itemForm.register("position", { valueAsNumber: true })}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="itemNotes">Notizen</Label>
              <Textarea id="itemNotes" rows={2} {...itemForm.register("notes")} />
            </div>
            {bomMutations.error && (
              <p className="text-xs text-destructive">{bomMutations.error}</p>
            )}
            <DialogFooter>
              <Button type="button" variant="outline" size="sm" onClick={() => setItemDialogOpen(false)}>
                Abbrechen
              </Button>
              <Button type="submit" size="sm" disabled={bomMutations.loading}>
                {bomMutations.loading ? "Wird hinzugefügt..." : "Position hinzufügen"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
