"use client";

import { useState, useMemo, useCallback, useEffect } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Building2, Plus, Eye, Ban, RotateCcw, Trash2, Copy, Check, EyeOff } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/shared/kpi-card";
import { DataTable, type ColumnDef, type RowAction } from "@/components/shared/data-table";
import { DomainStatusBadge } from "@/components/shared/domain-status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
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
import { useApi, useMutation } from "@/hooks/api/use-api";
import { formatDate } from "@/lib/format";
import type {
  CompanyResponse,
  CompanyCreateRequest,
  CompanyCreateResponse,
  CompanyPlan,
  CompanyStatus,
} from "@/types/api";

function slugify(value: string): string {
  return value
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-");
}

function getPlanVariant(plan: CompanyPlan) {
  switch (plan) {
    case "PROFESSIONAL":
      return "info" as const;
    case "ENTERPRISE":
      return "warning" as const;
    default:
      return "neutral" as const;
  }
}

function getCompanyStatusVariant(status: CompanyStatus) {
  switch (status) {
    case "ACTIVE":
      return "success" as const;
    case "SUSPENDED":
      return "error" as const;
    case "DELETED":
      return "neutral" as const;
    default:
      return "neutral" as const;
  }
}

// ─── Create Company Modal ──────────────────────────────

interface CreateCompanyModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

function CreateCompanyModal({ open, onClose, onSuccess }: CreateCompanyModalProps) {
  const { mutate, loading } = useMutation<CompanyCreateRequest, CompanyCreateResponse>();
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [slugManual, setSlugManual] = useState(false);
  const [plan, setPlan] = useState<CompanyPlan>("BASIC");
  const [adminEmail, setAdminEmail] = useState("");
  const [adminFirstName, setAdminFirstName] = useState("");
  const [adminLastName, setAdminLastName] = useState("");
  const [adminPassword, setAdminPassword] = useState("");
  const [randomPassword, setRandomPassword] = useState(true);
  const [showPassword, setShowPassword] = useState(false);

  // Result state
  const [result, setResult] = useState<CompanyCreateResponse | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!slugManual) {
      setSlug(slugify(name));
    }
  }, [name, slugManual]);

  const resetForm = useCallback(() => {
    setName("");
    setSlug("");
    setSlugManual(false);
    setPlan("BASIC");
    setAdminEmail("");
    setAdminFirstName("");
    setAdminLastName("");
    setAdminPassword("");
    setRandomPassword(true);
    setShowPassword(false);
    setResult(null);
    setCopied(false);
  }, []);

  const handleClose = useCallback(() => {
    resetForm();
    onClose();
  }, [resetForm, onClose]);

  const handleSubmit = async () => {
    if (!name.trim() || !slug.trim() || !adminEmail.trim() || !adminFirstName.trim() || !adminLastName.trim()) {
      toast.error("Bitte fuellen Sie alle Pflichtfelder aus.");
      return;
    }

    try {
      const body: CompanyCreateRequest = {
        name: name.trim(),
        slug: slug.trim(),
        plan,
        adminEmail: adminEmail.trim(),
        adminFirstName: adminFirstName.trim(),
        adminLastName: adminLastName.trim(),
      };
      if (!randomPassword && adminPassword.trim()) {
        body.adminPassword = adminPassword.trim();
      }

      const res = await mutate("post", "/api/system/companies", body);
      if (res) {
        setResult(res);
        toast.success("Unternehmen erfolgreich erstellt.");
        onSuccess();
      }
    } catch {
      toast.error("Fehler beim Erstellen des Unternehmens.");
    }
  };

  const handleCopy = () => {
    if (!result) return;
    const text = `Email: ${result.adminEmail}\nPassword: ${result.adminPassword}`;
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>
            {result ? "Unternehmen erstellt" : "Unternehmen erstellen"}
          </DialogTitle>
          <DialogDescription>
            {result
              ? "Speichern Sie die Admin-Zugangsdaten."
              : "Neues Unternehmen mit einem Admin-Konto einrichten."}
          </DialogDescription>
        </DialogHeader>

        {result ? (
          <div className="space-y-4">
            <div className="rounded-lg border bg-muted/50 p-4 space-y-2">
              <div>
                <span className="text-xs font-medium text-muted-foreground">
                  Unternehmen
                </span>
                <p className="text-sm font-medium">{result.name}</p>
              </div>
              <div>
                <span className="text-xs font-medium text-muted-foreground">
                  Slug
                </span>
                <p className="font-mono text-sm">{result.slug}</p>
              </div>
              <div>
                <span className="text-xs font-medium text-muted-foreground">
                  Plan
                </span>
                <p className="text-sm">{result.plan}</p>
              </div>
            </div>
            <div className="rounded-lg border border-primary/20 bg-primary/5 p-4 space-y-2">
              <p className="text-xs font-semibold uppercase tracking-wider text-primary">
                Admin-Zugangsdaten
              </p>
              <div>
                <span className="text-xs font-medium text-muted-foreground">
                  E-Mail
                </span>
                <p className="font-mono text-sm">{result.adminEmail}</p>
              </div>
              <div>
                <span className="text-xs font-medium text-muted-foreground">
                  Passwort
                </span>
                <p className="font-mono text-sm">{result.adminPassword}</p>
              </div>
              <Button
                variant="outline"
                size="sm"
                className="mt-2 gap-1.5"
                onClick={handleCopy}
              >
                {copied ? (
                  <Check className="h-3.5 w-3.5" />
                ) : (
                  <Copy className="h-3.5 w-3.5" />
                )}
                {copied ? "Kopiert" : "Zugangsdaten kopieren"}
              </Button>
            </div>
            <DialogFooter>
              <Button variant="default" size="sm" onClick={handleClose}>
                Fertig
              </Button>
            </DialogFooter>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="company-name">Firmenname *</Label>
              <Input
                id="company-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Acme Manufacturing"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="company-slug">Slug *</Label>
              <Input
                id="company-slug"
                value={slug}
                onChange={(e) => {
                  setSlug(e.target.value);
                  setSlugManual(true);
                }}
                placeholder="acme-manufacturing"
                className="font-mono"
              />
              <p className="text-[11px] text-muted-foreground">
                Automatisch aus dem Namen generiert. Bearbeiten zum Ueberschreiben.
              </p>
            </div>

            <div className="space-y-2">
              <Label>Plan</Label>
              <Select
                value={plan}
                onValueChange={(v) => setPlan(v as CompanyPlan)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="BASIC">Basic</SelectItem>
                  <SelectItem value="PROFESSIONAL">Professional</SelectItem>
                  <SelectItem value="ENTERPRISE">Enterprise</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="border-t pt-4 space-y-2">
              <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Admin-Konto
              </p>
              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1">
                  <Label htmlFor="admin-first">Vorname *</Label>
                  <Input
                    id="admin-first"
                    value={adminFirstName}
                    onChange={(e) => setAdminFirstName(e.target.value)}
                  />
                </div>
                <div className="space-y-1">
                  <Label htmlFor="admin-last">Nachname *</Label>
                  <Input
                    id="admin-last"
                    value={adminLastName}
                    onChange={(e) => setAdminLastName(e.target.value)}
                  />
                </div>
              </div>
              <div className="space-y-1">
                <Label htmlFor="admin-email">Email *</Label>
                <Input
                  id="admin-email"
                  type="email"
                  value={adminEmail}
                  onChange={(e) => setAdminEmail(e.target.value)}
                />
              </div>

              <div className="flex items-center gap-2 pt-1">
                <Checkbox
                  id="random-pw"
                  checked={randomPassword}
                  onCheckedChange={(v) => setRandomPassword(!!v)}
                />
                <Label htmlFor="random-pw" className="text-sm font-normal">
                  Zufaelliges Passwort generieren
                </Label>
              </div>

              {!randomPassword && (
                <div className="space-y-1">
                  <Label htmlFor="admin-pw">Passwort *</Label>
                  <div className="relative">
                    <Input
                      id="admin-pw"
                      type={showPassword ? "text" : "password"}
                      value={adminPassword}
                      onChange={(e) => setAdminPassword(e.target.value)}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon-xs"
                      className="absolute right-1 top-1/2 -translate-y-1/2"
                      onClick={() => setShowPassword(!showPassword)}
                    >
                      {showPassword ? (
                        <EyeOff className="h-3.5 w-3.5" />
                      ) : (
                        <Eye className="h-3.5 w-3.5" />
                      )}
                    </Button>
                  </div>
                </div>
              )}
            </div>

            <DialogFooter>
              <Button variant="outline" size="sm" onClick={handleClose}>
                Abbrechen
              </Button>
              <Button size="sm" onClick={handleSubmit} disabled={loading}>
                {loading ? "Erstellen..." : "Unternehmen erstellen"}
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

// ─── Suspend Dialog ──────────────────────────────────────

interface SuspendDialogProps {
  open: boolean;
  company: CompanyResponse | null;
  onConfirm: (reason: string) => void;
  onCancel: () => void;
}

function SuspendDialog({ open, company, onConfirm, onCancel }: SuspendDialogProps) {
  const [reason, setReason] = useState("");

  useEffect(() => {
    if (open) setReason("");
  }, [open]);

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onCancel()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Unternehmen sperren</DialogTitle>
          <DialogDescription>
            &quot;{company?.name}&quot; sperren? Benutzer koennen sich nicht mehr
            anmelden.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          <Label htmlFor="suspend-reason">Sperrgrund</Label>
          <Textarea
            id="suspend-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Grund fuer die Sperrung..."
            rows={3}
          />
        </div>
        <DialogFooter className="mt-2 gap-2 sm:gap-0">
          <Button variant="outline" size="sm" onClick={onCancel}>
            Abbrechen
          </Button>
          <Button
            variant="destructive"
            size="sm"
            onClick={() => onConfirm(reason)}
          >
            Sperren
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ─── Main Page ────────────────────────────────────────────

export default function CompaniesPage() {
  const router = useRouter();
  const {
    data: companies,
    loading,
    refetch,
  } = useApi<CompanyResponse[]>("/api/system/companies");
  const { mutate } = useMutation();

  const [createOpen, setCreateOpen] = useState(false);
  const [suspendTarget, setSuspendTarget] = useState<CompanyResponse | null>(null);
  const [reactivateTarget, setReactivateTarget] = useState<CompanyResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<CompanyResponse | null>(null);

  // Filters
  const [filterStatus, setFilterStatus] = useState<string>("ALL");
  const [filterPlan, setFilterPlan] = useState<string>("ALL");

  // KPIs
  const totalCompanies = companies?.length ?? 0;
  const activeCompanies =
    companies?.filter((c) => c.status === "ACTIVE").length ?? 0;
  const thisMonth = useMemo(() => {
    if (!companies) return 0;
    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    return companies.filter(
      (c) => new Date(c.createdAt) >= startOfMonth
    ).length;
  }, [companies]);

  // Filtered data
  const filteredCompanies = useMemo(() => {
    if (!companies) return [];
    return companies.filter((c) => {
      if (filterStatus !== "ALL" && c.status !== filterStatus) return false;
      if (filterPlan !== "ALL" && c.plan !== filterPlan) return false;
      return true;
    });
  }, [companies, filterStatus, filterPlan]);

  // Actions
  const handleSuspend = async (reason: string) => {
    if (!suspendTarget) return;
    try {
      await mutate(
        "post",
        `/api/system/companies/${suspendTarget.id}/suspend`,
        { reason }
      );
      toast.success(`"${suspendTarget.name}" gesperrt.`);
      setSuspendTarget(null);
      refetch();
    } catch {
      toast.error("Fehler beim Sperren des Unternehmens.");
    }
  };

  const handleReactivate = async () => {
    if (!reactivateTarget) return;
    try {
      await mutate(
        "post",
        `/api/system/companies/${reactivateTarget.id}/activate`
      );
      toast.success(`"${reactivateTarget.name}" reaktiviert.`);
      setReactivateTarget(null);
      refetch();
    } catch {
      toast.error("Fehler beim Reaktivieren des Unternehmens.");
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await mutate("delete", `/api/system/companies/${deleteTarget.id}`);
      toast.success(`"${deleteTarget.name}" geloescht.`);
      setDeleteTarget(null);
      refetch();
    } catch {
      toast.error("Fehler beim Loeschen des Unternehmens.");
    }
  };

  // Columns
  const columns: ColumnDef<CompanyResponse>[] = [
    {
      id: "name",
      header: "Name",
      accessorKey: "name",
      cell: (row) => <span className="font-medium">{row.name}</span>,
    },
    {
      id: "slug",
      header: "Slug",
      accessorKey: "slug",
      cell: (row) => <span className="font-mono text-xs">{row.slug}</span>,
    },
    {
      id: "plan",
      header: "Plan",
      accessorKey: "plan",
      cell: (row) => (
        <DomainStatusBadge variant={getPlanVariant(row.plan)}>
          {row.plan}
        </DomainStatusBadge>
      ),
    },
    {
      id: "status",
      header: "Status",
      accessorKey: "status",
      cell: (row) => (
        <DomainStatusBadge variant={getCompanyStatusVariant(row.status)}>
          {row.status}
        </DomainStatusBadge>
      ),
    },
    {
      id: "createdAt",
      header: "Erstellt",
      accessorKey: "createdAt",
      cell: (row) => (
        <span className="text-xs text-muted-foreground">
          {formatDate(row.createdAt)}
        </span>
      ),
    },
  ];

  // Row actions
  const getRowActions = useCallback(
    (row: CompanyResponse): RowAction<CompanyResponse>[] => {
      const actions: RowAction<CompanyResponse>[] = [
        {
          label: "Details anzeigen",
          icon: <Eye className="h-3.5 w-3.5" />,
          onClick: (r) => router.push(`/system/companies/${r.id}`),
        },
      ];

      if (row.status === "ACTIVE") {
        actions.push({
          label: "Sperren",
          icon: <Ban className="h-3.5 w-3.5" />,
          onClick: (r) => setSuspendTarget(r),
          variant: "destructive",
        });
      }

      if (row.status === "SUSPENDED") {
        actions.push({
          label: "Reaktivieren",
          icon: <RotateCcw className="h-3.5 w-3.5" />,
          onClick: (r) => setReactivateTarget(r),
        });
      }

      actions.push({
        label: "Loeschen",
        icon: <Trash2 className="h-3.5 w-3.5" />,
        onClick: (r) => setDeleteTarget(r),
        variant: "destructive",
      });

      return actions;
    },
    [router]
  );

  // We need to flatten row actions since DataTable expects static actions
  // Use a wrapper approach: provide all possible actions and hide irrelevant ones
  const allRowActions: RowAction<CompanyResponse>[] = useMemo(
    () => [
      {
        label: "Details anzeigen",
        icon: <Eye className="h-3.5 w-3.5" />,
        onClick: (r) => router.push(`/system/companies/${r.id}`),
      },
      {
        label: "Sperren",
        icon: <Ban className="h-3.5 w-3.5" />,
        onClick: (r) => setSuspendTarget(r),
        variant: "destructive" as const,
      },
      {
        label: "Reaktivieren",
        icon: <RotateCcw className="h-3.5 w-3.5" />,
        onClick: (r) => setReactivateTarget(r),
      },
      {
        label: "Loeschen",
        icon: <Trash2 className="h-3.5 w-3.5" />,
        onClick: (r) => setDeleteTarget(r),
        variant: "destructive" as const,
      },
    ],
    [router]
  );

  // suppress lint for getRowActions since we use allRowActions instead
  void getRowActions;

  return (
    <>
      <PageHeader
        title="Unternehmen"
        description="Alle Unternehmen auf der Plattform verwalten."
        breadcrumb={["System", "Unternehmen"]}
        actions={
          <Button size="sm" className="gap-1.5" onClick={() => setCreateOpen(true)}>
            <Plus className="h-3.5 w-3.5" />
            Neues Unternehmen
          </Button>
        }
      />

      {/* KPIs */}
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <KpiCard label="Unternehmen gesamt" value={String(totalCompanies)} loading={loading} />
        <KpiCard label="Aktive" value={String(activeCompanies)} loading={loading} />
        <KpiCard label="Diesen Monat erstellt" value={String(thisMonth)} loading={loading} />
      </div>

      {/* Table */}
      <DataTable
        data={filteredCompanies}
        columns={columns}
        searchKey="name"
        searchPlaceholder="Unternehmen suchen..."
        loading={loading}
        rowActions={allRowActions}
        onRowClick={(row) => router.push(`/system/companies/${row.id}`)}
        emptyState={{
          icon: <Building2 className="h-8 w-8 text-muted-foreground/40" />,
          title: "Noch keine Unternehmen",
          description: "Erstellen Sie das erste Unternehmen.",
        }}
        filterSlots={
          <>
            <Select value={filterStatus} onValueChange={setFilterStatus}>
              <SelectTrigger className="h-9 w-[140px]">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Alle Status</SelectItem>
                <SelectItem value="ACTIVE">Aktiv</SelectItem>
                <SelectItem value="SUSPENDED">Gesperrt</SelectItem>
                <SelectItem value="DELETED">Geloescht</SelectItem>
              </SelectContent>
            </Select>
            <Select value={filterPlan} onValueChange={setFilterPlan}>
              <SelectTrigger className="h-9 w-[140px]">
                <SelectValue placeholder="Plan" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Alle Plaene</SelectItem>
                <SelectItem value="BASIC">Basic</SelectItem>
                <SelectItem value="PROFESSIONAL">Professional</SelectItem>
                <SelectItem value="ENTERPRISE">Enterprise</SelectItem>
              </SelectContent>
            </Select>
          </>
        }
      />

      {/* Modals */}
      <CreateCompanyModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSuccess={refetch}
      />

      <SuspendDialog
        open={!!suspendTarget}
        company={suspendTarget}
        onConfirm={handleSuspend}
        onCancel={() => setSuspendTarget(null)}
      />

      {/* Reactivate confirmation */}
      <Dialog
        open={!!reactivateTarget}
        onOpenChange={(v) => !v && setReactivateTarget(null)}
      >
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Unternehmen reaktivieren</DialogTitle>
            <DialogDescription>
              &quot;{reactivateTarget?.name}&quot; reaktivieren? Benutzer koennen sich
              wieder anmelden.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-2 gap-2 sm:gap-0">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setReactivateTarget(null)}
            >
              Abbrechen
            </Button>
            <Button size="sm" onClick={handleReactivate}>
              Reaktivieren
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete confirmation */}
      <Dialog
        open={!!deleteTarget}
        onOpenChange={(v) => !v && setDeleteTarget(null)}
      >
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <div className="flex items-start gap-3">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-destructive/10">
                <Trash2 className="h-4 w-4 text-destructive" />
              </div>
              <div>
                <DialogTitle className="text-base">Unternehmen loeschen</DialogTitle>
                <DialogDescription className="mt-1 text-sm">
                  &quot;{deleteTarget?.name}&quot; endgueltig loeschen? Diese
                  Aktion kann nicht rueckgaengig gemacht werden. Alle Daten gehen verloren.
                </DialogDescription>
              </div>
            </div>
          </DialogHeader>
          <DialogFooter className="mt-2 gap-2 sm:gap-0">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setDeleteTarget(null)}
            >
              Abbrechen
            </Button>
            <Button variant="destructive" size="sm" onClick={handleDelete}>
              Loeschen
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
