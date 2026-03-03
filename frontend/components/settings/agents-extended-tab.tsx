"use client";

import { useState, useCallback, useEffect } from "react";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import { StatusBadge } from "@/components/shared/status-badge";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { useAgentTemplateDetail, useAgentTemplateMutations, useAvailableTools } from "@/hooks/api/use-settings";
import { apiClient } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "sonner";
import {
  Bot,
  Save,
  Loader2,
  RotateCcw,
  Wrench,
  Shield,
  AlertTriangle,
} from "lucide-react";
import type { AgentInstance, AgentTemplate, ApiResponse } from "@/types/api";

interface InstanceRow {
  id: string;
  name: string;
  role: string;
  status: string;
  model: string;
  templateId: string;
  customSystemPrompt: string | null;
}

interface ModelInfo {
  id: string;
  name: string;
}

const VALID_MODELS: ModelInfo[] = [
  { id: "claude-opus-4-6", name: "Claude Opus 4.6" },
  { id: "claude-sonnet-4-6", name: "Claude Sonnet 4.6" },
  { id: "claude-haiku-4-5-20251001", name: "Claude Haiku 4.5" },
];

const PERMISSION_CONFIG: Record<string, { label: string; className: string }> = {
  READ_ONLY: { label: "NUR_LESEN", className: "bg-success/10 text-success border-success/20" },
  WRITE: { label: "SCHREIBEN", className: "bg-warning/10 text-warning border-warning/20" },
  CRITICAL: { label: "KRITISCH", className: "bg-error/10 text-error border-error/20" },
};

export function AgentsExtendedTab() {
  const [instances, setInstances] = useState<InstanceRow[]>([]);
  const [models] = useState<ModelInfo[]>(VALID_MODELS);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  // Detail sheet
  const [selectedInstance, setSelectedInstance] = useState<InstanceRow | null>(null);
  const [showDetail, setShowDetail] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [instancesRes, templatesRes] = await Promise.all([
        apiClient.get<ApiResponse<AgentInstance[]>>("/api/agent-instances"),
        apiClient.get<ApiResponse<AgentTemplate[]>>("/api/agent-templates"),
      ]);

      const templateMap = new Map(templatesRes.data.map((t) => [t.id, t]));

      const rows: InstanceRow[] = instancesRes.data.map((inst) => {
        const template = templateMap.get(inst.templateId);
        let model = "";
        try {
          const config = inst.config ? JSON.parse(inst.config) : {};
          model = config.model || "";
        } catch {
          // ignore
        }
        return {
          id: inst.id,
          name: inst.name,
          role: template?.role || "Unknown",
          status: inst.status,
          model,
          templateId: inst.templateId,
          customSystemPrompt: inst.customSystemPrompt ?? null,
        };
      });
      setInstances(rows);
    } catch {
      setInstances([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleModelChange = async (instanceId: string, newModel: string) => {
    setUpdatingId(instanceId);
    try {
      await apiClient.patch(`/api/agent-instances/${instanceId}/model`, { model: newModel });
      setInstances((prev) => prev.map((inst) => inst.id === instanceId ? { ...inst, model: newModel } : inst));
      toast.success("Modell aktualisiert");
    } catch (err) {
      toast.error("Fehler beim Aktualisieren des Modells", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
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
      cell: (row) => <span className="font-mono text-xs text-muted-foreground">{row.role}</span>,
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
        return <StatusBadge status={statusMap[row.status] || "idle"}>{row.status}</StatusBadge>;
      },
    },
    {
      id: "model",
      header: "Modell",
      accessorKey: "model",
      sortable: false,
      cell: (row) => (
        <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
          <Select
            value={row.model || undefined}
            onValueChange={(value) => handleModelChange(row.id, value)}
            disabled={updatingId === row.id}
          >
            <SelectTrigger className="h-8 w-56" size="sm">
              <SelectValue placeholder="Modell wählen">
                {updatingId === row.id ? (
                  <span className="flex items-center gap-1.5 text-muted-foreground">
                    <Loader2 className="h-3 w-3 animate-spin" />
                    Aktualisieren...
                  </span>
                ) : (
                  <span className="font-mono text-xs">{VALID_MODELS.find(m => m.id === row.model)?.name || row.model || "Nicht gesetzt"}</span>
                )}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {VALID_MODELS.map((model) => (
                <SelectItem key={model.id} value={model.id}>
                  <span className="text-xs">{model.name}</span>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <DataTable<InstanceRow>
        data={instances}
        columns={columns}
        loading={loading}
        searchKey="name"
        searchPlaceholder="Agenten-Instanzen suchen..."
        onRowClick={(row) => { setSelectedInstance(row); setShowDetail(true); }}
        emptyState={{
          icon: <Bot className="h-8 w-8 text-muted-foreground/40" />,
          title: "Keine Agenten-Instanzen",
          description: "Agenten-Instanzen erscheinen hier, sobald sie über die API erstellt wurden.",
        }}
      />

      {/* Agent Detail Sheet */}
      {selectedInstance && (
        <AgentDetailSheet
          instance={selectedInstance}
          open={showDetail}
          onOpenChange={setShowDetail}
          models={models}
          onSaved={fetchData}
        />
      )}
    </div>
  );
}

// ─── Agent Detail Sheet ────────────────────────────────

interface AgentDetailSheetProps {
  instance: InstanceRow;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  models: ModelInfo[];
  onSaved: () => void;
}

function AgentDetailSheet({ instance, open, onOpenChange, models, onSaved }: AgentDetailSheetProps) {
  const { data: template, loading: templateLoading, refetch: refetchTemplate } = useAgentTemplateDetail(instance.templateId);
  const { data: availableTools, loading: toolsLoading } = useAvailableTools();
  const mutations = useAgentTemplateMutations();

  const [systemPrompt, setSystemPrompt] = useState("");
  const [maxTokens, setMaxTokens] = useState(0);
  const [enabledTools, setEnabledTools] = useState<Set<string>>(new Set());
  const [saving, setSaving] = useState(false);
  const [showResetDialog, setShowResetDialog] = useState(false);

  // Init form: system prompt from instance (or fall back to template), rest from template
  useEffect(() => {
    if (template) {
      setSystemPrompt(instance.customSystemPrompt || template.basePrompt || "");
      setMaxTokens(template.maxTokensPerRun || 4096);
      let tools: string[] = [];
      try {
        const parsed = JSON.parse(template.allowedTools || "[]");
        tools = Array.isArray(parsed) ? parsed : [];
      } catch {
        tools = [];
      }
      setEnabledTools(new Set(tools));
    }
  }, [template, instance.customSystemPrompt]);

  const handleSave = useCallback(async () => {
    if (!template) return;
    setSaving(true);
    try {
      // Save system prompt to instance
      await apiClient.patch(`/api/agent-instances/${instance.id}`, { systemPrompt });
      // Save tools + tokens to template
      await mutations.patch(template.id, {
        allowedTools: JSON.stringify(Array.from(enabledTools)),
        maxTokensPerRun: maxTokens,
      });
      toast.success("Agent-Konfiguration gespeichert");
      refetchTemplate();
      onSaved();
    } catch (err) {
      toast.error("Fehler beim Speichern", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setSaving(false);
    }
  }, [template, instance.id, systemPrompt, enabledTools, maxTokens, mutations, refetchTemplate, onSaved]);

  const handleReset = useCallback(async () => {
    if (!template) return;
    setSystemPrompt(template.basePrompt || "");
    setMaxTokens(template.maxTokensPerRun || 4096);
    let tools: string[] = [];
    try {
      const parsed = JSON.parse(template.allowedTools || "[]");
      tools = Array.isArray(parsed) ? parsed : [];
    } catch {
      tools = [];
    }
    setEnabledTools(new Set(tools));
    setShowResetDialog(false);
    toast.success("Auf Vorlage zurückgesetzt");
  }, [template]);

  const toggleTool = useCallback((toolName: string) => {
    setEnabledTools((prev) => {
      const next = new Set(prev);
      if (next.has(toolName)) next.delete(toolName);
      else next.add(toolName);
      return next;
    });
  }, []);

  const isLoading = templateLoading || toolsLoading;

  return (
    <>
      <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent className="w-full sm:max-w-xl overflow-y-auto">
          <SheetHeader>
            <SheetTitle className="flex items-center gap-2">
              <Bot className="h-4 w-4 text-primary" />
              {instance.name}
            </SheetTitle>
          </SheetHeader>

          {isLoading ? (
            <div className="space-y-6 mt-6">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-32 w-full" />
              <Skeleton className="h-64 w-full" />
            </div>
          ) : (
            <div className="space-y-6 mt-6">
              {/* Model Section */}
              <Card className="border-border/50">
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm">Modell</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-1.5">
                    <Label className="text-xs">Token-Budget pro Run</Label>
                    <Input
                      type="number"
                      value={maxTokens}
                      onChange={(e) => setMaxTokens(parseInt(e.target.value) || 0)}
                      min={256}
                      max={200000}
                    />
                  </div>
                </CardContent>
              </Card>

              {/* System Prompt Section */}
              <Card className="border-border/50">
                <CardHeader className="pb-3">
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-sm">System-Prompt</CardTitle>
                    <Button
                      variant="outline"
                      size="sm"
                      className="gap-1 text-xs"
                      onClick={() => setShowResetDialog(true)}
                    >
                      <RotateCcw className="h-3 w-3" />
                      Zurücksetzen
                    </Button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-2">
                  <div className="relative">
                    <Textarea
                      value={systemPrompt}
                      onChange={(e) => setSystemPrompt(e.target.value)}
                      rows={14}
                      className="font-mono text-xs resize-y min-h-[300px]"
                    />
                    <span className="absolute bottom-2 right-3 text-[10px] text-muted-foreground/60 font-mono pointer-events-none">
                      {systemPrompt.length.toLocaleString("de-DE")} Zeichen
                    </span>
                  </div>
                  <div className="flex items-start gap-1.5 text-xs text-muted-foreground">
                    <AlertTriangle className="h-3 w-3 shrink-0 mt-0.5" />
                    <span>Wirkt sofort auf neue Konversationen. Überschreibt den Template-Prompt für diese Instanz.</span>
                  </div>
                </CardContent>
              </Card>

              {/* Tool Whitelist Section */}
              <Card className="border-border/50">
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm flex items-center gap-1.5">
                    <Wrench className="h-3.5 w-3.5" />
                    Tool-Whitelist
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="divide-y divide-border/50 rounded-md border border-border/50">
                    {(availableTools || []).map((tool) => {
                      const permConfig = PERMISSION_CONFIG[tool.permissionLevel] || PERMISSION_CONFIG.READ_ONLY;
                      return (
                        <div key={tool.name} className="flex items-center justify-between px-3 py-2.5">
                          <div className="flex items-center gap-3 min-w-0 flex-1">
                            <Switch
                              checked={enabledTools.has(tool.name)}
                              onCheckedChange={() => toggleTool(tool.name)}
                            />
                            <div className="min-w-0">
                              <div className="flex items-center gap-2">
                                <span className="text-sm font-medium truncate">{tool.name}</span>
                                <Badge variant="outline" className={`text-[10px] shrink-0 ${permConfig.className}`}>
                                  <Shield className="h-2.5 w-2.5 mr-0.5" />
                                  {permConfig.label}
                                </Badge>
                              </div>
                              <p className="text-xs text-muted-foreground truncate">{tool.description}</p>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                    {(!availableTools || availableTools.length === 0) && (
                      <div className="px-3 py-6 text-center text-sm text-muted-foreground">
                        Keine Tools verfügbar.
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>

              {/* Save Button */}
              <div className="flex items-center gap-3 pb-6">
                <Button onClick={handleSave} disabled={saving} className="gap-2">
                  {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                  {saving ? "Speichern..." : "Speichern"}
                </Button>
              </div>
            </div>
          )}
        </SheetContent>
      </Sheet>

      <ConfirmationDialog
        open={showResetDialog}
        title="Auf Vorlage zurücksetzen?"
        description="Der System-Prompt und die Tool-Konfiguration werden auf die Werte der Vorlage zurückgesetzt. Nicht gespeicherte Änderungen gehen verloren."
        onConfirm={handleReset}
        onCancel={() => setShowResetDialog(false)}
        confirmLabel="Zurücksetzen"
      />
    </>
  );
}
