"use client";

import { useState, useCallback } from "react";
import { useRouter, useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  ArrowLeft,
  AlertTriangle,
  Puzzle,
  Layers,
  ListTree,
  Link2,
  Plus,
  Save,
  X,
  Pencil,
  CheckCircle,
  Trash2,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import { DomainStatusBadge, getPartTypeVariant, getVersionStatusVariant } from "@/components/shared/domain-status-badge";
import { SkeletonCard, SkeletonTable } from "@/components/shared/skeleton-variants";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

import {
  usePart,
  usePartMutations,
  useBomVersionActive,
  useBomItems,
  useBomMutations,
  useProcessPlans,
  useProcessPlanMutations,
  useCalculations,
} from "@/hooks/api/use-bom";
import type {
  BomItemResponse,
  BomItemRequest,
  ProcessPlanResponse,
  CalculationResponse,
} from "@/types/api";
import { formatDate, formatNumber, formatCurrency, humanizeStatus } from "@/lib/format";
import { toast } from "sonner";

// ─── Schemas ────────────────────────────────────────────

const bomItemSchema = z.object({
  componentPartId: z.string().min(1, "Component Part ID is required"),
  quantity: z.number().min(0.01, "Quantity must be > 0"),
  position: z.number().min(1, "Position must be >= 1"),
  notes: z.string().optional(),
});
type BomItemFormValues = z.infer<typeof bomItemSchema>;

const processPlanSchema = z.object({
  name: z.string().min(1, "Name is required"),
  versionNumber: z.number().min(1).optional(),
});
type ProcessPlanFormValues = z.infer<typeof processPlanSchema>;

// ─── BOM Item Columns ──────────────────────────────────

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

// ─── Process Plan Columns ──────────────────────────────

const processPlanColumns: ColumnDef<ProcessPlanResponse>[] = [
  {
    id: "name",
    header: "Name",
    accessorKey: "name",
    cell: (row) => <span className="font-medium">{row.name}</span>,
  },
  {
    id: "versionNumber",
    header: "Version",
    cell: (row) => (
      <span className="font-mono text-xs">v{row.versionNumber}</span>
    ),
  },
  {
    id: "status",
    header: "Status",
    cell: (row) => (
      <DomainStatusBadge variant={getVersionStatusVariant(row.status)}>
        {humanizeStatus(row.status)}
      </DomainStatusBadge>
    ),
  },
  {
    id: "validFrom",
    header: "Gültig ab",
    cell: (row) => (
      <span className="font-mono text-xs text-muted-foreground">
        {formatDate(row.validFrom)}
      </span>
    ),
  },
  {
    id: "createdAt",
    header: "Erstellt",
    cell: (row) => (
      <span className="font-mono text-xs text-muted-foreground">
        {formatDate(row.createdAt)}
      </span>
    ),
  },
];

// ─── Calculation Columns ───────────────────────────────

const calculationColumns: ColumnDef<CalculationResponse>[] = [
  {
    id: "calculatedAt",
    header: "Datum",
    cell: (row) => (
      <span className="font-mono text-xs">{formatDate(row.calculatedAt)}</span>
    ),
  },
  {
    id: "quantity",
    header: "Menge",
    cell: (row) => (
      <span className="font-mono text-xs">{formatNumber(row.quantity)}</span>
    ),
  },
  {
    id: "materialCost",
    header: "Material",
    cell: (row) => (
      <span className="font-mono text-xs">{formatCurrency(row.materialCost)}</span>
    ),
  },
  {
    id: "laborCost",
    header: "Arbeit",
    cell: (row) => (
      <span className="font-mono text-xs">{formatCurrency(row.laborCost)}</span>
    ),
  },
  {
    id: "totalCost",
    header: "Gesamt",
    cell: (row) => (
      <span className="font-mono text-sm font-semibold">{formatCurrency(row.totalCost)}</span>
    ),
  },
];

// ─── Component ──────────────────────────────────────────

export default function PartDetailPage() {
  const router = useRouter();
  const params = useParams();
  const partId = params.id as string;

  const { data: part, loading, error, refetch } = usePart(partId);
  const partMutations = usePartMutations();

  // BOM
  const { data: activeBom, refetch: refetchBom } = useBomVersionActive(partId);
  const { data: bomItems, loading: bomItemsLoading, refetch: refetchBomItems } = useBomItems(activeBom?.id ?? null);
  const bomMutations = useBomMutations();

  // Process Plans
  const { data: processPlans, loading: plansLoading, refetch: refetchPlans } = useProcessPlans(partId);
  const planMutations = useProcessPlanMutations();

  // Calculations
  const { data: calculations } = useCalculations(partId);

  // State
  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [editType, setEditType] = useState("PRODUCT");

  const [bomItemDialogOpen, setBomItemDialogOpen] = useState(false);
  const [processPlanDialogOpen, setProcessPlanDialogOpen] = useState(false);

  const bomItemForm = useForm<BomItemFormValues>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(bomItemSchema) as any,
    defaultValues: {
      componentPartId: "",
      quantity: 1,
      position: 1,
      notes: "",
    },
  });

  const processPlanForm = useForm<ProcessPlanFormValues>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(processPlanSchema) as any,
    defaultValues: {
      name: "",
      versionNumber: 1,
    },
  });

  // ─── Edit Handlers ────────────────────
  const startEditing = useCallback(() => {
    if (!part) return;
    setEditName(part.name);
    setEditDescription(part.description ?? "");
    setEditType(part.type);
    setIsEditing(true);
  }, [part]);

  const handleSaveEdit = async () => {
    if (!part) return;
    try {
      const result = await partMutations.updatePart(part.id, {
        name: editName,
        description: editDescription || undefined,
        type: editType,
      });
      if (result) {
        toast.success("Teil erfolgreich aktualisiert");
        setIsEditing(false);
        refetch();
      }
    } catch (err) {
      toast.error("Teil konnte nicht aktualisiert werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  // ─── BOM Handlers ─────────────────────
  const handleAddBomItem = async (values: BomItemFormValues) => {
    if (!activeBom) {
      toast.error("Keine aktive Stücklistenversion. Erstellen Sie zuerst eine.");
      return;
    }
    const req: BomItemRequest = {
      componentPartId: values.componentPartId,
      quantity: values.quantity,
      position: values.position,
      notes: values.notes || undefined,
    };
    try {
      await bomMutations.addItem(activeBom.id, req);
      toast.success("Stücklistenposition hinzugefügt");
      setBomItemDialogOpen(false);
      bomItemForm.reset({ componentPartId: "", quantity: 1, position: 1, notes: "" });
      refetchBomItems();
    } catch (err) {
      toast.error("Position konnte nicht hinzugefügt werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  const handleCreateBomVersion = async () => {
    try {
      await bomMutations.createVersion({ partId });
      toast.success("Neue Stücklistenversion erstellt");
      refetchBom();
    } catch (err) {
      toast.error("Version konnte nicht erstellt werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  const handleActivateBom = async () => {
    if (!activeBom) return;
    try {
      await bomMutations.activateVersion(activeBom.id);
      toast.success("Stücklistenversion aktiviert");
      refetchBom();
    } catch (err) {
      toast.error("Aktivierung fehlgeschlagen", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  // ─── Process Plan Handlers ────────────
  const handleCreateProcessPlan = async (values: ProcessPlanFormValues) => {
    try {
      await planMutations.createPlan({
        partId,
        name: values.name,
        versionNumber: values.versionNumber,
      });
      toast.success("Arbeitsplan erstellt");
      setProcessPlanDialogOpen(false);
      processPlanForm.reset({ name: "", versionNumber: 1 });
      refetchPlans();
    } catch (err) {
      toast.error("Arbeitsplan konnte nicht erstellt werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  // ─── Loading ──────────────────────────
  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-3">
          <SkeletonCard className="h-12 w-48" />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
      </div>
    );
  }

  // ─── Error ────────────────────────────
  if (error || !part) {
    return (
      <div className="flex flex-col items-center gap-4 py-16">
        <AlertTriangle className="h-10 w-10 text-destructive/60" />
        <p className="text-sm text-muted-foreground">{error ?? "Teil nicht gefunden"}</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => router.push("/parts-and-processes")}>
            <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
            Zurück
          </Button>
          <Button variant="outline" size="sm" onClick={refetch}>
            Erneut versuchen
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={`${part.partNumber} — ${part.name}`}
        breadcrumb={["Teile & Prozesse", part.partNumber]}
        actions={
          <Button
            variant="outline"
            size="sm"
            onClick={() => router.push("/parts-and-processes")}
          >
            <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
            Zurück
          </Button>
        }
      />

      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview" className="gap-1.5">
            <Puzzle className="h-3.5 w-3.5" />
            Übersicht
          </TabsTrigger>
          <TabsTrigger value="bom" className="gap-1.5">
            <Layers className="h-3.5 w-3.5" />
            Stückliste
          </TabsTrigger>
          <TabsTrigger value="processes" className="gap-1.5">
            <ListTree className="h-3.5 w-3.5" />
            Arbeitspläne
          </TabsTrigger>
          <TabsTrigger value="usage" className="gap-1.5">
            <Link2 className="h-3.5 w-3.5" />
            Verwendung
          </TabsTrigger>
        </TabsList>

        {/* ─── Overview Tab ─── */}
        <TabsContent value="overview" className="mt-4 space-y-6">
          <Card className="relative overflow-hidden p-6">
            <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Teiledetails
              </h2>
              {!isEditing ? (
                <Button variant="outline" size="sm" className="gap-1.5" onClick={startEditing}>
                  <Pencil className="h-3 w-3" />
                  Bearbeiten
                </Button>
              ) : (
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" className="gap-1.5" onClick={() => setIsEditing(false)}>
                    <X className="h-3 w-3" />
                    Abbrechen
                  </Button>
                  <Button size="sm" className="gap-1.5" onClick={handleSaveEdit} disabled={partMutations.loading}>
                    <Save className="h-3 w-3" />
                    {partMutations.loading ? "Wird gespeichert..." : "Speichern"}
                  </Button>
                </div>
              )}
            </div>

            {isEditing ? (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label className="text-xs text-muted-foreground">Teilenummer</Label>
                  <Input value={part.partNumber} disabled className="font-mono" />
                </div>
                <div className="space-y-2">
                  <Label className="text-xs text-muted-foreground">Name</Label>
                  <Input value={editName} onChange={(e) => setEditName(e.target.value)} />
                </div>
                <div className="col-span-full space-y-2">
                  <Label className="text-xs text-muted-foreground">Beschreibung</Label>
                  <Textarea
                    value={editDescription}
                    onChange={(e) => setEditDescription(e.target.value)}
                    rows={2}
                  />
                </div>
                <div className="space-y-2">
                  <Label className="text-xs text-muted-foreground">Typ</Label>
                  <Select value={editType} onValueChange={setEditType}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="PRODUCT">Produkt</SelectItem>
                      <SelectItem value="COMPONENT">Komponente</SelectItem>
                      <SelectItem value="RAW_MATERIAL">Rohmaterial</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                {partMutations.error && (
                  <p className="col-span-full text-xs text-destructive">{partMutations.error}</p>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <DetailField label="Teilenummer" value={part.partNumber} mono />
                <DetailField label="Name" value={part.name} />
                <DetailField
                  label="Beschreibung"
                  value={part.description ?? "–"}
                  className="col-span-full"
                />
                <div className="space-y-1">
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">Type</p>
                  <DomainStatusBadge variant={getPartTypeVariant(part.type)}>
                    {humanizeStatus(part.type)}
                  </DomainStatusBadge>
                </div>
                <div className="space-y-1">
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">Status</p>
                  <DomainStatusBadge variant={getVersionStatusVariant(part.status)}>
                    {humanizeStatus(part.status)}
                  </DomainStatusBadge>
                </div>
                <DetailField label="Einheit" value={part.unitId ? part.unitId.slice(0, 8) + "..." : "–"} />
                <DetailField label="Erstellt" value={formatDate(part.createdAt)} mono />
              </div>
            )}
          </Card>

          {/* Calculation History */}
          {calculations && calculations.length > 0 && (
            <Card className="relative overflow-hidden p-6">
              <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
              <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Kalkulationshistorie
              </h2>
              <DataTable<CalculationResponse>
                data={calculations}
                columns={calculationColumns}
                emptyState={{ title: "Noch keine Kalkulationen" }}
              />
            </Card>
          )}
        </TabsContent>

        {/* ─── BOM Tab ─── */}
        <TabsContent value="bom" className="mt-4 space-y-4">
          {activeBom ? (
            <>
              <Card className="relative overflow-hidden p-4">
                <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-medium">Stücklistenversion</span>
                    <span className="font-mono text-xs text-muted-foreground">v{activeBom.versionNumber}</span>
                    <DomainStatusBadge variant={getVersionStatusVariant(activeBom.status)}>
                      {humanizeStatus(activeBom.status)}
                    </DomainStatusBadge>
                  </div>
                  <div className="flex gap-2">
                    {activeBom.status === "DRAFT" && (
                      <Button
                        size="sm"
                        variant="outline"
                        className="gap-1.5"
                        onClick={handleActivateBom}
                        disabled={bomMutations.loading}
                      >
                        <CheckCircle className="h-3.5 w-3.5" />
                        Aktivieren
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="outline"
                      className="gap-1.5"
                      onClick={handleCreateBomVersion}
                      disabled={bomMutations.loading}
                    >
                      <Plus className="h-3.5 w-3.5" />
                      Neue Version
                    </Button>
                  </div>
                </div>
              </Card>

              {bomItemsLoading ? (
                <SkeletonTable rows={3} columns={5} />
              ) : (
                <DataTable<BomItemResponse>
                  data={bomItems ?? []}
                  columns={bomItemColumns}
                  searchPlaceholder="Positionen suchen..."
                  filterSlots={
                    <Button
                      size="sm"
                      className="gap-1.5"
                      onClick={() => setBomItemDialogOpen(true)}
                    >
                      <Plus className="h-3.5 w-3.5" />
                      Position hinzufügen
                    </Button>
                  }
                  emptyState={{
                    icon: <Layers className="h-8 w-8 text-muted-foreground/40" />,
                    title: "Keine Stücklistenpositionen",
                    description: "Fügen Sie Komponenten zu dieser Stückliste hinzu.",
                    action: (
                      <Button
                        size="sm"
                        variant="outline"
                        className="mt-2"
                        onClick={() => setBomItemDialogOpen(true)}
                      >
                        <Plus className="mr-1.5 h-3.5 w-3.5" />
                        Position hinzufügen
                      </Button>
                    ),
                  }}
                />
              )}
            </>
          ) : (
            <Card className="relative overflow-hidden p-6">
              <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
              <div className="flex flex-col items-center gap-3 py-8 text-center">
                <Layers className="h-10 w-10 text-muted-foreground/30" />
                <h3 className="text-sm font-medium text-foreground/70">
                  Keine Stücklistenversion
                </h3>
                <p className="max-w-sm text-xs text-muted-foreground">
                  Erstellen Sie eine Stücklistenversion, um die Stückliste für dieses Teil zu definieren.
                </p>
                <Button
                  size="sm"
                  className="mt-2 gap-1.5"
                  onClick={handleCreateBomVersion}
                  disabled={bomMutations.loading}
                >
                  <Plus className="h-3.5 w-3.5" />
                  Stücklistenversion erstellen
                </Button>
              </div>
            </Card>
          )}
        </TabsContent>

        {/* ─── Process Plans Tab ─── */}
        <TabsContent value="processes" className="mt-4">
          {plansLoading ? (
            <SkeletonTable rows={3} columns={5} />
          ) : (
            <DataTable<ProcessPlanResponse>
              data={processPlans ?? []}
              columns={processPlanColumns}
              searchPlaceholder="Arbeitspläne suchen..."
              searchKey="name"
              filterSlots={
                <Button
                  size="sm"
                  className="gap-1.5"
                  onClick={() => setProcessPlanDialogOpen(true)}
                >
                  <Plus className="h-3.5 w-3.5" />
                  Neuer Arbeitsplan
                </Button>
              }
              onRowClick={(row) => router.push(`/parts-and-processes/processes/${row.id}`)}
              emptyState={{
                icon: <ListTree className="h-8 w-8 text-muted-foreground/40" />,
                title: "Keine Arbeitspläne",
                description: "Erstellen Sie einen Arbeitsplan, um Fertigungsschritte zu definieren.",
                action: (
                  <Button
                    size="sm"
                    variant="outline"
                    className="mt-2"
                    onClick={() => setProcessPlanDialogOpen(true)}
                  >
                    <Plus className="mr-1.5 h-3.5 w-3.5" />
                    Neuer Arbeitsplan
                  </Button>
                ),
              }}
            />
          )}
        </TabsContent>

        {/* ─── Usage Tab ─── */}
        <TabsContent value="usage" className="mt-4">
          <Card className="relative overflow-hidden p-6">
            <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
            <div className="flex flex-col items-center gap-3 py-8 text-center">
              <Link2 className="h-10 w-10 text-muted-foreground/30" />
              <h3 className="text-sm font-medium text-foreground/70">
                Verwendungsverfolgung
              </h3>
              <p className="max-w-sm text-xs text-muted-foreground">
                Verwendungsverfolgung folgt in Kürze. Hier werden alle Stücklisten und Aufträge
                angezeigt, die dieses Teil referenzieren.
              </p>
            </div>
          </Card>
        </TabsContent>
      </Tabs>

      {/* ─── Add BOM Item Dialog ─── */}
      <Dialog open={bomItemDialogOpen} onOpenChange={setBomItemDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Stücklistenposition hinzufügen</DialogTitle>
            <DialogDescription>
              Komponente zur Stückliste hinzufügen.
            </DialogDescription>
          </DialogHeader>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          <form onSubmit={bomItemForm.handleSubmit(handleAddBomItem as any)} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="componentPartId">Komponententeil-ID</Label>
              <Input
                id="componentPartId"
                className="font-mono text-xs"
                placeholder="UUID des Komponententeils"
                {...bomItemForm.register("componentPartId")}
              />
              {bomItemForm.formState.errors.componentPartId && (
                <p className="text-xs text-destructive">
                  {bomItemForm.formState.errors.componentPartId.message}
                </p>
              )}
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="bomQty">Menge</Label>
                <Input
                  id="bomQty"
                  type="number"
                  min={0.01}
                  step={0.01}
                  className="font-mono"
                  {...bomItemForm.register("quantity", { valueAsNumber: true })}
                />
                {bomItemForm.formState.errors.quantity && (
                  <p className="text-xs text-destructive">
                    {bomItemForm.formState.errors.quantity.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="bomPos">Position</Label>
                <Input
                  id="bomPos"
                  type="number"
                  min={1}
                  className="font-mono"
                  {...bomItemForm.register("position", { valueAsNumber: true })}
                />
                {bomItemForm.formState.errors.position && (
                  <p className="text-xs text-destructive">
                    {bomItemForm.formState.errors.position.message}
                  </p>
                )}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="bomNotes">Notizen</Label>
              <Textarea
                id="bomNotes"
                rows={2}
                {...bomItemForm.register("notes")}
              />
            </div>
            {bomMutations.error && (
              <p className="text-xs text-destructive">{bomMutations.error}</p>
            )}
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setBomItemDialogOpen(false)}
              >
                Abbrechen
              </Button>
              <Button type="submit" size="sm" disabled={bomMutations.loading}>
                {bomMutations.loading ? "Wird hinzugefügt..." : "Position hinzufügen"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* ─── New Process Plan Dialog ─── */}
      <Dialog open={processPlanDialogOpen} onOpenChange={setProcessPlanDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Neuer Arbeitsplan</DialogTitle>
            <DialogDescription>
              Neuen Arbeitsplan für {part.partNumber} erstellen.
            </DialogDescription>
          </DialogHeader>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          <form onSubmit={processPlanForm.handleSubmit(handleCreateProcessPlan as any)} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="planName">Planname</Label>
              <Input id="planName" {...processPlanForm.register("name")} />
              {processPlanForm.formState.errors.name && (
                <p className="text-xs text-destructive">
                  {processPlanForm.formState.errors.name.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="planVersion">Versionsnummer</Label>
              <Input
                id="planVersion"
                type="number"
                min={1}
                className="font-mono"
                {...processPlanForm.register("versionNumber", { valueAsNumber: true })}
              />
            </div>
            {planMutations.error && (
              <p className="text-xs text-destructive">{planMutations.error}</p>
            )}
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setProcessPlanDialogOpen(false)}
              >
                Abbrechen
              </Button>
              <Button type="submit" size="sm" disabled={planMutations.loading}>
                {planMutations.loading ? "Wird erstellt..." : "Plan erstellen"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ─── Helper Components ──────────────────────────────────

function DetailField({
  label,
  value,
  mono,
  className,
}: {
  label: string;
  value: string;
  mono?: boolean;
  className?: string;
}) {
  return (
    <div className={`space-y-1 ${className ?? ""}`}>
      <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
        {label}
      </p>
      <p className={`text-sm ${mono ? "font-mono" : ""}`}>{value}</p>
    </div>
  );
}
