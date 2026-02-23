"use client";

import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Package,
  AlertTriangle,
  ArrowDownToLine,
  Plus,
  Eye,
  Truck,
  User,
  Phone,
  Mail,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/shared/kpi-card";
import { DataTable, type ColumnDef, type RowAction } from "@/components/shared/data-table";
import { DomainStatusBadge } from "@/components/shared/domain-status-badge";
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

import {
  useArticles,
  useSuppliers,
  useCriticalArticles,
  useInventoryMutations,
} from "@/hooks/api/use-inventory";
import type {
  ArticleResponse,
  SupplierResponse,
  CreateArticleRequest,
  CreateSupplierRequest,
} from "@/types/api";
import { formatNumber, humanizeStatus } from "@/lib/format";

// ─── Article Status Helpers ─────────────────────────────

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

// ─── Schemas ────────────────────────────────────────────

const articleSchema = z.object({
  articleNumber: z.string().min(1, "Article number is required"),
  name: z.string().min(1, "Name is required"),
  description: z.string().optional(),
  minStock: z.number().min(0).optional(),
  reorderPoint: z.number().min(0).optional(),
  status: z.string().default("ACTIVE"),
});
type ArticleFormValues = z.infer<typeof articleSchema>;

const supplierSchema = z.object({
  name: z.string().min(1, "Name is required"),
  contactName: z.string().optional(),
  email: z.string().email("Invalid email").optional().or(z.literal("")),
  phone: z.string().optional(),
  address: z.string().optional(),
  taxId: z.string().optional(),
});
type SupplierFormValues = z.infer<typeof supplierSchema>;

// ─── Article Columns ────────────────────────────────────

const articleColumns: ColumnDef<ArticleResponse>[] = [
  {
    id: "articleNumber",
    header: "Article #",
    accessorKey: "articleNumber",
    cell: (row) => (
      <span className="font-mono text-xs font-medium text-primary">
        {row.articleNumber}
      </span>
    ),
  },
  {
    id: "name",
    header: "Name",
    accessorKey: "name",
  },
  {
    id: "category",
    header: "Category",
    cell: (row) => (
      <span className="text-muted-foreground text-xs">
        {row.categoryId ? row.categoryId.slice(0, 8) + "..." : "\u2013"}
      </span>
    ),
    sortable: false,
  },
  {
    id: "unit",
    header: "Unit",
    cell: (row) => (
      <span className="text-muted-foreground text-xs">
        {row.unitId ? row.unitId.slice(0, 8) + "..." : "\u2013"}
      </span>
    ),
    sortable: false,
  },
  {
    id: "minStock",
    header: "Min Stock",
    cell: (row) => (
      <span className="font-mono text-xs">{formatNumber(row.minStock)}</span>
    ),
  },
  {
    id: "reorderPoint",
    header: "Reorder Point",
    cell: (row) => (
      <span className="font-mono text-xs">{formatNumber(row.reorderPoint)}</span>
    ),
  },
  {
    id: "status",
    header: "Status",
    cell: (row) => (
      <DomainStatusBadge variant={getArticleStatusVariant(row.status)}>
        {humanizeStatus(row.status)}
      </DomainStatusBadge>
    ),
  },
];

// ─── Supplier Columns ───────────────────────────────────

const supplierColumns: ColumnDef<SupplierResponse>[] = [
  {
    id: "name",
    header: "Name",
    accessorKey: "name",
    cell: (row) => <span className="font-medium">{row.name}</span>,
  },
  {
    id: "contactName",
    header: "Contact",
    cell: (row) => (
      <span className="flex items-center gap-1.5 text-sm">
        {row.contactName ? (
          <>
            <User className="h-3 w-3 text-muted-foreground" />
            {row.contactName}
          </>
        ) : (
          <span className="text-muted-foreground">{"\u2013"}</span>
        )}
      </span>
    ),
  },
  {
    id: "email",
    header: "Email",
    cell: (row) => (
      <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
        {row.email ? (
          <>
            <Mail className="h-3 w-3" />
            {row.email}
          </>
        ) : (
          "\u2013"
        )}
      </span>
    ),
  },
  {
    id: "phone",
    header: "Phone",
    cell: (row) => (
      <span className="flex items-center gap-1.5 font-mono text-xs text-muted-foreground">
        {row.phone ? (
          <>
            <Phone className="h-3 w-3" />
            {row.phone}
          </>
        ) : (
          "\u2013"
        )}
      </span>
    ),
  },
  {
    id: "status",
    header: "Status",
    cell: (row) => (
      <DomainStatusBadge variant={getArticleStatusVariant(row.status)}>
        {humanizeStatus(row.status)}
      </DomainStatusBadge>
    ),
  },
];

// ─── Component ──────────────────────────────────────────

export default function InventoryPage() {
  const router = useRouter();
  const { data: articles, loading: articlesLoading, error: articlesError, refetch: refetchArticles } = useArticles();
  const { data: suppliers, loading: suppliersLoading, error: suppliersError, refetch: refetchSuppliers } = useSuppliers();
  const { data: criticalArticles } = useCriticalArticles();
  const mutations = useInventoryMutations();

  const [articleDialogOpen, setArticleDialogOpen] = useState(false);
  const [supplierDialogOpen, setSupplierDialogOpen] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>("all");

  // ─── Article Form ───────────────────────
  const articleForm = useForm<ArticleFormValues>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(articleSchema) as any,
    defaultValues: {
      articleNumber: `ART-${Date.now().toString(36).toUpperCase()}`,
      name: "",
      description: "",
      minStock: 0,
      reorderPoint: 0,
      status: "ACTIVE",
    },
  });

  // ─── Supplier Form ─────────────────────
  const supplierForm = useForm<SupplierFormValues>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(supplierSchema) as any,
    defaultValues: {
      name: "",
      contactName: "",
      email: "",
      phone: "",
      address: "",
      taxId: "",
    },
  });

  // ─── KPI Data ──────────────────────────
  const totalArticles = articles?.length ?? 0;
  const criticalCount = criticalArticles?.length ?? 0;

  // ─── Filtered Articles ──────────────────
  const filteredArticles = useMemo(() => {
    if (!articles) return [];
    if (statusFilter === "all") return articles;
    return articles.filter((a) => a.status === statusFilter);
  }, [articles, statusFilter]);

  // ─── Handlers ──────────────────────────
  const handleCreateArticle = async (values: ArticleFormValues) => {
    const req: CreateArticleRequest = {
      articleNumber: values.articleNumber,
      name: values.name,
      description: values.description || undefined,
      minStock: values.minStock,
      reorderPoint: values.reorderPoint,
      status: values.status,
    };
    const result = await mutations.createArticle(req);
    if (result) {
      setArticleDialogOpen(false);
      articleForm.reset({
        articleNumber: `ART-${Date.now().toString(36).toUpperCase()}`,
        name: "",
        description: "",
        minStock: 0,
        reorderPoint: 0,
        status: "ACTIVE",
      });
      refetchArticles();
    }
  };

  const handleCreateSupplier = async (values: SupplierFormValues) => {
    const req: CreateSupplierRequest = {
      name: values.name,
      contactName: values.contactName || undefined,
      email: values.email || undefined,
      phone: values.phone || undefined,
      address: values.address || undefined,
      taxId: values.taxId || undefined,
    };
    const result = await mutations.createSupplier(req);
    if (result) {
      setSupplierDialogOpen(false);
      supplierForm.reset();
      refetchSuppliers();
    }
  };

  const articleRowActions: RowAction<ArticleResponse>[] = [
    {
      label: "View Detail",
      icon: <Eye className="h-3.5 w-3.5" />,
      onClick: (row) => router.push(`/inventory/articles/${row.id}`),
    },
    {
      label: "Book Movement",
      icon: <ArrowDownToLine className="h-3.5 w-3.5" />,
      onClick: (row) => router.push(`/inventory/articles/${row.id}?tab=movements`),
    },
  ];

  const supplierRowActions: RowAction<SupplierResponse>[] = [
    {
      label: "View Detail",
      icon: <Eye className="h-3.5 w-3.5" />,
      onClick: (row) => router.push(`/inventory/suppliers/${row.id}`),
    },
  ];

  // ─── Status filter slot ────────────────
  const articleFilterSlot = (
    <Select value={statusFilter} onValueChange={setStatusFilter}>
      <SelectTrigger size="sm" className="w-36">
        <SelectValue placeholder="All Statuses" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="all">All Statuses</SelectItem>
        <SelectItem value="ACTIVE">Active</SelectItem>
        <SelectItem value="INACTIVE">Inactive</SelectItem>
        <SelectItem value="DISCONTINUED">Discontinued</SelectItem>
      </SelectContent>
    </Select>
  );

  const kpiLoading = articlesLoading;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Inventory"
        description="Articles, stock levels, and supplier management"
      />

      {/* Critical Articles Banner */}
      {criticalCount > 0 && (
        <div className="flex items-center gap-3 rounded-lg border border-amber-500/20 bg-amber-500/5 px-4 py-3">
          <AlertTriangle className="h-4 w-4 shrink-0 text-amber-500" />
          <p className="text-sm text-amber-600 dark:text-amber-400">
            <span className="font-mono font-semibold">{criticalCount}</span>{" "}
            article{criticalCount !== 1 ? "s" : ""} below minimum stock level.
            Review and reorder to prevent shortages.
          </p>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {kpiLoading ? (
          <>
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </>
        ) : (
          <>
            <KpiCard
              label="Total Articles"
              value={formatNumber(totalArticles)}
              trend={
                totalArticles > 0
                  ? { direction: "neutral", value: `${totalArticles} tracked` }
                  : undefined
              }
            />
            <KpiCard
              label="Critical Articles"
              value={formatNumber(criticalCount)}
              trend={
                criticalCount > 0
                  ? { direction: "down", value: "Below min stock" }
                  : { direction: "up", value: "All OK" }
              }
            />
            <KpiCard
              label="Movements Today"
              value={"\u2013"}
              trend={{ direction: "neutral", value: "Tracking pending" }}
            />
            <KpiCard
              label="Total Value"
              value={"\u2013"}
              trend={{ direction: "neutral", value: "Valuation pending" }}
            />
          </>
        )}
      </div>

      {/* Tabs */}
      <Tabs defaultValue="articles">
        <TabsList>
          <TabsTrigger value="articles" className="gap-1.5">
            <Package className="h-3.5 w-3.5" />
            Articles
          </TabsTrigger>
          <TabsTrigger value="suppliers" className="gap-1.5">
            <Truck className="h-3.5 w-3.5" />
            Suppliers
          </TabsTrigger>
        </TabsList>

        {/* ─── Articles Tab ─── */}
        <TabsContent value="articles" className="mt-4">
          {articlesError ? (
            <div className="flex flex-col items-center gap-3 py-12 text-center">
              <AlertTriangle className="h-8 w-8 text-destructive/60" />
              <p className="text-sm text-muted-foreground">{articlesError}</p>
              <Button variant="outline" size="sm" onClick={refetchArticles}>
                Retry
              </Button>
            </div>
          ) : (
            <DataTable<ArticleResponse>
              data={filteredArticles}
              columns={articleColumns}
              searchPlaceholder="Search articles..."
              searchKey="name"
              filterSlots={
                <>
                  {articleFilterSlot}
                  <Button
                    size="sm"
                    className="gap-1.5"
                    onClick={() => setArticleDialogOpen(true)}
                  >
                    <Plus className="h-3.5 w-3.5" />
                    New Article
                  </Button>
                </>
              }
              rowActions={articleRowActions}
              onRowClick={(row) => router.push(`/inventory/articles/${row.id}`)}
              loading={articlesLoading}
              emptyState={{
                icon: <Package className="h-8 w-8 text-muted-foreground/40" />,
                title: "No articles yet",
                description: "Create your first article to start tracking inventory.",
                action: (
                  <Button
                    size="sm"
                    variant="outline"
                    className="mt-2"
                    onClick={() => setArticleDialogOpen(true)}
                  >
                    <Plus className="mr-1.5 h-3.5 w-3.5" />
                    New Article
                  </Button>
                ),
              }}
            />
          )}
        </TabsContent>

        {/* ─── Suppliers Tab ─── */}
        <TabsContent value="suppliers" className="mt-4">
          {suppliersError ? (
            <div className="flex flex-col items-center gap-3 py-12 text-center">
              <AlertTriangle className="h-8 w-8 text-destructive/60" />
              <p className="text-sm text-muted-foreground">{suppliersError}</p>
              <Button variant="outline" size="sm" onClick={refetchSuppliers}>
                Retry
              </Button>
            </div>
          ) : (
            <DataTable<SupplierResponse>
              data={suppliers ?? []}
              columns={supplierColumns}
              searchPlaceholder="Search suppliers..."
              searchKey="name"
              filterSlots={
                <Button
                  size="sm"
                  className="gap-1.5"
                  onClick={() => setSupplierDialogOpen(true)}
                >
                  <Plus className="h-3.5 w-3.5" />
                  New Supplier
                </Button>
              }
              rowActions={supplierRowActions}
              onRowClick={(row) => router.push(`/inventory/suppliers/${row.id}`)}
              loading={suppliersLoading}
              emptyState={{
                icon: <Truck className="h-8 w-8 text-muted-foreground/40" />,
                title: "No suppliers yet",
                description: "Add suppliers to manage your supply chain.",
                action: (
                  <Button
                    size="sm"
                    variant="outline"
                    className="mt-2"
                    onClick={() => setSupplierDialogOpen(true)}
                  >
                    <Plus className="mr-1.5 h-3.5 w-3.5" />
                    New Supplier
                  </Button>
                ),
              }}
            />
          )}
        </TabsContent>
      </Tabs>

      {/* ─── New Article Dialog ─── */}
      <Dialog open={articleDialogOpen} onOpenChange={setArticleDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>New Article</DialogTitle>
            <DialogDescription>
              Add a new article to the inventory system.
            </DialogDescription>
          </DialogHeader>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          <form onSubmit={articleForm.handleSubmit(handleCreateArticle as any)} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="articleNumber">Article Number</Label>
              <Input
                id="articleNumber"
                className="font-mono"
                {...articleForm.register("articleNumber")}
              />
              {articleForm.formState.errors.articleNumber && (
                <p className="text-xs text-destructive">
                  {articleForm.formState.errors.articleNumber.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="articleName">Name</Label>
              <Input id="articleName" {...articleForm.register("name")} />
              {articleForm.formState.errors.name && (
                <p className="text-xs text-destructive">
                  {articleForm.formState.errors.name.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="articleDesc">Description</Label>
              <Textarea
                id="articleDesc"
                rows={2}
                {...articleForm.register("description")}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="minStock">Min Stock</Label>
                <Input
                  id="minStock"
                  type="number"
                  min={0}
                  className="font-mono"
                  {...articleForm.register("minStock", { valueAsNumber: true })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="reorderPoint">Reorder Point</Label>
                <Input
                  id="reorderPoint"
                  type="number"
                  min={0}
                  className="font-mono"
                  {...articleForm.register("reorderPoint", { valueAsNumber: true })}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Status</Label>
              <Select
                value={articleForm.watch("status")}
                onValueChange={(v) => articleForm.setValue("status", v)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ACTIVE">Active</SelectItem>
                  <SelectItem value="INACTIVE">Inactive</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {mutations.error && (
              <p className="text-xs text-destructive">{mutations.error}</p>
            )}
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setArticleDialogOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit" size="sm" disabled={mutations.loading}>
                {mutations.loading ? "Creating..." : "Create Article"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* ─── New Supplier Dialog ─── */}
      <Dialog open={supplierDialogOpen} onOpenChange={setSupplierDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>New Supplier</DialogTitle>
            <DialogDescription>
              Add a new supplier to your supply chain.
            </DialogDescription>
          </DialogHeader>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          <form onSubmit={supplierForm.handleSubmit(handleCreateSupplier as any)} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="supplierName">Name</Label>
              <Input id="supplierName" {...supplierForm.register("name")} />
              {supplierForm.formState.errors.name && (
                <p className="text-xs text-destructive">
                  {supplierForm.formState.errors.name.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="contactName">Contact Name</Label>
              <Input id="contactName" {...supplierForm.register("contactName")} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="supplierEmail">Email</Label>
                <Input id="supplierEmail" type="email" {...supplierForm.register("email")} />
                {supplierForm.formState.errors.email && (
                  <p className="text-xs text-destructive">
                    {supplierForm.formState.errors.email.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="supplierPhone">Phone</Label>
                <Input id="supplierPhone" {...supplierForm.register("phone")} />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="supplierAddress">Address</Label>
              <Textarea
                id="supplierAddress"
                rows={2}
                {...supplierForm.register("address")}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="taxId">Tax ID</Label>
              <Input id="taxId" className="font-mono" {...supplierForm.register("taxId")} />
            </div>
            {mutations.error && (
              <p className="text-xs text-destructive">{mutations.error}</p>
            )}
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setSupplierDialogOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit" size="sm" disabled={mutations.loading}>
                {mutations.loading ? "Creating..." : "Create Supplier"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
