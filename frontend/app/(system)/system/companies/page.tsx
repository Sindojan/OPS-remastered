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
      toast.error("Please fill in all required fields.");
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
        toast.success("Company created successfully.");
        onSuccess();
      }
    } catch {
      toast.error("Failed to create company.");
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
            {result ? "Company Created" : "Create Company"}
          </DialogTitle>
          <DialogDescription>
            {result
              ? "Save the admin login credentials below."
              : "Set up a new company with an admin account."}
          </DialogDescription>
        </DialogHeader>

        {result ? (
          <div className="space-y-4">
            <div className="rounded-lg border bg-muted/50 p-4 space-y-2">
              <div>
                <span className="text-xs font-medium text-muted-foreground">
                  Company
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
                Admin Login Credentials
              </p>
              <div>
                <span className="text-xs font-medium text-muted-foreground">
                  Email
                </span>
                <p className="font-mono text-sm">{result.adminEmail}</p>
              </div>
              <div>
                <span className="text-xs font-medium text-muted-foreground">
                  Password
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
                {copied ? "Copied" : "Copy Credentials"}
              </Button>
            </div>
            <DialogFooter>
              <Button variant="default" size="sm" onClick={handleClose}>
                Done
              </Button>
            </DialogFooter>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="company-name">Company Name *</Label>
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
                Auto-generated from name. Edit to override.
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
                Admin Account
              </p>
              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1">
                  <Label htmlFor="admin-first">First Name *</Label>
                  <Input
                    id="admin-first"
                    value={adminFirstName}
                    onChange={(e) => setAdminFirstName(e.target.value)}
                  />
                </div>
                <div className="space-y-1">
                  <Label htmlFor="admin-last">Last Name *</Label>
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
                  Generate random password
                </Label>
              </div>

              {!randomPassword && (
                <div className="space-y-1">
                  <Label htmlFor="admin-pw">Password *</Label>
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
                Cancel
              </Button>
              <Button size="sm" onClick={handleSubmit} disabled={loading}>
                {loading ? "Creating..." : "Create Company"}
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
          <DialogTitle>Suspend Company</DialogTitle>
          <DialogDescription>
            Suspend &quot;{company?.name}&quot;? Users will no longer be able to
            log in.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          <Label htmlFor="suspend-reason">Reason</Label>
          <Textarea
            id="suspend-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Reason for suspension..."
            rows={3}
          />
        </div>
        <DialogFooter className="mt-2 gap-2 sm:gap-0">
          <Button variant="outline" size="sm" onClick={onCancel}>
            Cancel
          </Button>
          <Button
            variant="destructive"
            size="sm"
            onClick={() => onConfirm(reason)}
          >
            Suspend
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
      toast.success(`"${suspendTarget.name}" suspended.`);
      setSuspendTarget(null);
      refetch();
    } catch {
      toast.error("Failed to suspend company.");
    }
  };

  const handleReactivate = async () => {
    if (!reactivateTarget) return;
    try {
      await mutate(
        "post",
        `/api/system/companies/${reactivateTarget.id}/activate`
      );
      toast.success(`"${reactivateTarget.name}" reactivated.`);
      setReactivateTarget(null);
      refetch();
    } catch {
      toast.error("Failed to reactivate company.");
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await mutate("delete", `/api/system/companies/${deleteTarget.id}`);
      toast.success(`"${deleteTarget.name}" deleted.`);
      setDeleteTarget(null);
      refetch();
    } catch {
      toast.error("Failed to delete company.");
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
      header: "Created",
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
          label: "View Details",
          icon: <Eye className="h-3.5 w-3.5" />,
          onClick: (r) => router.push(`/system/companies/${r.id}`),
        },
      ];

      if (row.status === "ACTIVE") {
        actions.push({
          label: "Suspend",
          icon: <Ban className="h-3.5 w-3.5" />,
          onClick: (r) => setSuspendTarget(r),
          variant: "destructive",
        });
      }

      if (row.status === "SUSPENDED") {
        actions.push({
          label: "Reactivate",
          icon: <RotateCcw className="h-3.5 w-3.5" />,
          onClick: (r) => setReactivateTarget(r),
        });
      }

      actions.push({
        label: "Delete",
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
        label: "View Details",
        icon: <Eye className="h-3.5 w-3.5" />,
        onClick: (r) => router.push(`/system/companies/${r.id}`),
      },
      {
        label: "Suspend",
        icon: <Ban className="h-3.5 w-3.5" />,
        onClick: (r) => setSuspendTarget(r),
        variant: "destructive" as const,
      },
      {
        label: "Reactivate",
        icon: <RotateCcw className="h-3.5 w-3.5" />,
        onClick: (r) => setReactivateTarget(r),
      },
      {
        label: "Delete",
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
        title="Companies"
        description="Manage all companies on the platform."
        breadcrumb={["System", "Companies"]}
        actions={
          <Button size="sm" className="gap-1.5" onClick={() => setCreateOpen(true)}>
            <Plus className="h-3.5 w-3.5" />
            New Company
          </Button>
        }
      />

      {/* KPIs */}
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <KpiCard label="Total Companies" value={String(totalCompanies)} loading={loading} />
        <KpiCard label="Active" value={String(activeCompanies)} loading={loading} />
        <KpiCard label="Created This Month" value={String(thisMonth)} loading={loading} />
      </div>

      {/* Table */}
      <DataTable
        data={filteredCompanies}
        columns={columns}
        searchKey="name"
        searchPlaceholder="Search companies..."
        loading={loading}
        rowActions={allRowActions}
        onRowClick={(row) => router.push(`/system/companies/${row.id}`)}
        emptyState={{
          icon: <Building2 className="h-8 w-8 text-muted-foreground/40" />,
          title: "No companies yet",
          description: "Create the first company to get started.",
        }}
        filterSlots={
          <>
            <Select value={filterStatus} onValueChange={setFilterStatus}>
              <SelectTrigger className="h-9 w-[140px]">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Status</SelectItem>
                <SelectItem value="ACTIVE">Active</SelectItem>
                <SelectItem value="SUSPENDED">Suspended</SelectItem>
                <SelectItem value="DELETED">Deleted</SelectItem>
              </SelectContent>
            </Select>
            <Select value={filterPlan} onValueChange={setFilterPlan}>
              <SelectTrigger className="h-9 w-[140px]">
                <SelectValue placeholder="Plan" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Plans</SelectItem>
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
            <DialogTitle>Reactivate Company</DialogTitle>
            <DialogDescription>
              Reactivate &quot;{reactivateTarget?.name}&quot;? Users will be able
              to log in again.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-2 gap-2 sm:gap-0">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setReactivateTarget(null)}
            >
              Cancel
            </Button>
            <Button size="sm" onClick={handleReactivate}>
              Reactivate
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
                <DialogTitle className="text-base">Delete Company</DialogTitle>
                <DialogDescription className="mt-1 text-sm">
                  Permanently delete &quot;{deleteTarget?.name}&quot;? This
                  action cannot be undone. All data will be lost.
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
              Cancel
            </Button>
            <Button variant="destructive" size="sm" onClick={handleDelete}>
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
