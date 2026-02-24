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
  BookOpen,
  Plus,
  Pencil,
  Trash2,
  X,
  Tag,
} from "lucide-react";
import type {
  ApiResponse,
  LlmConfig,
  AgentInstance,
  AgentTemplate,
  RoleAgentDefaultResponse,
  RoleAgentDefaultUpdateRequest,
  KnowledgeCategoryResponse,
  KnowledgeTagResponse,
} from "@/types/api";
import { toast } from "sonner";
import {
  useKnowledgeCategories,
  useKnowledgeCategoryMutations,
  useKnowledgeTags,
  useKnowledgeTagMutations,
} from "@/hooks/api/use-knowledge";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";

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
      toast.success("Konfiguration erfolgreich gespeichert");
      setHasApiKey(true);
      setApiKey("");
      await fetchModels();
    } catch (err) {
      toast.error("Fehler beim Speichern der Konfiguration", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
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
      toast.success(`Verbindung erfolgreich. ${models.length} Modelle verfuegbar.`);
    } catch (err) {
      toast.error("Verbindungstest fehlgeschlagen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
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
            <p className="text-sm font-medium">API-Schluessel-Status</p>
            <p className="text-xs text-muted-foreground">
              {hasApiKey
                ? "API-Schluessel ist konfiguriert und bereit"
                : "Noch kein API-Schluessel konfiguriert"}
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
            {hasApiKey ? "Konfiguriert" : "Nicht konfiguriert"}
          </Badge>
        </CardContent>
      </Card>

      {/* Configuration form */}
      <Card className="border-border/50">
        <CardHeader className="pb-4">
          <CardTitle className="text-base">Anbieter-Einstellungen</CardTitle>
        </CardHeader>
        <CardContent className="space-y-5">
          {/* Provider */}
          <div className="space-y-2">
            <Label htmlFor="provider">Anbieter</Label>
            <Select value={provider} onValueChange={setProvider}>
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Anbieter waehlen" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="anthropic">Anthropic</SelectItem>
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">
              Derzeit wird nur Anthropic unterstuetzt
            </p>
          </div>

          {/* API Key */}
          <div className="space-y-2">
            <Label htmlFor="apiKey">API-Schluessel</Label>
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
                ? "Neuen Schluessel eingeben, um den bestehenden zu ersetzen"
                : "Geben Sie Ihren Anthropic API-Schluessel ein"}
            </p>
          </div>

          {/* Default Model */}
          <div className="space-y-2">
            <Label htmlFor="defaultModel">Standard-Modell</Label>
            <div className="relative">
              <Select value={defaultModel} onValueChange={setDefaultModel} disabled={modelsLoading}>
                <SelectTrigger className="w-full">
                  {modelsLoading ? (
                    <span className="flex items-center gap-2 text-muted-foreground">
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      Modelle laden...
                    </span>
                  ) : (
                    <SelectValue placeholder="Standard-Modell waehlen" />
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
                ? "Modelle mit dem eingegebenen API-Schluessel geladen"
                : "Wird fuer Agenten-Instanzen verwendet, die kein Modell angeben"}
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
              {saving ? "Speichern..." : "Konfiguration speichern"}
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
              {testing ? "Testen..." : "Verbindung testen"}
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
      toast.success("Modell aktualisiert");
    } catch (err) {
      toast.error("Fehler beim Aktualisieren des Modells", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
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
      header: "Rolle",
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
      header: "Modell",
      accessorKey: "model",
      sortable: false,
      cell: (row) => (
        <div className="flex items-center gap-2">
          <Select
            value={row.model || undefined}
            onValueChange={(value) => handleModelChange(row.id, value)}
            disabled={updatingId === row.id}
          >
            <SelectTrigger className="h-8 w-56" size="sm">
              <SelectValue placeholder="Modell waehlen">
                {updatingId === row.id ? (
                  <span className="flex items-center gap-1.5 text-muted-foreground">
                    <Loader2 className="h-3 w-3 animate-spin" />
                    Aktualisieren...
                  </span>
                ) : (
                  <span className="font-mono text-xs">
                    {row.model || "Nicht gesetzt"}
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
      searchPlaceholder="Agenten-Instanzen suchen..."
      emptyState={{
        icon: <Bot className="h-8 w-8 text-muted-foreground/40" />,
        title: "Keine Agenten-Instanzen",
        description:
          "Agenten-Instanzen erscheinen hier, sobald sie ueber die API erstellt wurden.",
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
                  value={assignments[role] || undefined}
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

// ============ Knowledge Settings Tab ============

function KnowledgeSettingsTab() {
  const { data: categories, loading: catsLoading, refetch: refetchCats } = useKnowledgeCategories();
  const catMutations = useKnowledgeCategoryMutations();
  const { data: allTags, loading: tagsLoading, refetch: refetchTags } = useKnowledgeTags();
  const tagMutations = useKnowledgeTagMutations();

  // Category form state
  const [showCatForm, setShowCatForm] = useState(false);
  const [editingCatId, setEditingCatId] = useState<string | null>(null);
  const [catName, setCatName] = useState("");
  const [catColor, setCatColor] = useState("#3b82f6");

  // Tag form state
  const [showTagForm, setShowTagForm] = useState(false);
  const [tagName, setTagName] = useState("");

  // Delete confirmation
  const [deleteCatId, setDeleteCatId] = useState<string | null>(null);
  const [deleteTagId, setDeleteTagId] = useState<string | null>(null);

  // Category handlers
  const openCatForm = useCallback((cat?: KnowledgeCategoryResponse) => {
    if (cat) {
      setEditingCatId(cat.id);
      setCatName(cat.name);
      setCatColor(cat.color || "#3b82f6");
    } else {
      setEditingCatId(null);
      setCatName("");
      setCatColor("#3b82f6");
    }
    setShowCatForm(true);
  }, []);

  const handleSaveCat = useCallback(async () => {
    if (!catName.trim()) return;
    try {
      if (editingCatId) {
        await catMutations.updateCategory(editingCatId, { name: catName, color: catColor });
        toast.success("Kategorie aktualisiert");
      } else {
        await catMutations.createCategory({ name: catName, color: catColor });
        toast.success("Kategorie erstellt");
      }
      setShowCatForm(false);
      setCatName("");
      setCatColor("#3b82f6");
      setEditingCatId(null);
      refetchCats();
    } catch (err) {
      toast.error("Fehler beim Speichern der Kategorie", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [catName, catColor, editingCatId, catMutations, refetchCats]);

  const handleDeleteCat = useCallback(async () => {
    if (!deleteCatId) return;
    try {
      await catMutations.deleteCategory(deleteCatId);
      toast.success("Kategorie gelöscht");
      setDeleteCatId(null);
      refetchCats();
    } catch (err) {
      toast.error("Fehler beim Löschen der Kategorie", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [deleteCatId, catMutations, refetchCats]);

  // Tag handlers
  const handleSaveTag = useCallback(async () => {
    if (!tagName.trim()) return;
    try {
      await tagMutations.createTag(tagName.trim());
      toast.success("Tag erstellt");
      setShowTagForm(false);
      setTagName("");
      refetchTags();
    } catch (err) {
      toast.error("Fehler beim Erstellen des Tags", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [tagName, tagMutations, refetchTags]);

  const handleDeleteTag = useCallback(async () => {
    if (!deleteTagId) return;
    try {
      await tagMutations.deleteTag(deleteTagId);
      toast.success("Tag gelöscht");
      setDeleteTagId(null);
      refetchTags();
    } catch (err) {
      toast.error("Fehler beim Löschen des Tags", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [deleteTagId, tagMutations, refetchTags]);

  if (catsLoading || tagsLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Categories */}
      <Card className="border-border/50">
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Kategorien-Verwaltung</CardTitle>
            <Button
              variant="outline"
              size="sm"
              className="gap-1.5"
              onClick={() => openCatForm()}
            >
              <Plus className="h-3.5 w-3.5" />
              Neue Kategorie
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          {/* Inline form */}
          {showCatForm && (
            <div className="flex items-end gap-3 rounded-md border border-border/50 bg-muted/30 p-3">
              <div className="flex-1 space-y-1.5">
                <Label className="text-xs">Name</Label>
                <Input
                  value={catName}
                  onChange={(e) => setCatName(e.target.value)}
                  placeholder="Kategoriename..."
                  className="text-sm"
                  onKeyDown={(e) => {
                    if (e.key === "Enter") handleSaveCat();
                  }}
                />
              </div>
              <div className="space-y-1.5">
                <Label className="text-xs">Farbe</Label>
                <Input
                  type="color"
                  value={catColor}
                  onChange={(e) => setCatColor(e.target.value)}
                  className="h-9 w-16 cursor-pointer p-1"
                />
              </div>
              <Button size="sm" onClick={handleSaveCat} disabled={!catName.trim() || catMutations.loading}>
                {catMutations.loading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Save className="h-3.5 w-3.5" />}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setShowCatForm(false);
                  setEditingCatId(null);
                }}
              >
                <X className="h-3.5 w-3.5" />
              </Button>
            </div>
          )}

          {/* Category list */}
          {(categories || []).length === 0 && !showCatForm ? (
            <p className="text-sm text-muted-foreground py-4 text-center">
              Noch keine Kategorien vorhanden.
            </p>
          ) : (
            <div className="divide-y divide-border/50 rounded-md border border-border/50">
              {(categories || []).map((cat) => (
                <div key={cat.id} className="flex items-center justify-between px-4 py-2.5">
                  <div className="flex items-center gap-2.5">
                    <span
                      className="inline-block h-3 w-3 rounded-full"
                      style={{ backgroundColor: cat.color || "#6b7280" }}
                    />
                    <span className="text-sm font-medium">{cat.name}</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7"
                      onClick={() => openCatForm(cat)}
                    >
                      <Pencil className="h-3 w-3" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7 text-destructive hover:text-destructive"
                      onClick={() => setDeleteCatId(cat.id)}
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Tags */}
      <Card className="border-border/50">
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Tags-Verwaltung</CardTitle>
            <Button
              variant="outline"
              size="sm"
              className="gap-1.5"
              onClick={() => setShowTagForm(true)}
            >
              <Plus className="h-3.5 w-3.5" />
              Neuer Tag
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          {/* Inline form */}
          {showTagForm && (
            <div className="flex items-end gap-3 rounded-md border border-border/50 bg-muted/30 p-3">
              <div className="flex-1 space-y-1.5">
                <Label className="text-xs">Name</Label>
                <Input
                  value={tagName}
                  onChange={(e) => setTagName(e.target.value)}
                  placeholder="Tagname..."
                  className="text-sm"
                  onKeyDown={(e) => {
                    if (e.key === "Enter") handleSaveTag();
                  }}
                />
              </div>
              <Button size="sm" onClick={handleSaveTag} disabled={!tagName.trim() || tagMutations.loading}>
                {tagMutations.loading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Save className="h-3.5 w-3.5" />}
              </Button>
              <Button variant="ghost" size="sm" onClick={() => setShowTagForm(false)}>
                <X className="h-3.5 w-3.5" />
              </Button>
            </div>
          )}

          {/* Tag list */}
          {(allTags || []).length === 0 && !showTagForm ? (
            <p className="text-sm text-muted-foreground py-4 text-center">
              Noch keine Tags vorhanden.
            </p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {(allTags || []).map((tag) => (
                <Badge
                  key={tag.id}
                  variant="secondary"
                  className="gap-1.5 text-sm py-1 px-2.5"
                >
                  <Tag className="h-3 w-3" />
                  {tag.name}
                  <button
                    onClick={() => setDeleteTagId(tag.id)}
                    className="ml-1 hover:text-destructive transition-colors"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </Badge>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Delete Confirmations */}
      <ConfirmationDialog
        open={!!deleteCatId}
        title="Kategorie löschen?"
        description="Die Kategorie wird unwiderruflich gelöscht. Artikel mit dieser Kategorie behalten ihren Inhalt."
        onConfirm={handleDeleteCat}
        onCancel={() => setDeleteCatId(null)}
        variant="destructive"
        confirmLabel="Kategorie löschen"
      />
      <ConfirmationDialog
        open={!!deleteTagId}
        title="Tag löschen?"
        description="Der Tag wird von allen Artikeln entfernt und unwiderruflich gelöscht."
        onConfirm={handleDeleteTag}
        onCancel={() => setDeleteTagId(null)}
        variant="destructive"
        confirmLabel="Tag löschen"
      />
    </div>
  );
}

// ============ Main Settings Page ============

export default function SettingsPage() {
  return (
    <div className="space-y-6">
        <PageHeader
          title="Einstellungen"
          description="Systemkonfiguration und LLM-Anbieterverwaltung"
          breadcrumb={["System", "Einstellungen"]}
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
              LLM-Konfiguration
            </TabsTrigger>
            <TabsTrigger value="instances" className="gap-1.5">
              <Bot className="h-3.5 w-3.5" />
              Agenten-Instanzen
            </TabsTrigger>
            <TabsTrigger value="roles" className="gap-1.5">
              <Shield className="h-3.5 w-3.5" />
              Rollen & Agents
            </TabsTrigger>
            <TabsTrigger value="knowledge" className="gap-1.5">
              <BookOpen className="h-3.5 w-3.5" />
              Wissen
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

          <TabsContent value="knowledge">
            <KnowledgeSettingsTab />
          </TabsContent>
        </Tabs>
      </div>
  );
}
