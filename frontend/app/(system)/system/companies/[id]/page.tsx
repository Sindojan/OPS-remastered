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
      toast.success("Name aktualisiert.");
      setEditName(null);
      refetch();
    } catch {
      toast.error("Fehler beim Aktualisieren des Namens.");
    }
  };

  // Save plan
  const handleSavePlan = async (newPlan: CompanyPlan) => {
    try {
      const body: CompanyUpdateRequest = { plan: newPlan };
      await mutate("patch", `/api/system/companies/${companyId}`, body);
      toast.success("Plan aktualisiert.");
      setEditPlan(null);
      refetch();
    } catch {
      toast.error("Fehler beim Aktualisieren des Plans.");
    }
  };

  // Suspend
  const handleSuspend = async () => {
    try {
      await mutate("post", `/api/system/companies/${companyId}/suspend`, {
        reason: suspendReason,
      });
      toast.success("Unternehmen gesperrt.");
      setSuspendOpen(false);
      setSuspendReason("");
      refetch();
    } catch {
      toast.error("Fehler beim Sperren des Unternehmens.");
    }
  };

  // Reactivate
  const handleReactivate = async () => {
    try {
      await mutate("post", `/api/system/companies/${companyId}/activate`);
      toast.success("Unternehmen reaktiviert.");
      refetch();
    } catch {
      toast.error("Fehler beim Reaktivieren des Unternehmens.");
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
        toast.error("Fehler beim Zuruecksetzen des Passworts.");
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
          Laden...
        </div>
      </div>
    );
  }

  if (!company) {
    return (
      <div className="flex h-64 flex-col items-center justify-center gap-2">
        <p className="text-sm text-muted-foreground">Unternehmen nicht gefunden.</p>
        <Button variant="outline" size="sm" asChild>
          <Link href="/system/companies">Zurueck zu Unternehmen</Link>
        </Button>
      </div>
    );
  }

  return (
    <>
      <PageHeader
        title={company.name}
        breadcrumb={["System", "Unternehmen", company.name]}
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
                Sperren
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
                Reaktivieren
              </Button>
            )}
            <Button variant="ghost" size="sm" className="gap-1.5" asChild>
              <Link href="/system/companies">
                <ArrowLeft className="h-3.5 w-3.5" />
                Zurueck
              </Link>
            </Button>
          </div>
        }
      />

      <Tabs defaultValue="overview" className="space-y-4">
        <TabsList>
          <TabsTrigger value="overview">Uebersicht</TabsTrigger>
          <TabsTrigger value="stats">Statistiken</TabsTrigger>
          <TabsTrigger value="admins">Admin-Benutzer</TabsTrigger>
        </TabsList>

        {/* ─── Overview Tab ──────────────────────────── */}
        <TabsContent value="overview" className="space-y-4">
          <Card className="p-6">
            <div className="space-y-5">
              {/* Name */}
              <div className="space-y-1.5">
                <Label className="text-xs font-medium text-muted-foreground">
                  Firmenname
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
                      Bearbeiten
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
                      Aendern
                    </Button>
                  </div>
                )}
              </div>

              {/* Created */}
              <div className="space-y-1.5">
                <Label className="text-xs font-medium text-muted-foreground">
                  Erstellt am
                </Label>
                <p className="text-sm">{formatDateTime(company.createdAt)}</p>
              </div>

              {/* Suspended info */}
              {company.suspendedAt && (
                <div className="space-y-1.5">
                  <Label className="text-xs font-medium text-muted-foreground">
                    Gesperrt am
                  </Label>
                  <p className="text-sm">
                    {formatDateTime(company.suspendedAt)}
                  </p>
                  {company.suspendReason && (
                    <p className="text-xs text-muted-foreground">
                      Grund: {company.suspendReason}
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
              label="Benutzer"
              value={String(stats?.userCount ?? 0)}
              loading={statsLoading}
            />
            <KpiCard
              label="Agent-Laeufe (30T)"
              value={String(stats?.activeAgentRuns30d ?? 0)}
              loading={statsLoading}
            />
            <KpiCard
              label="Speicher"
              value={String(stats?.storageUsedMb ?? 0)}
              unit="MB"
              loading={statsLoading}
            />
            <KpiCard
              label="Zuletzt aktiv"
              value={
                stats?.lastActiveAt
                  ? formatDate(stats.lastActiveAt)
                  : "Nie"
              }
              loading={statsLoading}
            />
          </div>
        </TabsContent>

        {/* ─── Admin Users Tab ────────────────────────── */}
        <TabsContent value="admins">
          <Card className="p-6">
            {adminsLoading ? (
              <p className="text-sm text-muted-foreground">Laden...</p>
            ) : !admins || admins.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                Keine Admin-Benutzer gefunden.
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
                      Passwort zuruecksetzen
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
            <DialogTitle>Unternehmen sperren</DialogTitle>
            <DialogDescription>
              &quot;{company.name}&quot; sperren? Benutzer koennen sich nicht mehr
              anmelden.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="detail-suspend-reason">Sperrgrund</Label>
            <Textarea
              id="detail-suspend-reason"
              value={suspendReason}
              onChange={(e) => setSuspendReason(e.target.value)}
              placeholder="Grund fuer die Sperrung..."
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
              Abbrechen
            </Button>
            <Button variant="destructive" size="sm" onClick={handleSuspend}>
              Sperren
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
            <DialogTitle>Passwort zurueckgesetzt</DialogTitle>
            <DialogDescription>
              Das neue Passwort wurde generiert. Teilen Sie es sicher mit.
            </DialogDescription>
          </DialogHeader>
          <div className="rounded-lg border border-primary/20 bg-primary/5 p-4 space-y-2">
            <p className="text-xs font-semibold uppercase tracking-wider text-primary">
              Neues Passwort
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
              {copied ? "Kopiert" : "Passwort kopieren"}
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
              Fertig
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
