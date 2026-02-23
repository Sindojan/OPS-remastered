"use client";

import { useState, useCallback } from "react";
import { useRouter, useParams } from "next/navigation";
import {
  ArrowLeft,
  AlertTriangle,
  Truck,
  Pencil,
  Save,
  X,
  Mail,
  Phone,
  MapPin,
  FileText,
  User,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import { DomainStatusBadge } from "@/components/shared/domain-status-badge";
import { SkeletonCard, SkeletonTable } from "@/components/shared/skeleton-variants";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

import {
  useSupplier,
  useSupplierArticles,
  useInventoryMutations,
} from "@/hooks/api/use-inventory";
import type { SupplierArticleResponse } from "@/types/api";
import { formatNumber, humanizeStatus } from "@/lib/format";

// ─── Status Helpers ─────────────────────────────────────

function getSupplierStatusVariant(status: string) {
  switch (status) {
    case "ACTIVE":
      return "success" as const;
    case "INACTIVE":
      return "neutral" as const;
    case "BLOCKED":
      return "error" as const;
    default:
      return "neutral" as const;
  }
}

// ─── Supplier Article Columns ───────────────────────────

const supplierArticleColumns: ColumnDef<SupplierArticleResponse>[] = [
  {
    id: "supplierArticleNumber",
    header: "Supplier Article #",
    cell: (row) => (
      <span className="font-mono text-xs font-medium text-primary">
        {row.supplierArticleNumber ?? "\u2013"}
      </span>
    ),
  },
  {
    id: "articleId",
    header: "Article ID",
    cell: (row) => (
      <span className="font-mono text-xs text-muted-foreground">
        {row.articleId.slice(0, 8)}...
      </span>
    ),
  },
  {
    id: "leadTimeDays",
    header: "Lead Time",
    cell: (row) => (
      <span className="font-mono text-xs">
        {row.leadTimeDays != null ? `${row.leadTimeDays}d` : "\u2013"}
      </span>
    ),
  },
  {
    id: "minOrderQuantity",
    header: "Min Order Qty",
    cell: (row) => (
      <span className="font-mono text-xs">
        {formatNumber(row.minOrderQuantity)}
      </span>
    ),
  },
];

// ─── Component ──────────────────────────────────────────

export default function SupplierDetailPage() {
  const router = useRouter();
  const params = useParams();
  const supplierId = params.id as string;

  const { data: supplier, loading, error, refetch } = useSupplier(supplierId);
  const { data: supplierArticles, loading: articlesLoading } = useSupplierArticles(supplierId);
  const mutations = useInventoryMutations();

  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState("");
  const [editContactName, setEditContactName] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editPhone, setEditPhone] = useState("");
  const [editAddress, setEditAddress] = useState("");
  const [editTaxId, setEditTaxId] = useState("");

  const startEditing = useCallback(() => {
    if (!supplier) return;
    setEditName(supplier.name);
    setEditContactName(supplier.contactName ?? "");
    setEditEmail(supplier.email ?? "");
    setEditPhone(supplier.phone ?? "");
    setEditAddress(supplier.address ?? "");
    setEditTaxId(supplier.taxId ?? "");
    setIsEditing(true);
  }, [supplier]);

  const handleSaveEdit = async () => {
    if (!supplier) return;
    const result = await mutations.updateSupplier(supplier.id, {
      name: editName,
      contactName: editContactName || undefined,
      email: editEmail || undefined,
      phone: editPhone || undefined,
      address: editAddress || undefined,
      taxId: editTaxId || undefined,
    });
    if (result) {
      setIsEditing(false);
      refetch();
    }
  };

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
        <SkeletonTable />
      </div>
    );
  }

  // ─── Error ─────────────────────────────
  if (error || !supplier) {
    return (
      <div className="flex flex-col items-center gap-4 py-16">
        <AlertTriangle className="h-10 w-10 text-destructive/60" />
        <p className="text-sm text-muted-foreground">{error ?? "Supplier not found"}</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => router.push("/inventory")}>
            <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
            Back to Inventory
          </Button>
          <Button variant="outline" size="sm" onClick={refetch}>
            Retry
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={supplier.name}
        breadcrumb={["Inventory", "Suppliers", supplier.name]}
        actions={
          <Button
            variant="outline"
            size="sm"
            onClick={() => router.push("/inventory")}
          >
            <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
            Back
          </Button>
        }
      />

      {/* ─── Key Data ─── */}
      <Card className="relative overflow-hidden p-6">
        <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
            Supplier Details
          </h2>
          {!isEditing ? (
            <Button variant="outline" size="sm" className="gap-1.5" onClick={startEditing}>
              <Pencil className="h-3 w-3" />
              Edit
            </Button>
          ) : (
            <div className="flex gap-2">
              <Button variant="outline" size="sm" className="gap-1.5" onClick={() => setIsEditing(false)}>
                <X className="h-3 w-3" />
                Cancel
              </Button>
              <Button size="sm" className="gap-1.5" onClick={handleSaveEdit} disabled={mutations.loading}>
                <Save className="h-3 w-3" />
                {mutations.loading ? "Saving..." : "Save"}
              </Button>
            </div>
          )}
        </div>

        {isEditing ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground">Name</Label>
              <Input value={editName} onChange={(e) => setEditName(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground">Tax ID</Label>
              <Input
                value={editTaxId}
                onChange={(e) => setEditTaxId(e.target.value)}
                className="font-mono"
              />
            </div>
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground">Contact Name</Label>
              <Input
                value={editContactName}
                onChange={(e) => setEditContactName(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground">Email</Label>
              <Input
                type="email"
                value={editEmail}
                onChange={(e) => setEditEmail(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground">Phone</Label>
              <Input
                value={editPhone}
                onChange={(e) => setEditPhone(e.target.value)}
              />
            </div>
            <div className="col-span-full space-y-2">
              <Label className="text-xs text-muted-foreground">Address</Label>
              <Textarea
                value={editAddress}
                onChange={(e) => setEditAddress(e.target.value)}
                rows={2}
              />
            </div>
            {mutations.error && (
              <p className="col-span-full text-xs text-destructive">{mutations.error}</p>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <DetailField label="Name" value={supplier.name} icon={<Truck className="h-3.5 w-3.5" />} />
            <DetailField label="Tax ID" value={supplier.taxId ?? "\u2013"} icon={<FileText className="h-3.5 w-3.5" />} mono />
            <DetailField label="Contact Name" value={supplier.contactName ?? "\u2013"} icon={<User className="h-3.5 w-3.5" />} />
            <DetailField label="Email" value={supplier.email ?? "\u2013"} icon={<Mail className="h-3.5 w-3.5" />} />
            <DetailField label="Phone" value={supplier.phone ?? "\u2013"} icon={<Phone className="h-3.5 w-3.5" />} mono />
            <DetailField label="Address" value={supplier.address ?? "\u2013"} icon={<MapPin className="h-3.5 w-3.5" />} />
            <div className="space-y-1">
              <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                Status
              </p>
              <DomainStatusBadge variant={getSupplierStatusVariant(supplier.status)}>
                {humanizeStatus(supplier.status)}
              </DomainStatusBadge>
            </div>
          </div>
        )}
      </Card>

      {/* ─── Supplier Articles ─── */}
      <div className="space-y-3">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
          Supplier Articles
        </h2>
        {articlesLoading ? (
          <SkeletonTable rows={3} columns={4} />
        ) : (
          <DataTable<SupplierArticleResponse>
            data={supplierArticles ?? []}
            columns={supplierArticleColumns}
            searchPlaceholder="Search articles..."
            emptyState={{
              icon: <Truck className="h-8 w-8 text-muted-foreground/40" />,
              title: "No articles linked",
              description: "This supplier has no articles linked yet.",
            }}
          />
        )}
      </div>
    </div>
  );
}

// ─── Helper Components ──────────────────────────────────

function DetailField({
  label,
  value,
  icon,
  mono,
}: {
  label: string;
  value: string;
  icon?: React.ReactNode;
  mono?: boolean;
}) {
  return (
    <div className="space-y-1">
      <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
        {label}
      </p>
      <p className={`flex items-center gap-1.5 text-sm ${mono ? "font-mono" : ""}`}>
        {icon && <span className="text-muted-foreground">{icon}</span>}
        {value}
      </p>
    </div>
  );
}
