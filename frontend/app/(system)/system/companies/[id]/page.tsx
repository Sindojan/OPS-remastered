"use client";

import { useState, useCallback, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  ArrowLeft,
  Ban,
  Bot,
  Check,
  Copy,
  KeyRound,
  Package,
  RotateCcw,
  Save,
  Settings2,
  Coins,
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
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import { useApi, useMutation } from "@/hooks/api/use-api";
import { formatDate, formatDateTime } from "@/lib/format";
import type {
  CompanyResponse,
  CompanyUpdateRequest,
  CompanyStatsResponse,
  CompanyAdminResponse,
  CompanyPlan,
  CompanyStatus,
  ModuleResponse,
  BudgetOverviewResponse,
  AgentInstanceDetailResponse,
  SystemLlmConfigResponse,
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

function formatTokens(n: number): string {
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace(/\.0$/, "") + "M";
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, "") + "K";
  return String(n);
}

function formatCost(n: number): string {
  return "$" + n.toFixed(4);
}

// ─── Modules Tab ────────────────────────────────────────

function CompanyModulesTab({ companyId }: { companyId: string }) {
  const {
    data: modules,
    loading: modulesLoading,
    refetch: refetchModules,
  } = useApi<ModuleResponse[]>(`/api/system/companies/${companyId}/modules`);
  const { mutate: mutateMod } = useMutation();

  const handleToggleModule = async (moduleId: string, enabled: boolean) => {
    try {
      await mutateMod(
        "put",
        `/api/system/companies/${companyId}/modules/${moduleId}/toggle`,
        { enabled }
      );
      toast.success(enabled ? "Modul aktiviert" : "Modul deaktiviert");
      refetchModules();
    } catch {
      toast.error("Fehler beim Umschalten des Moduls");
    }
  };

  if (modulesLoading) {
    return (
      <Card className="p-6">
        <p className="text-sm text-muted-foreground">Laden...</p>
      </Card>
    );
  }

  return (
    <Card className="p-6">
      <div className="mb-4">
        <h3 className="text-sm font-semibold">Feature-Module</h3>
        <p className="text-xs text-muted-foreground">
          Module fuer dieses Unternehmen aktivieren oder deaktivieren.
          Deaktivierte Module sind in Navigation, API und Agent-Tools nicht
          verfuegbar.
        </p>
      </div>
      <div className="rounded-md border">
        <div className="grid grid-cols-[1fr_auto] gap-4 border-b px-4 py-2.5 text-xs font-medium uppercase tracking-wider text-muted-foreground">
          <span>Modul</span>
          <span>Status</span>
        </div>
        {(modules || []).map((mod) => (
          <div
            key={mod.id}
            className="grid grid-cols-[1fr_auto] items-center gap-4 border-b px-4 py-3 last:border-b-0"
          >
            <div className="flex items-center gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/10">
                <Package className="h-4 w-4 text-primary" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium">{mod.label}</span>
                  {mod.core && (
                    <Badge
                      variant="secondary"
                      className="text-[10px] px-1.5 py-0"
                    >
                      Kern
                    </Badge>
                  )}
                </div>
                <p className="text-xs text-muted-foreground">
                  {mod.description}
                </p>
              </div>
            </div>
            <Switch
              checked={mod.enabled}
              onCheckedChange={(checked) =>
                handleToggleModule(mod.id, checked)
              }
              disabled={mod.core}
            />
          </div>
        ))}
      </div>
    </Card>
  );
}

// ─── Token Usage Tab ────────────────────────────────────

function CompanyBudgetTab({ companyId }: { companyId: string }) {
  const { data: budget, loading } = useApi<BudgetOverviewResponse>(
    `/api/system/companies/${companyId}/budget`
  );

  if (loading) {
    return (
      <Card className="p-6">
        <p className="text-sm text-muted-foreground">Laden...</p>
      </Card>
    );
  }

  if (!budget) {
    return (
      <Card className="p-6">
        <p className="text-sm text-muted-foreground">
          Keine Budget-Daten verfuegbar.
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <KpiCard
          label="Kosten (30 Tage)"
          value={formatCost(budget.last30Days.totalCostUsd)}
        />
        <KpiCard
          label="Tokens (30 Tage)"
          value={formatTokens(budget.last30Days.totalTokens)}
        />
        <KpiCard
          label="Laeufe (30 Tage)"
          value={String(budget.last30Days.totalRuns)}
        />
      </div>

      {/* Agent Breakdown */}
      {budget.last30Days.byAgent.length > 0 && (
        <Card className="p-6">
          <h3 className="mb-3 text-sm font-semibold">
            Aufschluesselung nach Agent
          </h3>
          <div className="rounded-md border">
            <div className="grid grid-cols-4 gap-4 border-b px-4 py-2.5 text-xs font-medium uppercase tracking-wider text-muted-foreground">
              <span>Agent</span>
              <span className="text-right">Kosten</span>
              <span className="text-right">Tokens</span>
              <span className="text-right">Laeufe</span>
            </div>
            {budget.last30Days.byAgent.map((agent) => (
              <div
                key={agent.agentName}
                className="grid grid-cols-4 gap-4 border-b px-4 py-2.5 text-sm last:border-b-0"
              >
                <span className="font-medium">{agent.agentName}</span>
                <span className="text-right font-mono text-xs">
                  {formatCost(agent.costUsd)}
                </span>
                <span className="text-right font-mono text-xs">
                  {formatTokens(agent.tokens)}
                </span>
                <span className="text-right font-mono text-xs">
                  {agent.runs}
                </span>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* Daily Usage */}
      {budget.dailyUsage.length > 0 && (
        <Card className="p-6">
          <h3 className="mb-3 text-sm font-semibold">Tagesverbrauch</h3>
          <div className="rounded-md border">
            <div className="grid grid-cols-3 gap-4 border-b px-4 py-2.5 text-xs font-medium uppercase tracking-wider text-muted-foreground">
              <span>Datum</span>
              <span className="text-right">Kosten</span>
              <span className="text-right">Tokens</span>
            </div>
            {budget.dailyUsage
              .slice()
              .reverse()
              .map((day) => (
                <div
                  key={day.date}
                  className="grid grid-cols-3 gap-4 border-b px-4 py-2 text-sm last:border-b-0"
                >
                  <span className="font-mono text-xs">{day.date}</span>
                  <span className="text-right font-mono text-xs">
                    {formatCost(day.costUsd)}
                  </span>
                  <span className="text-right font-mono text-xs">
                    {formatTokens(day.tokens)}
                  </span>
                </div>
              ))}
          </div>
        </Card>
      )}
    </div>
  );
}

// ─── Agents Tab ─────────────────────────────────────────

const MODEL_OPTIONS = [
  { value: "claude-opus-4-20250514", label: "Claude Opus 4.6" },
  { value: "claude-sonnet-4-20250514", label: "Claude Sonnet 4.6" },
  { value: "claude-haiku-4-5-20251001", label: "Claude Haiku 4.5" },
];

function CompanyAgentsTab({ companyId }: { companyId: string }) {
  const {
    data: agents,
    loading,
    refetch,
  } = useApi<AgentInstanceDetailResponse[]>(
    `/api/system/companies/${companyId}/agents`
  );
  const { mutate } = useMutation();

  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [editPrompt, setEditPrompt] = useState<string>("");
  const [saving, setSaving] = useState(false);

  const handleModelChange = async (instanceId: string, model: string) => {
    try {
      await mutate(
        "patch",
        `/api/system/companies/${companyId}/agents/${instanceId}`,
        { model }
      );
      toast.success("Model aktualisiert");
      refetch();
    } catch {
      toast.error("Fehler beim Aktualisieren des Models");
    }
  };

  const handleSavePrompt = async (instanceId: string) => {
    setSaving(true);
    try {
      await mutate(
        "patch",
        `/api/system/companies/${companyId}/agents/${instanceId}`,
        { customSystemPrompt: editPrompt }
      );
      toast.success("System-Prompt gespeichert");
      refetch();
      setExpandedId(null);
    } catch {
      toast.error("Fehler beim Speichern des System-Prompts");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Card className="p-6">
        <p className="text-sm text-muted-foreground">Laden...</p>
      </Card>
    );
  }

  return (
    <Card className="p-6">
      <div className="mb-4">
        <h3 className="text-sm font-semibold">Agent-Instanzen</h3>
        <p className="text-xs text-muted-foreground">
          Model und System-Prompt pro Agent-Instanz konfigurieren.
        </p>
      </div>
      <div className="space-y-3">
        {(agents || []).map((agent) => (
          <div key={agent.instanceId} className="rounded-lg border">
            <div className="flex items-center justify-between px-4 py-3">
              <div className="flex items-center gap-3">
                <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/10">
                  <Bot className="h-4 w-4 text-primary" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium">{agent.name}</span>
                    <Badge variant="outline" className="text-[10px] px-1.5 py-0">
                      {agent.templateName}
                    </Badge>
                    {agent.hasCustomPrompt && (
                      <Badge variant="secondary" className="text-[10px] px-1.5 py-0">
                        Custom Prompt
                      </Badge>
                    )}
                  </div>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <span>{agent.status}</span>
                    <span>·</span>
                    <span className="flex items-center gap-1">
                      <span className={`inline-block h-1.5 w-1.5 rounded-full ${
                        agent.activityStatus === "BUSY" ? "bg-primary animate-pulse" :
                        agent.activityStatus === "ERROR" ? "bg-destructive" : "bg-success"
                      }`} />
                      {agent.activityStatus === "BUSY" ? "Aktiv" : agent.activityStatus === "ERROR" ? "Fehler" : "Bereit"}
                    </span>
                    <span>·</span>
                    <span>{agent.toolCount} Tools</span>
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Select
                  value={agent.model}
                  onValueChange={(v) => handleModelChange(agent.instanceId, v)}
                >
                  <SelectTrigger className="w-[200px] h-8 text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {MODEL_OPTIONS.map((opt) => (
                      <SelectItem key={opt.value} value={opt.value}>
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-xs"
                  onClick={() => {
                    if (expandedId === agent.instanceId) {
                      setExpandedId(null);
                    } else {
                      setExpandedId(agent.instanceId);
                      setEditPrompt(agent.customSystemPrompt || "");
                    }
                  }}
                >
                  <Settings2 className="h-3.5 w-3.5 mr-1" />
                  Prompt
                </Button>
              </div>
            </div>
            {expandedId === agent.instanceId && (
              <div className="border-t px-4 py-3 space-y-2">
                <Label className="text-xs">Custom System-Prompt</Label>
                <Textarea
                  value={editPrompt}
                  onChange={(e) => setEditPrompt(e.target.value)}
                  rows={8}
                  className="font-mono text-xs"
                  placeholder="Leer = Template-Standard verwenden"
                />
                <div className="flex justify-end gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setExpandedId(null)}
                  >
                    Abbrechen
                  </Button>
                  <Button
                    size="sm"
                    onClick={() => handleSavePrompt(agent.instanceId)}
                    disabled={saving}
                  >
                    <Save className="h-3.5 w-3.5 mr-1" />
                    Speichern
                  </Button>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </Card>
  );
}

// ─── LLM Config Tab ─────────────────────────────────────

function CompanyLlmTab({ companyId }: { companyId: string }) {
  const {
    data: config,
    loading,
    refetch,
  } = useApi<SystemLlmConfigResponse>(
    `/api/system/companies/${companyId}/llm-config`
  );
  const { mutate } = useMutation();

  const [apiKey, setApiKey] = useState("");
  const [defaultModel, setDefaultModel] = useState("claude-sonnet-4-20250514");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (config?.defaultModel) {
      setDefaultModel(config.defaultModel);
    }
  }, [config?.defaultModel]);

  const handleSave = async () => {
    setSaving(true);
    try {
      await mutate("put", `/api/system/companies/${companyId}/llm-config`, {
        provider: "anthropic",
        apiKey: apiKey || undefined,
        defaultModel,
      });
      toast.success("LLM-Konfiguration gespeichert");
      setApiKey("");
      refetch();
    } catch {
      toast.error("Fehler beim Speichern der LLM-Konfiguration");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Card className="p-6">
        <p className="text-sm text-muted-foreground">Laden...</p>
      </Card>
    );
  }

  return (
    <Card className="p-6 space-y-5">
      <div>
        <h3 className="text-sm font-semibold">LLM-Konfiguration</h3>
        <p className="text-xs text-muted-foreground">
          API-Schluessel und Standard-Modell fuer dieses Unternehmen
          konfigurieren.
        </p>
      </div>

      <div className="space-y-4">
        <div className="space-y-1.5">
          <Label className="text-xs">Provider</Label>
          <p className="text-sm font-medium">Anthropic</p>
        </div>

        <div className="space-y-1.5">
          <Label className="text-xs">API-Key Status</Label>
          <div className="flex items-center gap-2">
            {config?.hasApiKey ? (
              <Badge variant="default" className="text-xs">
                Konfiguriert
              </Badge>
            ) : (
              <Badge variant="destructive" className="text-xs">
                Nicht konfiguriert
              </Badge>
            )}
          </div>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="sys-api-key" className="text-xs">
            API-Key {config?.hasApiKey ? "(neu setzen)" : ""}
          </Label>
          <Input
            id="sys-api-key"
            type="password"
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            placeholder={config?.hasApiKey ? "Neuen Key eingeben..." : "sk-ant-..."}
            className="max-w-md font-mono text-sm"
          />
        </div>

        <div className="space-y-1.5">
          <Label className="text-xs">Standard-Modell</Label>
          <Select value={defaultModel} onValueChange={setDefaultModel}>
            <SelectTrigger className="w-[260px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {MODEL_OPTIONS.map((opt) => (
                <SelectItem key={opt.value} value={opt.value}>
                  {opt.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <Button onClick={handleSave} disabled={saving} size="sm">
          <Save className="h-3.5 w-3.5 mr-1" />
          Speichern
        </Button>
      </div>
    </Card>
  );
}

// ─── Main Page ──────────────────────────────────────────

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
          <TabsTrigger value="modules">Module</TabsTrigger>
          <TabsTrigger value="budget">Token-Verbrauch</TabsTrigger>
          <TabsTrigger value="agents">Agenten</TabsTrigger>
          <TabsTrigger value="llm">LLM</TabsTrigger>
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
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
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
              label="Zuletzt aktiv"
              value={
                stats?.lastActiveAt
                  ? formatDate(stats.lastActiveAt)
                  : "Nie"
              }
              loading={statsLoading}
            />
            <KpiCard
              label="Tokens (30T)"
              value={formatTokens(stats?.totalTokens30d ?? 0)}
              loading={statsLoading}
            />
            <KpiCard
              label="Kosten (30T)"
              value={formatCost(stats?.totalCostUsd30d ?? 0)}
              loading={statsLoading}
            />
            <KpiCard
              label="Speicher"
              value={String(stats?.storageUsedMb ?? 0)}
              unit="MB"
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

        {/* ─── Modules Tab ──────────────────────────── */}
        <TabsContent value="modules">
          <CompanyModulesTab companyId={companyId} />
        </TabsContent>

        {/* ─── Budget/Token Usage Tab ────────────────── */}
        <TabsContent value="budget">
          <CompanyBudgetTab companyId={companyId} />
        </TabsContent>

        {/* ─── Agents Tab ────────────────────────────── */}
        <TabsContent value="agents">
          <CompanyAgentsTab companyId={companyId} />
        </TabsContent>

        {/* ─── LLM Config Tab ────────────────────────── */}
        <TabsContent value="llm">
          <CompanyLlmTab companyId={companyId} />
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
