"use client";

import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Puzzle,
  Layers,
  ListTree,
  Plus,
  Eye,
  AlertTriangle,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/shared/kpi-card";
import { DataTable, type ColumnDef, type RowAction } from "@/components/shared/data-table";
import { DomainStatusBadge, getPartTypeVariant, getVersionStatusVariant } from "@/components/shared/domain-status-badge";
import { SkeletonCard } from "@/components/shared/skeleton-variants";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
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
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card } from "@/components/ui/card";

import { useParts, usePartMutations } from "@/hooks/api/use-bom";
import type { PartResponse, CreatePartRequest } from "@/types/api";
import { formatDate, formatNumber, humanizeStatus } from "@/lib/format";
import { toast } from "sonner";

// ─── Schemas ────────────────────────────────────────────

const partSchema = z.object({
  partNumber: z.string().min(1, "Teilenummer ist erforderlich"),
  name: z.string().min(1, "Name ist erforderlich"),
  description: z.string().optional(),
  type: z.string().min(1, "Typ ist erforderlich"),
});
type PartFormValues = z.infer<typeof partSchema>;

// ─── Part Columns ──────────────────────────────────────

const partColumns: ColumnDef<PartResponse>[] = [
  {
    id: "partNumber",
    header: "Teil-Nr.",
    accessorKey: "partNumber",
    cell: (row) => (
      <span className="font-mono text-xs font-medium text-primary">
        {row.partNumber}
      </span>
    ),
  },
  {
    id: "name",
    header: "Name",
    accessorKey: "name",
    cell: (row) => <span className="font-medium">{row.name}</span>,
  },
  {
    id: "type",
    header: "Typ",
    cell: (row) => (
      <DomainStatusBadge variant={getPartTypeVariant(row.type)}>
        {humanizeStatus(row.type)}
      </DomainStatusBadge>
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
    id: "createdAt",
    header: "Erstellt",
    cell: (row) => (
      <span className="font-mono text-xs text-muted-foreground">
        {formatDate(row.createdAt)}
      </span>
    ),
  },
];

// ─── Component ──────────────────────────────────────────

export default function PartsAndProcessesPage() {
  const router = useRouter();
  const { data: parts, loading: partsLoading, error: partsError, refetch: refetchParts } = useParts();
  const mutations = usePartMutations();

  const [partDialogOpen, setPartDialogOpen] = useState(false);
  const [typeFilter, setTypeFilter] = useState<string>("all");

  const partForm = useForm<PartFormValues>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(partSchema) as any,
    defaultValues: {
      partNumber: `PRT-${Date.now().toString(36).toUpperCase()}`,
      name: "",
      description: "",
      type: "PRODUCT",
    },
  });

  // ─── KPI Data ──────────────────────────
  const totalParts = parts?.length ?? 0;
  const productCount = parts?.filter((p) => p.type === "PRODUCT").length ?? 0;
  const componentCount = parts?.filter((p) => p.type === "COMPONENT").length ?? 0;
  const rawMaterialCount = parts?.filter((p) => p.type === "RAW_MATERIAL").length ?? 0;

  // ─── Filtered Parts ────────────────────
  const filteredParts = useMemo(() => {
    if (!parts) return [];
    if (typeFilter === "all") return parts;
    return parts.filter((p) => p.type === typeFilter);
  }, [parts, typeFilter]);

  // ─── Handlers ──────────────────────────
  const handleCreatePart = async (values: PartFormValues) => {
    const req: CreatePartRequest = {
      partNumber: values.partNumber,
      name: values.name,
      description: values.description || undefined,
      type: values.type,
    };
    try {
      const result = await mutations.createPart(req);
      if (result) {
        toast.success("Teil erfolgreich erstellt");
        setPartDialogOpen(false);
        partForm.reset({
          partNumber: `PRT-${Date.now().toString(36).toUpperCase()}`,
          name: "",
          description: "",
          type: "PRODUCT",
        });
        refetchParts();
      }
    } catch (err) {
      toast.error("Teil konnte nicht erstellt werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
    }
  };

  const partRowActions: RowAction<PartResponse>[] = [
    {
      label: "Details anzeigen",
      icon: <Eye className="h-3.5 w-3.5" />,
      onClick: (row) => router.push(`/parts-and-processes/parts/${row.id}`),
    },
  ];

  const typeFilterSlot = (
    <Select value={typeFilter} onValueChange={setTypeFilter}>
      <SelectTrigger size="sm" className="w-40">
        <SelectValue placeholder="Alle Typen" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="all">Alle Typen</SelectItem>
        <SelectItem value="PRODUCT">Produkt</SelectItem>
        <SelectItem value="COMPONENT">Komponente</SelectItem>
        <SelectItem value="RAW_MATERIAL">Rohmaterial</SelectItem>
      </SelectContent>
    </Select>
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title="Teile & Prozesse"
        description="Teile, Stücklisten und Arbeitspläne verwalten"
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {partsLoading ? (
          <>
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </>
        ) : (
          <>
            <KpiCard
              label="Teile gesamt"
              value={formatNumber(totalParts)}
              trend={totalParts > 0 ? { direction: "neutral", value: `${totalParts} erfasst` } : undefined}
            />
            <KpiCard
              label="Produkte"
              value={formatNumber(productCount)}
              trend={{ direction: "neutral", value: "Fertigteile" }}
            />
            <KpiCard
              label="Komponenten"
              value={formatNumber(componentCount)}
              trend={{ direction: "neutral", value: "Baugruppen" }}
            />
            <KpiCard
              label="Rohmaterialien"
              value={formatNumber(rawMaterialCount)}
              trend={{ direction: "neutral", value: "Grundmaterialien" }}
            />
          </>
        )}
      </div>

      {/* Tabs */}
      <Tabs defaultValue="parts">
        <TabsList>
          <TabsTrigger value="parts" className="gap-1.5">
            <Puzzle className="h-3.5 w-3.5" />
            Teile
          </TabsTrigger>
          <TabsTrigger value="bom" className="gap-1.5">
            <Layers className="h-3.5 w-3.5" />
            Stücklisten
          </TabsTrigger>
          <TabsTrigger value="processes" className="gap-1.5">
            <ListTree className="h-3.5 w-3.5" />
            Arbeitspläne
          </TabsTrigger>
        </TabsList>

        {/* ─── Parts Tab ─── */}
        <TabsContent value="parts" className="mt-4">
          {partsError ? (
            <div className="flex flex-col items-center gap-3 py-12 text-center">
              <AlertTriangle className="h-8 w-8 text-destructive/60" />
              <p className="text-sm text-muted-foreground">{partsError}</p>
              <Button variant="outline" size="sm" onClick={refetchParts}>
                Erneut versuchen
              </Button>
            </div>
          ) : (
            <DataTable<PartResponse>
              data={filteredParts}
              columns={partColumns}
              searchPlaceholder="Teile suchen..."
              searchKey="name"
              filterSlots={
                <>
                  {typeFilterSlot}
                  <Button
                    size="sm"
                    className="gap-1.5"
                    onClick={() => setPartDialogOpen(true)}
                  >
                    <Plus className="h-3.5 w-3.5" />
                    Neues Teil
                  </Button>
                </>
              }
              rowActions={partRowActions}
              onRowClick={(row) => router.push(`/parts-and-processes/parts/${row.id}`)}
              loading={partsLoading}
              emptyState={{
                icon: <Puzzle className="h-8 w-8 text-muted-foreground/40" />,
                title: "Noch keine Teile",
                description: "Erstellen Sie Ihr erstes Teil, um mit Stücklisten und Arbeitsplänen zu beginnen.",
                action: (
                  <Button
                    size="sm"
                    variant="outline"
                    className="mt-2"
                    onClick={() => setPartDialogOpen(true)}
                  >
                    <Plus className="mr-1.5 h-3.5 w-3.5" />
                    Neues Teil
                  </Button>
                ),
              }}
            />
          )}
        </TabsContent>

        {/* ─── BOM Tab ─── */}
        <TabsContent value="bom" className="mt-4">
          <Card className="relative overflow-hidden p-6">
            <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
            <div className="flex flex-col items-center gap-3 py-8 text-center">
              <Layers className="h-10 w-10 text-muted-foreground/30" />
              <h3 className="text-sm font-medium text-foreground/70">
                Stücklisten
              </h3>
              <p className="max-w-sm text-xs text-muted-foreground">
                Wählen Sie ein Teil im Teile-Tab, um dessen Stückliste anzuzeigen und zu verwalten.
                Jedes Teil kann mehrere Stücklistenversionen mit verschiedenen Komponentenkonfigurationen haben.
              </p>
            </div>
          </Card>
        </TabsContent>

        {/* ─── Process Plans Tab ─── */}
        <TabsContent value="processes" className="mt-4">
          <Card className="relative overflow-hidden p-6">
            <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
            <div className="flex flex-col items-center gap-3 py-8 text-center">
              <ListTree className="h-10 w-10 text-muted-foreground/30" />
              <h3 className="text-sm font-medium text-foreground/70">
                Arbeitspläne
              </h3>
              <p className="max-w-sm text-xs text-muted-foreground">
                Wählen Sie ein Teil im Teile-Tab, um dessen Arbeitspläne anzuzeigen und zu verwalten.
                Arbeitspläne definieren die Fertigungsschritte, Stationen und Zeitschätzungen.
              </p>
            </div>
          </Card>
        </TabsContent>
      </Tabs>

      {/* ─── New Part Dialog ─── */}
      <Dialog open={partDialogOpen} onOpenChange={setPartDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Neues Teil</DialogTitle>
            <DialogDescription>
              Neues Teil für Stücklisten- und Arbeitsplanverwaltung erstellen.
            </DialogDescription>
          </DialogHeader>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          <form onSubmit={partForm.handleSubmit(handleCreatePart as any)} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="partNumber">Teilenummer</Label>
              <Input
                id="partNumber"
                className="font-mono"
                {...partForm.register("partNumber")}
              />
              {partForm.formState.errors.partNumber && (
                <p className="text-xs text-destructive">
                  {partForm.formState.errors.partNumber.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="partName">Name</Label>
              <Input id="partName" {...partForm.register("name")} />
              {partForm.formState.errors.name && (
                <p className="text-xs text-destructive">
                  {partForm.formState.errors.name.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="partDesc">Beschreibung</Label>
              <Textarea
                id="partDesc"
                rows={2}
                {...partForm.register("description")}
              />
            </div>
            <div className="space-y-2">
              <Label>Typ</Label>
              <Select
                value={partForm.watch("type")}
                onValueChange={(v) => partForm.setValue("type", v)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="PRODUCT">Produkt</SelectItem>
                  <SelectItem value="COMPONENT">Komponente</SelectItem>
                  <SelectItem value="RAW_MATERIAL">Rohmaterial</SelectItem>
                </SelectContent>
              </Select>
              {partForm.formState.errors.type && (
                <p className="text-xs text-destructive">
                  {partForm.formState.errors.type.message}
                </p>
              )}
            </div>
            {mutations.error && (
              <p className="text-xs text-destructive">{mutations.error}</p>
            )}
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setPartDialogOpen(false)}
              >
                Abbrechen
              </Button>
              <Button type="submit" size="sm" disabled={mutations.loading}>
                {mutations.loading ? "Wird erstellt..." : "Teil erstellen"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
