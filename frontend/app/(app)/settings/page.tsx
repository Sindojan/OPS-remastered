"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { PageHeader } from "@/components/shared/page-header";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import { StatusBadge } from "@/components/shared/status-badge";
import { apiClient } from "@/lib/api-client";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Settings,
  Key,
  Bot,
  Save,
  CheckCircle,
  XCircle,
  Zap,
  Loader2,
  Shield,
} from "lucide-react";
import type {
  ApiResponse,
  LlmConfig,
  AgentInstance,
  AgentTemplate,
  RoleAgentDefaultResponse,
  RoleAgentDefaultUpdateRequest,
} from "@/types/api";
import { toast } from "sonner";

// ============ Types for internal use ============

interface ModelInfo {
  id: string;
  name: string;
}

interface InstanceRow {
  id: string;
  name: string;
  role: string;
  status: string;
  model: string;
  templateId: string;
}

// ============ LLM Configuration Tab ============

function LlmConfigTab() {
  const [provider, setProvider] = useState("anthropic");
  const [apiKey, setApiKey] = useState("");
  const [defaultModel, setDefaultModel] = useState("");
  const [models, setModels] = useState<ModelInfo[]>([]);
  const [hasApiKey, setHasApiKey] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [modelsLoading, setModelsLoading] = useState(false);
  const [saveMessage, setSaveMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const fetchConfig = useCallback(async () => {
    try {
      const config = await apiClient.get<ApiResponse<LlmConfig>>(
        "/api/settings/llm"
      );
      setProvider(config.data.provider || "anthropic");
      setDefaultModel(config.data.defaultModel || "");
      setHasApiKey(config.data.hasApiKey);
    } catch {
      // Config may not exist yet
    }
  }, []);

  const fetchModels = useCallback(async () => {
    try {
      const response = await apiClient.get<ApiResponse<string[]>>(
        "/api/settings/llm/models"
      );
      setModels(
        response.data.map((id) => ({ id, name: id }))
      );
    } catch {
      setModels([
        { id: "claude-sonnet-4-20250514", name: "Claude Sonnet 4" },
        { id: "claude-opus-4-20250514", name: "Claude Opus 4" },
        { id: "claude-3-5-haiku-20241022", name: "Claude 3.5 Haiku" },
      ]);
    }
  }, []);

  const fetchModelsWithKey = useCallback(async (key: string, prov: string) => {
    setModelsLoading(true);
    try {
      const response = await apiClient.post<ApiResponse<string[]>>(
        "/api/settings/llm/models",
        { provider: prov, apiKey: key }
      );
      setModels(
        response.data.map((id) => ({ id, name: id }))
      );
    } catch {
      // Keep existing models on error
    } finally {
      setModelsLoading(false);
    }
  }, []);

  useEffect(() => {
    const init = async () => {
      setLoading(true);
      await Promise.all([fetchConfig(), fetchModels()]);
      setLoading(false);
    };
    init();
  }, [fetchConfig, fetchModels]);

  // Debounce: reload models when API key changes
  useEffect(() => {
    if (!apiKey || apiKey.length < 10) return;
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      fetchModelsWithKey(apiKey, provider);
    }, 800);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [apiKey, provider, fetchModelsWithKey]);

  const handleSave = async () => {
    setSaving(true);
    setSaveMessage(null);
    try {
      await apiClient.put<ApiResponse<LlmConfig>>("/api/settings/llm", {
        provider,
        apiKey: apiKey || undefined,
        defaultModel,
      });
      toast.success("Configuration saved successfully");
      setHasApiKey(true);
      setApiKey("");
      await fetchModels();
    } catch (err) {
      toast.error("Failed to save configuration", {
        description: err instanceof Error ? err.message : "Unknown error",
      });
    } finally {
      setSaving(false);
    }
  };

  const handleTestConnection = async () => {
    setTesting(true);
    setSaveMessage(null);
    try {
      await fetchModels();
      toast.success(`Connection successful. ${models.length} models available.`);
    } catch (err) {
      toast.error("Connection test failed", {
        description: err instanceof Error ? err.message : "Unknown error",
      });
    } finally {
      setTesting(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-32" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Status indicator */}
      <Card className="border-border/50">
        <CardContent className="flex items-center gap-3 py-4">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
            <Key className="h-5 w-5 text-primary" />
          </div>
          <div className="flex-1">
            <p className="text-sm font-medium">API Key Status</p>
            <p className="text-xs text-muted-foreground">
              {hasApiKey
                ? "API key is configured and ready"
                : "No API key configured yet"}
            </p>
          </div>
          <Badge
            variant={hasApiKey ? "default" : "outline"}
            className={
              hasApiKey
                ? "gap-1 bg-success/10 text-success border-success/20"
                : "gap-1"
            }
          >
            {hasApiKey ? (
              <CheckCircle className="h-3 w-3" />
            ) : (
              <XCircle className="h-3 w-3" />
            )}
            {hasApiKey ? "Configured" : "Not configured"}
          </Badge>
        </CardContent>
      </Card>

      {/* Configuration form */}
      <Card className="border-border/50">
        <CardHeader className="pb-4">
          <CardTitle className="text-base">Provider Settings</CardTitle>
        </CardHeader>
        <CardContent className="space-y-5">
          {/* Provider */}
          <div className="space-y-2">
            <Label htmlFor="provider">Provider</Label>
            <Select value={provider} onValueChange={setProvider}>
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Select provider" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="anthropic">Anthropic</SelectItem>
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">
              Currently only Anthropic is supported
            </p>
          </div>

          {/* API Key */}
          <div className="space-y-2">
            <Label htmlFor="apiKey">API Key</Label>
            <Input
              id="apiKey"
              type="password"
              placeholder={
                hasApiKey ? "••••••••••••••••••••" : "sk-ant-..."
              }
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              className="font-mono text-sm"
            />
            <p className="text-xs text-muted-foreground">
              {hasApiKey
                ? "Enter a new key to replace the existing one"
                : "Enter your Anthropic API key"}
            </p>
          </div>

          {/* Default Model */}
          <div className="space-y-2">
            <Label htmlFor="defaultModel">Default Model</Label>
            <div className="relative">
              <Select value={defaultModel} onValueChange={setDefaultModel} disabled={modelsLoading}>
                <SelectTrigger className="w-full">
                  {modelsLoading ? (
                    <span className="flex items-center gap-2 text-muted-foreground">
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      Loading models...
                    </span>
                  ) : (
                    <SelectValue placeholder="Select default model" />
                  )}
                </SelectTrigger>
                <SelectContent>
                  {models.map((model) => (
                    <SelectItem key={model.id} value={model.id}>
                      <span className="font-mono text-xs">{model.name}</span>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <p className="text-xs text-muted-foreground">
              {apiKey
                ? "Models loaded using the entered API key"
                : "Used for agent instances that don\u2019t specify a model"}
            </p>
          </div>

          {/* Status message */}
          {saveMessage && (
            <div
              className={`flex items-center gap-2 rounded-md border px-3 py-2 text-sm ${
                saveMessage.type === "success"
                  ? "border-success/20 bg-success/5 text-success"
                  : "border-error/20 bg-error/5 text-error"
              }`}
            >
              {saveMessage.type === "success" ? (
                <CheckCircle className="h-4 w-4 shrink-0" />
              ) : (
                <XCircle className="h-4 w-4 shrink-0" />
              )}
              <span>{saveMessage.text}</span>
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center gap-3 pt-2">
            <Button onClick={handleSave} disabled={saving} className="gap-2">
              {saving ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Save className="h-4 w-4" />
              )}
              {saving ? "Saving..." : "Save Configuration"}
            </Button>
            <Button
              variant="outline"
              onClick={handleTestConnection}
              disabled={testing || !hasApiKey}
              className="gap-2"
            >
              {testing ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Zap className="h-4 w-4" />
              )}
              {testing ? "Testing..." : "Test Connection"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// ============ Agent Instances Tab ============

function AgentInstancesTab() {
  const [instances, setInstances] = useState<InstanceRow[]>([]);
  const [models, setModels] = useState<ModelInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [instancesRes, templatesRes, modelsRes] = await Promise.all([
        apiClient.get<ApiResponse<AgentInstance[]>>("/api/agent-instances"),
        apiClient.get<ApiResponse<AgentTemplate[]>>("/api/agent-templates"),
        apiClient.get<ApiResponse<string[]>>("/api/settings/llm/models").catch(() => ({
          success: true,
          data: [] as string[],
          timestamp: "",
        })),
      ]);

      const templateMap = new Map(
        templatesRes.data.map((t) => [t.id, t])
      );

      const rawModels = modelsRes.data || [];
      const mappedModels: ModelInfo[] = rawModels.length > 0
        ? rawModels.map((id) => ({ id, name: id }))
        : [
            { id: "claude-sonnet-4-20250514", name: "Claude Sonnet 4" },
            { id: "claude-opus-4-20250514", name: "Claude Opus 4" },
            { id: "claude-haiku-4-20250414", name: "Claude Haiku 4" },
          ];
      setModels(mappedModels);

      const rows: InstanceRow[] = instancesRes.data.map((inst) => {
        const template = templateMap.get(inst.templateId);
        let model = "";
        try {
          const config = inst.config ? JSON.parse(inst.config) : {};
          model = config.model || "";
        } catch {
          // ignore parse errors
        }
        return {
          id: inst.id,
          name: inst.name,
          role: template?.role || "Unknown",
          status: inst.status,
          model,
          templateId: inst.templateId,
        };
      });

      setInstances(rows);
    } catch {
      // API not available
      setInstances([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleModelChange = async (instanceId: string, newModel: string) => {
    setUpdatingId(instanceId);
    try {
      await apiClient.patch(`/api/agent-instances/${instanceId}/model`, {
        model: newModel,
      });
      setInstances((prev) =>
        prev.map((inst) =>
          inst.id === instanceId ? { ...inst, model: newModel } : inst
        )
      );
      toast.success("Model updated");
    } catch (err) {
      toast.error("Failed to update model", {
        description: err instanceof Error ? err.message : "Unknown error",
      });
      // Revert on error - refetch
      await fetchData();
    } finally {
      setUpdatingId(null);
    }
  };

  const columns: ColumnDef<InstanceRow>[] = [
    {
      id: "name",
      header: "Name",
      accessorKey: "name",
      cell: (row) => (
        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-primary/10">
            <Bot className="h-3.5 w-3.5 text-primary" />
          </div>
          <span className="font-medium text-sm">{row.name}</span>
        </div>
      ),
    },
    {
      id: "role",
      header: "Role",
      accessorKey: "role",
      cell: (row) => (
        <span className="font-mono text-xs text-muted-foreground">
          {row.role}
        </span>
      ),
    },
    {
      id: "status",
      header: "Status",
      accessorKey: "status",
      cell: (row) => {
        const statusMap: Record<string, "idle" | "busy" | "degraded" | "quarantine" | "success" | "error" | "warning"> = {
          ACTIVE: "success",
          IDLE: "idle",
          BUSY: "busy",
          PAUSED: "warning",
          ERROR: "error",
          DISABLED: "quarantine",
        };
        const mapped = statusMap[row.status] || "idle";
        return <StatusBadge status={mapped}>{row.status}</StatusBadge>;
      },
    },
    {
      id: "model",
      header: "Model",
      accessorKey: "model",
      sortable: false,
      cell: (row) => (
        <div className="flex items-center gap-2">
          <Select
            value={row.model || ""}
            onValueChange={(value) => handleModelChange(row.id, value)}
            disabled={updatingId === row.id}
          >
            <SelectTrigger className="h-8 w-56" size="sm">
              <SelectValue placeholder="Select model">
                {updatingId === row.id ? (
                  <span className="flex items-center gap-1.5 text-muted-foreground">
                    <Loader2 className="h-3 w-3 animate-spin" />
                    Updating...
                  </span>
                ) : (
                  <span className="font-mono text-xs">
                    {row.model || "Not set"}
                  </span>
                )}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {models.map((model) => (
                <SelectItem key={model.id} value={model.id}>
                  <span className="font-mono text-xs">{model.name}</span>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      ),
    },
  ];

  return (
    <DataTable<InstanceRow>
      data={instances}
      columns={columns}
      loading={loading}
      searchKey="name"
      searchPlaceholder="Search agent instances..."
      emptyState={{
        icon: <Bot className="h-8 w-8 text-muted-foreground/40" />,
        title: "No agent instances",
        description:
          "Agent instances will appear here once they are created via the API.",
      }}
    />
  );
}

// ============ Role Agent Defaults Tab ============

const ROLES = ["ADMIN", "MANAGER", "TEAM_LEAD", "WORKER"] as const;

const ROLE_LABELS: Record<string, string> = {
  ADMIN: "Admin",
  MANAGER: "Manager",
  TEAM_LEAD: "Team Lead",
  WORKER: "Worker",
};

function RoleAgentTab() {
  const [defaults, setDefaults] = useState<RoleAgentDefaultResponse[]>([]);
  const [instances, setInstances] = useState<AgentInstance[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [assignments, setAssignments] = useState<Record<string, string>>({});

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [defaultsRes, instancesRes] = await Promise.all([
        apiClient
          .get<ApiResponse<RoleAgentDefaultResponse[]>>(
            "/api/settings/role-agent-defaults"
          )
          .catch(() => ({ data: [] as RoleAgentDefaultResponse[] })),
        apiClient.get<ApiResponse<AgentInstance[]>>("/api/agent-instances"),
      ]);

      setDefaults(defaultsRes.data);
      setInstances(instancesRes.data);

      const map: Record<string, string> = {};
      defaultsRes.data.forEach((d) => {
        map[d.role] = d.agentInstanceId;
      });
      setAssignments(map);
    } catch {
      // API not available
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const activeInstances = instances.filter((i) => i.status === "ACTIVE");

  const handleSave = async () => {
    setSaving(true);
    try {
      const updates: RoleAgentDefaultUpdateRequest[] = Object.entries(
        assignments
      )
        .filter(([, id]) => id)
        .map(([role, agentInstanceId]) => ({ role, agentInstanceId }));

      await apiClient.put<ApiResponse<RoleAgentDefaultResponse[]>>(
        "/api/settings/role-agent-defaults",
        updates
      );
      toast.success("Rollen-Zuweisungen gespeichert");
      await fetchData();
    } catch (err) {
      toast.error("Fehler beim Speichern", {
        description:
          err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setSaving(false);
    }
  };

  const getInstanceName = (instanceId: string) => {
    const inst = instances.find((i) => i.id === instanceId);
    return inst?.name || null;
  };

  if (loading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-10 w-64" />
        {ROLES.map((role) => (
          <Skeleton key={role} className="h-16 w-full" />
        ))}
      </div>
    );
  }

  return (
    <Card className="border-border/50">
      <CardHeader className="pb-4">
        <CardTitle className="text-base">
          Rollen-Default-Zuweisungen
        </CardTitle>
        <p className="text-sm text-muted-foreground">
          Jede Rolle hat einen Standard-Agent zugewiesen. Benutzer sehen diesen
          Agent im Agent Panel.
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Role rows */}
        <div className="rounded-md border border-border/50">
          <div className="grid grid-cols-[1fr_2fr] gap-4 border-b border-border/50 px-4 py-2.5 text-xs font-medium text-muted-foreground uppercase tracking-wider">
            <span>Rolle</span>
            <span>Zugewiesener Agent</span>
          </div>
          {ROLES.map((role) => {
            const currentName = assignments[role]
              ? getInstanceName(assignments[role])
              : null;
            return (
              <div
                key={role}
                className="grid grid-cols-[1fr_2fr] items-center gap-4 border-b border-border/50 px-4 py-3 last:border-b-0"
              >
                <div className="flex items-center gap-2.5">
                  <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/10">
                    <Shield className="h-4 w-4 text-primary" />
                  </div>
                  <div>
                    <span className="text-sm font-medium">
                      {ROLE_LABELS[role]}
                    </span>
                    <span className="ml-2 font-mono text-xs text-muted-foreground">
                      {role}
                    </span>
                  </div>
                </div>
                <Select
                  value={assignments[role] || ""}
                  onValueChange={(value) =>
                    setAssignments((prev) => ({ ...prev, [role]: value }))
                  }
                >
                  <SelectTrigger className="w-full max-w-sm">
                    <SelectValue placeholder="Agent auswählen...">
                      {currentName ? (
                        <span className="flex items-center gap-1.5">
                          <Bot className="h-3.5 w-3.5 text-muted-foreground" />
                          {currentName}
                        </span>
                      ) : (
                        <span className="text-muted-foreground">
                          Agent auswählen...
                        </span>
                      )}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {activeInstances.map((inst) => (
                      <SelectItem key={inst.id} value={inst.id}>
                        <span className="flex items-center gap-1.5">
                          <Bot className="h-3.5 w-3.5 text-muted-foreground" />
                          {inst.name}
                        </span>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            );
          })}
        </div>

        {/* Save button */}
        <div className="flex items-center gap-3 pt-2">
          <Button onClick={handleSave} disabled={saving} className="gap-2">
            {saving ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            {saving ? "Speichern..." : "Speichern"}
          </Button>
          {defaults.length > 0 && (
            <p className="text-xs text-muted-foreground">
              {defaults.length} Zuweisung{defaults.length !== 1 ? "en" : ""}{" "}
              konfiguriert
            </p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

// ============ Main Settings Page ============

export default function SettingsPage() {
  return (
    <div className="space-y-6">
        <PageHeader
          title="Settings"
          description="System configuration and LLM provider management"
          breadcrumb={["System", "Settings"]}
          actions={
            <div className="flex items-center gap-2">
              <Badge variant="outline" className="gap-1 font-mono text-xs">
                <Settings className="h-3 w-3" />
                Admin
              </Badge>
            </div>
          }
        />

        <Tabs defaultValue="llm" className="space-y-4">
          <TabsList>
            <TabsTrigger value="llm" className="gap-1.5">
              <Key className="h-3.5 w-3.5" />
              LLM Configuration
            </TabsTrigger>
            <TabsTrigger value="instances" className="gap-1.5">
              <Bot className="h-3.5 w-3.5" />
              Agent Instances
            </TabsTrigger>
            <TabsTrigger value="roles" className="gap-1.5">
              <Shield className="h-3.5 w-3.5" />
              Rollen & Agents
            </TabsTrigger>
          </TabsList>

          <TabsContent value="llm">
            <LlmConfigTab />
          </TabsContent>

          <TabsContent value="instances">
            <AgentInstancesTab />
          </TabsContent>

          <TabsContent value="roles">
            <RoleAgentTab />
          </TabsContent>
        </Tabs>
      </div>
  );
}
