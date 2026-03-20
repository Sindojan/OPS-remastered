"use client";

import { useState, useCallback } from "react";
import { useRouter, useParams, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  ArrowLeft,
  AlertTriangle,
  Package,
  ArrowDownToLine,
  Layers,
  Truck,
  Save,
  X,
  Pencil,
  BarChart3,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import { DomainStatusBadge } from "@/components/shared/domain-status-badge";
import { SkeletonCard, SkeletonTable } from "@/components/shared/skeleton-variants";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Progress } from "@/components/ui/progress";
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
  useArticle,
  useStockSummary,
  useMovements,
  useInventoryMutations,
} from "@/hooks/api/use-inventory";
import type {
  MovementResponse,
  StockMovementType,
  CreateMovementRequest,
} from "@/types/api";
import {
  formatDateTime,
  formatNumber,
  humanizeStatus,
} from "@/lib/format";
import { toast } from "sonner";

// ─── Status Helpers ─────────────────────────────────────

function getArticleStatusVariant(status: string) {
  switch (status) {
    case "ACTIVE":
      return "success" as const;
    case "INACTIVE":
      return "neutral" as const;
    case "DISCONTINUED":
      return "error" as const;
    default:
      return "neutral" as const;
  }
}

function getMovementTypeVariant(type: StockMovementType) {
  switch (type) {
    case "INBOUND":
      return "success" as const;
    case "OUTBOUND":
      return "warning" as const;
    case "TRANSFER":
      return "info" as const;
    case "CORRECTION":
      return "neutral" as const;
    default:
      return "neutral" as const;
  }
}

// ─── Movement Schema ────────────────────────────────────

const movementSchema = z.object({
  type: z.enum(["INBOUND", "OUTBOUND", "TRANSFER", "CORRECTION"]),
  quantity: z.number().min(1, "Menge muss mindestens 1 sein"),
  fromLocationId: z.string().optional(),
  toLocationId: z.string().optional(),
  notes: z.string().optional(),
});
type MovementFormValues = z.infer<typeof movementSchema>;

// ─── Movement Columns ───────────────────────────────────

const movementColumns: ColumnDef<MovementResponse>[] = [
  {
    id: "createdAt",
    header: "Datum",
    cell: (row) => (
      <span className="font-mono text-xs">{formatDateTime(row.createdAt)}</span>
    ),
  },
  {
    id: "type",
    header: "Typ",
    cell: (row) => (
      <DomainStatusBadge variant={getMovementTypeVariant(row.type)}>
        {humanizeStatus(row.type)}
      </DomainStatusBadge>
    ),
  },
  {
    id: "quantity",
    header: "Menge",
    cell: (row) => (
      <span className="font-mono text-sm font-semibold">
        {row.type === "OUTBOUND" ? "-" : "+"}
        {formatNumber(row.quantity)}
      </span>
    ),
  },
  {
    id: "fromLocation",
    header: "Von",
    cell: (row) => (
      <span className="text-xs text-muted-foreground">
        {row.fromLocationId ? row.fromLocationId.slice(0, 8) + "..." : "–"}
      </span>
    ),
    sortable: false,
  },
  {
    id: "toLocation",
    header: "Nach",
    cell: (row) => (
      <span className="text-xs text-muted-foreground">
        {row.toLocationId ? row.toLocationId.slice(0, 8) + "..." : "–"}
      </span>
    ),
    sortable: false,
  },
  {
    id: "reference",
    header: "Referenz",
    cell: (row) => (
      <span className="font-mono text-xs text-muted-foreground">
        {row.referenceType ? `${row.referenceType}` : "–"}
      </span>
    ),
    sortable: false,
  },
  {
    id: "performedBy",
    header: "Durchgefuehrt von",
    cell: (row) => (
      <span className="text-xs text-muted-foreground">
        {row.performedBy ?? "–"}
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

export default function ArticleDetailPage() {
  const router = useRouter();
  const params = useParams();
  const searchParams = useSearchParams();
  const articleId = params.id as string;

  const { data: article, loading, error, refetch } = useArticle(articleId);
  const { data: stockSummary } = useStockSummary(articleId);
  const { data: movements, loading: movementsLoading, refetch: refetchMovements } = useMovements(articleId);
  const mutations = useInventoryMutations();

  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [editMinStock, setEditMinStock] = useState(0);
  const [editReorderPoint, setEditReorderPoint] = useState(0);
  const [movementDialogOpen, setMovementDialogOpen] = useState(false);
  const [movementTypeFilter, setMovementTypeFilter] = useState<string>("all");

  const defaultTab = searchParams.get("tab") || "overview";

  const movementForm = useForm<MovementFormValues>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(movementSchema) as any,
    defaultValues: {
      type: "INBOUND",
      quantity: 1,
      fromLocationId: "",
      toLocationId: "",
      notes: "",
    },
  });

  const startEditing = useCallback(() => {
    if (!article) return;
    setEditName(article.name);
    setEditDescription(article.description ?? "");
    setEditMinStock(article.minStock ?? 0);
    setEditReorderPoint(article.reorderPoint ?? 0);
    setIsEditing(true);
  }, [article]);

  const handleSaveEdit = async () => {
    if (!article) return;
    try {
      const result = await mutations.updateArticle(article.id, {
        name: editName,
        description: editDescription || undefined,
        minStock: editMinStock,
        reorderPoint: editReorderPoint,
      });
      if (result) {
        toast.success("Artikel erfolgreich aktualisiert");
        setIsEditing(false);
        refetch();
      }
    } catch (err) {
      toast.error("Artikel konnte nicht aktualisiert werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  const handleCreateMovement = async (values: MovementFormValues) => {
    const req: CreateMovementRequest = {
      articleId,
      type: values.type,
      quantity: values.quantity,
      fromLocationId: values.fromLocationId || undefined,
      toLocationId: values.toLocationId || undefined,
      notes: values.notes || undefined,
    };
    try {
      const result = await mutations.createMovement(req);
      if (result) {
        toast.success("Bewegung erfolgreich gebucht");
        setMovementDialogOpen(false);
        movementForm.reset();
        refetchMovements();
      }
    } catch (err) {
      toast.error("Bewegung konnte nicht gebucht werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  const filteredMovements = movements
    ? movementTypeFilter === "all"
      ? movements
      : movements.filter((m) => m.type === movementTypeFilter)
    : [];

  // ─── Loading ───────────────────────────
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

  // ─── Error ─────────────────────────────
  if (error || !article) {
    return (
      <div className="flex flex-col items-center gap-4 py-16">
        <AlertTriangle className="h-10 w-10 text-destructive/60" />
        <p className="text-sm text-muted-foreground">{error ?? "Artikel nicht gefunden"}</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => router.push("/inventory")}>
            <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
            Zurueck zum Lager
          </Button>
          <Button variant="outline" size="sm" onClick={refetch}>
            Erneut versuchen
          </Button>
        </div>
      </div>
    );
  }

  // ─── Stock Gauge ───────────────────────
  const totalQty = stockSummary?.totalQuantity ?? 0;
  const reservedQty = stockSummary?.totalReserved ?? 0;
  const availableQty = stockSummary?.totalAvailable ?? 0;
  const maxQty = Math.max(totalQty, article.minStock ?? 0, 1);

  return (
    <div className="space-y-6">
      <PageHeader
        title={`${article.articleNumber} — ${article.name}`}
        breadcrumb={["Lager", article.articleNumber]}
        actions={
          <Button
            variant="outline"
            size="sm"
            onClick={() => router.push("/inventory")}
          >
            <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
            Zurueck
          </Button>
        }
      />

      <Tabs defaultValue={defaultTab}>
        <TabsList>
          <TabsTrigger value="overview" className="gap-1.5">
            <Package className="h-3.5 w-3.5" />
            Übersicht
          </TabsTrigger>
          <TabsTrigger value="movements" className="gap-1.5">
            <ArrowDownToLine className="h-3.5 w-3.5" />
            Bewegungen
          </TabsTrigger>
          <TabsTrigger value="suppliers" className="gap-1.5">
            <Truck className="h-3.5 w-3.5" />
            Lieferanten
          </TabsTrigger>
          <TabsTrigger value="bom" className="gap-1.5">
            <Layers className="h-3.5 w-3.5" />
            Verwendung
          </TabsTrigger>
        </TabsList>

        {/* ─── Overview Tab ─── */}
        <TabsContent value="overview" className="mt-4 space-y-6">
          {/* Key Data */}
          <Card className="relative overflow-hidden p-6">
            <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Artikeldetails
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
                  <Button size="sm" className="gap-1.5" onClick={handleSaveEdit} disabled={mutations.loading}>
                    <Save className="h-3 w-3" />
                    {mutations.loading ? "Wird gespeichert..." : "Speichern"}
                  </Button>
                </div>
              )}
            </div>

            {isEditing ? (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label className="text-xs text-muted-foreground">Artikelnummer</Label>
                  <Input value={article.articleNumber} disabled className="font-mono" />
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
                  <Label className="text-xs text-muted-foreground">Mindestbestand</Label>
                  <Input
                    type="number"
                    min={0}
                    className="font-mono"
                    value={editMinStock}
                    onChange={(e) => setEditMinStock(Number(e.target.value))}
                  />
                </div>
                <div className="space-y-2">
                  <Label className="text-xs text-muted-foreground">Nachbestellgrenze</Label>
                  <Input
                    type="number"
                    min={0}
                    className="font-mono"
                    value={editReorderPoint}
                    onChange={(e) => setEditReorderPoint(Number(e.target.value))}
                  />
                </div>
                {mutations.error && (
                  <p className="col-span-full text-xs text-destructive">{mutations.error}</p>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <DetailField label="Artikelnummer" value={article.articleNumber} mono />
                <DetailField label="Name" value={article.name} />
                <DetailField
                  label="Beschreibung"
                  value={article.description ?? "–"}
                  className="col-span-full"
                />
                <DetailField
                  label="Kategorie"
                  value={article.categoryId ? article.categoryId.slice(0, 8) + "..." : "–"}
                />
                <DetailField
                  label="Einheit"
                  value={article.unitId ? article.unitId.slice(0, 8) + "..." : "–"}
                />
                <DetailField label="Mindestbestand" value={formatNumber(article.minStock)} mono />
                <DetailField label="Nachbestellgrenze" value={formatNumber(article.reorderPoint)} mono />
                <div className="space-y-1">
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                    Status
                  </p>
                  <DomainStatusBadge variant={getArticleStatusVariant(article.status)}>
                    {humanizeStatus(article.status)}
                  </DomainStatusBadge>
                </div>
              </div>
            )}
          </Card>

          {/* Stock Gauge */}
          {stockSummary && (
            <Card className="relative overflow-hidden p-6">
              <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
              <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Bestandsniveaus
              </h2>
              <div className="space-y-4">
                <StockBar
                  label="Gesamt"
                  value={totalQty}
                  max={maxQty}
                  color="bg-primary"
                />
                <StockBar
                  label="Reserviert"
                  value={reservedQty}
                  max={maxQty}
                  color="bg-amber-500"
                />
                <StockBar
                  label="Verfuegbar"
                  value={availableQty}
                  max={maxQty}
                  color="bg-emerald-500"
                />
                {article.minStock != null && article.minStock > 0 && (
                  <div className="mt-2 flex items-center gap-2 text-xs text-muted-foreground">
                    <BarChart3 className="h-3 w-3" />
                    Mindestbestand: <span className="font-mono font-semibold">{formatNumber(article.minStock)}</span>
                    {availableQty < article.minStock && (
                      <span className="ml-2 text-amber-500">Unter Mindestbestand</span>
                    )}
                  </div>
                )}
              </div>
            </Card>
          )}
        </TabsContent>

        {/* ─── Movements Tab ─── */}
        <TabsContent value="movements" className="mt-4">
          {movementsLoading ? (
            <SkeletonTable rows={5} columns={6} />
          ) : (
            <DataTable<MovementResponse>
              data={filteredMovements}
              columns={movementColumns}
              searchPlaceholder="Bewegungen suchen..."
              searchKey="notes"
              filterSlots={
                <>
                  <Select value={movementTypeFilter} onValueChange={setMovementTypeFilter}>
                    <SelectTrigger size="sm" className="w-36">
                      <SelectValue placeholder="Alle Typen" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">Alle Typen</SelectItem>
                      <SelectItem value="INBOUND">Eingang</SelectItem>
                      <SelectItem value="OUTBOUND">Ausgang</SelectItem>
                      <SelectItem value="TRANSFER">Umlagerung</SelectItem>
                      <SelectItem value="CORRECTION">Korrektur</SelectItem>
                    </SelectContent>
                  </Select>
                  <Button
                    size="sm"
                    className="gap-1.5"
                    onClick={() => setMovementDialogOpen(true)}
                  >
                    <ArrowDownToLine className="h-3.5 w-3.5" />
                    Bewegung buchen
                  </Button>
                </>
              }
              emptyState={{
                icon: <ArrowDownToLine className="h-8 w-8 text-muted-foreground/40" />,
                title: "Noch keine Bewegungen",
                description: "Buchen Sie eine Lagerbewegung, um die Verfolgung zu starten.",
                action: (
                  <Button
                    size="sm"
                    variant="outline"
                    className="mt-2"
                    onClick={() => setMovementDialogOpen(true)}
                  >
                    Bewegung buchen
                  </Button>
                ),
              }}
            />
          )}
        </TabsContent>

        {/* ─── Suppliers Tab ─── */}
        <TabsContent value="suppliers" className="mt-4">
          <Card className="relative overflow-hidden p-6">
            <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
            <div className="flex flex-col items-center gap-3 py-8 text-center">
              <Truck className="h-10 w-10 text-muted-foreground/30" />
              <h3 className="text-sm font-medium text-foreground/70">
                Lieferanteninformationen
              </h3>
              <p className="max-w-sm text-xs text-muted-foreground">
                Lieferanten-Artikel-Beziehungen und Preisdetails werden hier angezeigt, sobald sie verknuepft sind.
              </p>
            </div>
          </Card>
        </TabsContent>

        {/* ─── BOM Usage Tab ─── */}
        <TabsContent value="bom" className="mt-4">
          <Card className="relative overflow-hidden p-6">
            <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
            <div className="flex flex-col items-center gap-3 py-8 text-center">
              <Layers className="h-10 w-10 text-muted-foreground/30" />
              <h3 className="text-sm font-medium text-foreground/70">
                Stuecklistenverwendung
              </h3>
              <p className="max-w-sm text-xs text-muted-foreground">
                Stuecklistenverwendung kommt bald. Hier werden alle Stuecklisten angezeigt,
                die diesen Artikel referenzieren.
              </p>
            </div>
          </Card>
        </TabsContent>
      </Tabs>

      {/* ─── Book Movement Dialog ─── */}
      <Dialog open={movementDialogOpen} onOpenChange={setMovementDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Lagerbewegung buchen</DialogTitle>
            <DialogDescription>
              Lagerbewegung für {article.articleNumber} erfassen.
            </DialogDescription>
          </DialogHeader>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          <form onSubmit={movementForm.handleSubmit(handleCreateMovement as any)} className="space-y-4">
            <div className="space-y-2">
              <Label>Bewegungsart</Label>
              <Select
                value={movementForm.watch("type")}
                onValueChange={(v) =>
                  movementForm.setValue("type", v as MovementFormValues["type"])
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="INBOUND">Eingang</SelectItem>
                  <SelectItem value="OUTBOUND">Ausgang</SelectItem>
                  <SelectItem value="TRANSFER">Umlagerung</SelectItem>
                  <SelectItem value="CORRECTION">Korrektur</SelectItem>
                </SelectContent>
              </Select>
              {movementForm.formState.errors.type && (
                <p className="text-xs text-destructive">
                  {movementForm.formState.errors.type.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="moveQty">Menge</Label>
              <Input
                id="moveQty"
                type="number"
                min={1}
                className="font-mono"
                {...movementForm.register("quantity", { valueAsNumber: true })}
              />
              {movementForm.formState.errors.quantity && (
                <p className="text-xs text-destructive">
                  {movementForm.formState.errors.quantity.message}
                </p>
              )}
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="fromLoc">Von Lagerort</Label>
                <Input
                  id="fromLoc"
                  placeholder="Lagerort-ID"
                  className="font-mono text-xs"
                  {...movementForm.register("fromLocationId")}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="toLoc">Nach Lagerort</Label>
                <Input
                  id="toLoc"
                  placeholder="Lagerort-ID"
                  className="font-mono text-xs"
                  {...movementForm.register("toLocationId")}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="moveNotes">Notizen</Label>
              <Textarea
                id="moveNotes"
                rows={2}
                {...movementForm.register("notes")}
              />
            </div>
            {mutations.error && (
              <p className="text-xs text-destructive">{mutations.error}</p>
            )}
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setMovementDialogOpen(false)}
              >
                Abbrechen
              </Button>
              <Button type="submit" size="sm" disabled={mutations.loading}>
                {mutations.loading ? "Wird gebucht..." : "Bewegung buchen"}
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

function StockBar({
  label,
  value,
  max,
  color,
}: {
  label: string;
  value: number;
  max: number;
  color: string;
}) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0;

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium text-muted-foreground">{label}</span>
        <span className="font-mono text-xs font-semibold">
          {formatNumber(value)}
        </span>
      </div>
      <div className="relative h-2 w-full overflow-hidden rounded-full bg-muted">
        <div
          className={`h-full rounded-full transition-all ${color}`}
          style={{ width: `${Math.min(pct, 100)}%` }}
        />
      </div>
    </div>
  );
}
