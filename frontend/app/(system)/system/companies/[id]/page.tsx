"use client";

import { useState, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  ArrowLeft,
  Ban,
  Check,
  Copy,
  KeyRound,
  RotateCcw,
  Save,
} from "lucide-react";
import Link from "next/link";
import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/shared/kpi-card";
import { DomainStatusBadge } from "@/components/shared/domain-status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card } from "@/components/ui/card";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { useApi, useMutation } from "@/hooks/api/use-api";
import { formatDate, formatDateTime } from "@/lib/format";
import type {
  CompanyResponse,
  CompanyUpdateRequest,
  CompanyStatsResponse,
  CompanyAdminResponse,
  CompanyPlan,
  CompanyStatus,
} from "@/types/api";

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

export default function CompanyDetailPage() {
  const params = useParams();
  const router = useRouter();
  const companyId = params.id as string;

  const {
    data: company,
    loading,
    refetch,
  } = useApi<CompanyResponse>(`/api/system/companies/${companyId}`);
  const {
    data: stats,
    loading: statsLoading,
  } = useApi<CompanyStatsResponse>(
    `/api/system/companies/${companyId}/stats`
  );
  const {
    data: admins,
    loading: adminsLoading,
    refetch: refetchAdmins,
  } = useApi<CompanyAdminResponse[]>(
    `/api/system/companies/${companyId}/admins`
  );

  const { mutate } = useMutation();

  // Edit state
  const [editName, setEditName] = useState<string | null>(null);
  const [editPlan, setEditPlan] = useState<CompanyPlan | null>(null);

  // Suspend dialog
  const [suspendOpen, setSuspendOpen] = useState(false);
  const [suspendReason, setSuspendReason] = useState("");

  // Password reset result (backend returns plain string as data)
  const [resetPassword, setResetPassword] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  // Save name
  const handleSaveName = async () => {
    if (editName === null || !editName.trim()) return;
    try {
      const body: CompanyUpdateRequest = { name: editName.trim() };
      await mutate("patch", `/api/system/companies/${companyId}`, body);
      toast.success("Name updated.");
      setEditName(null);
      refetch();
    } catch {
      toast.error("Failed to update name.");
    }
  };

  // Save plan
  const handleSavePlan = async (newPlan: CompanyPlan) => {
    try {
      const body: CompanyUpdateRequest = { plan: newPlan };
      await mutate("patch", `/api/system/companies/${companyId}`, body);
      toast.success("Plan updated.");
      setEditPlan(null);
      refetch();
    } catch {
      toast.error("Failed to update plan.");
    }
  };

  // Suspend
  const handleSuspend = async () => {
    try {
      await mutate("post", `/api/system/companies/${companyId}/suspend`, {
        reason: suspendReason,
      });
      toast.success("Company suspended.");
      setSuspendOpen(false);
      setSuspendReason("");
      refetch();
    } catch {
      toast.error("Failed to suspend company.");
    }
  };

  // Reactivate
  const handleReactivate = async () => {
    try {
      await mutate("post", `/api/system/companies/${companyId}/activate`);
      toast.success("Company reactivated.");
      refetch();
    } catch {
      toast.error("Failed to reactivate company.");
    }
  };

  // Reset password
  const handleResetPassword = useCallback(
    async (userId: string) => {
      try {
        const res = await mutate(
          "post",
          `/api/system/companies/${companyId}/admins/${userId}/reset-password`
        );
        setResetPassword(res as unknown as string);
        refetchAdmins();
      } catch {
        toast.error("Failed to reset password.");
      }
    },
    [companyId, mutate, refetchAdmins]
  );

  const handleCopyPassword = () => {
    if (!resetPassword) return;
    navigator.clipboard.writeText(resetPassword);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="font-mono text-sm text-muted-foreground">
          Loading...
        </div>
      </div>
    );
  }

  if (!company) {
    return (
      <div className="flex h-64 flex-col items-center justify-center gap-2">
        <p className="text-sm text-muted-foreground">Company not found.</p>
        <Button variant="outline" size="sm" asChild>
          <Link href="/system/companies">Back to Companies</Link>
        </Button>
      </div>
    );
  }

  return (
    <>
      <PageHeader
        title={company.name}
        breadcrumb={["System", "Companies", company.name]}
        actions={
          <div className="flex items-center gap-2">
            <DomainStatusBadge
              variant={getCompanyStatusVariant(company.status)}
            >
              {company.status}
            </DomainStatusBadge>
            {company.status === "ACTIVE" && (
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5 text-destructive"
                onClick={() => setSuspendOpen(true)}
              >
                <Ban className="h-3.5 w-3.5" />
                Suspend
              </Button>
            )}
            {company.status === "SUSPENDED" && (
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5"
                onClick={handleReactivate}
              >
                <RotateCcw className="h-3.5 w-3.5" />
                Reactivate
              </Button>
            )}
            <Button variant="ghost" size="sm" className="gap-1.5" asChild>
              <Link href="/system/companies">
                <ArrowLeft className="h-3.5 w-3.5" />
                Back
              </Link>
            </Button>
          </div>
        }
      />

      <Tabs defaultValue="overview" className="space-y-4">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="stats">Statistics</TabsTrigger>
          <TabsTrigger value="admins">Admin Users</TabsTrigger>
        </TabsList>

        {/* ─── Overview Tab ──────────────────────────── */}
        <TabsContent value="overview" className="space-y-4">
          <Card className="p-6">
            <div className="space-y-5">
              {/* Name */}
              <div className="space-y-1.5">
                <Label className="text-xs font-medium text-muted-foreground">
                  Company Name
                </Label>
                {editName !== null ? (
                  <div className="flex items-center gap-2">
                    <Input
                      value={editName}
                      onChange={(e) => setEditName(e.target.value)}
                      className="max-w-xs"
                    />
                    <Button size="icon-xs" onClick={handleSaveName}>
                      <Save className="h-3.5 w-3.5" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-xs"
                      onClick={() => setEditName(null)}
                    >
                      &times;
                    </Button>
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-medium">{company.name}</p>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-6 text-xs text-muted-foreground"
                      onClick={() => setEditName(company.name)}
                    >
                      Edit
                    </Button>
                  </div>
                )}
              </div>

              {/* Slug */}
              <div className="space-y-1.5">
                <Label className="text-xs font-medium text-muted-foreground">
                  Slug
                </Label>
                <p className="font-mono text-sm">{company.slug}</p>
              </div>

              {/* Plan */}
              <div className="space-y-1.5">
                <Label className="text-xs font-medium text-muted-foreground">
                  Plan
                </Label>
                {editPlan !== null ? (
                  <div className="flex items-center gap-2">
                    <Select
                      value={editPlan}
                      onValueChange={(v) => {
                        handleSavePlan(v as CompanyPlan);
                      }}
                    >
                      <SelectTrigger className="w-[180px]">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="BASIC">Basic</SelectItem>
                        <SelectItem value="PROFESSIONAL">
                          Professional
                        </SelectItem>
                        <SelectItem value="ENTERPRISE">Enterprise</SelectItem>
                      </SelectContent>
                    </Select>
                    <Button
                      variant="ghost"
                      size="icon-xs"
                      onClick={() => setEditPlan(null)}
                    >
                      &times;
                    </Button>
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <p className="text-sm">{company.plan}</p>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-6 text-xs text-muted-foreground"
                      onClick={() => setEditPlan(company.plan)}
                    >
                      Change
                    </Button>
                  </div>
                )}
              </div>

              {/* Created */}
              <div className="space-y-1.5">
                <Label className="text-xs font-medium text-muted-foreground">
                  Created At
                </Label>
                <p className="text-sm">{formatDateTime(company.createdAt)}</p>
              </div>

              {/* Suspended info */}
              {company.suspendedAt && (
                <div className="space-y-1.5">
                  <Label className="text-xs font-medium text-muted-foreground">
                    Suspended At
                  </Label>
                  <p className="text-sm">
                    {formatDateTime(company.suspendedAt)}
                  </p>
                  {company.suspendReason && (
                    <p className="text-xs text-muted-foreground">
                      Reason: {company.suspendReason}
                    </p>
                  )}
                </div>
              )}
            </div>
          </Card>
        </TabsContent>

        {/* ─── Statistics Tab ────────────────────────── */}
        <TabsContent value="stats">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <KpiCard
              label="Users"
              value={String(stats?.userCount ?? 0)}
              loading={statsLoading}
            />
            <KpiCard
              label="Agent Runs (30d)"
              value={String(stats?.activeAgentRuns30d ?? 0)}
              loading={statsLoading}
            />
            <KpiCard
              label="Storage"
              value={String(stats?.storageUsedMb ?? 0)}
              unit="MB"
              loading={statsLoading}
            />
            <KpiCard
              label="Last Active"
              value={
                stats?.lastActiveAt
                  ? formatDate(stats.lastActiveAt)
                  : "Never"
              }
              loading={statsLoading}
            />
          </div>
        </TabsContent>

        {/* ─── Admin Users Tab ────────────────────────── */}
        <TabsContent value="admins">
          <Card className="p-6">
            {adminsLoading ? (
              <p className="text-sm text-muted-foreground">Loading...</p>
            ) : !admins || admins.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                No admin users found.
              </p>
            ) : (
              <div className="space-y-3">
                {admins.map((admin) => (
                  <div
                    key={admin.id}
                    className="flex items-center justify-between rounded-lg border px-4 py-3"
                  >
                    <div>
                      <p className="text-sm font-medium">
                        {admin.firstName} {admin.lastName}
                      </p>
                      <p className="font-mono text-xs text-muted-foreground">
                        {admin.email}
                      </p>
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      className="gap-1.5"
                      onClick={() => handleResetPassword(admin.id)}
                    >
                      <KeyRound className="h-3.5 w-3.5" />
                      Reset Password
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </TabsContent>
      </Tabs>

      {/* Suspend Dialog */}
      <Dialog
        open={suspendOpen}
        onOpenChange={(v) => {
          if (!v) {
            setSuspendOpen(false);
            setSuspendReason("");
          }
        }}
      >
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Suspend Company</DialogTitle>
            <DialogDescription>
              Suspend &quot;{company.name}&quot;? Users will no longer be able to
              log in.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="detail-suspend-reason">Reason</Label>
            <Textarea
              id="detail-suspend-reason"
              value={suspendReason}
              onChange={(e) => setSuspendReason(e.target.value)}
              placeholder="Reason for suspension..."
              rows={3}
            />
          </div>
          <DialogFooter className="mt-2 gap-2 sm:gap-0">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setSuspendOpen(false);
                setSuspendReason("");
              }}
            >
              Cancel
            </Button>
            <Button variant="destructive" size="sm" onClick={handleSuspend}>
              Suspend
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Password Reset Result */}
      <Dialog
        open={!!resetPassword}
        onOpenChange={(v) => {
          if (!v) {
            setResetPassword(null);
            setCopied(false);
          }
        }}
      >
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Password Reset</DialogTitle>
            <DialogDescription>
              The new password has been generated. Share it securely.
            </DialogDescription>
          </DialogHeader>
          <div className="rounded-lg border border-primary/20 bg-primary/5 p-4 space-y-2">
            <p className="text-xs font-semibold uppercase tracking-wider text-primary">
              New Password
            </p>
            <p className="font-mono text-sm select-all">
              {resetPassword}
            </p>
            <Button
              variant="outline"
              size="sm"
              className="mt-2 gap-1.5"
              onClick={handleCopyPassword}
            >
              {copied ? (
                <Check className="h-3.5 w-3.5" />
              ) : (
                <Copy className="h-3.5 w-3.5" />
              )}
              {copied ? "Copied" : "Copy Password"}
            </Button>
          </div>
          <DialogFooter>
            <Button
              size="sm"
              onClick={() => {
                setResetPassword(null);
                setCopied(false);
              }}
            >
              Done
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
