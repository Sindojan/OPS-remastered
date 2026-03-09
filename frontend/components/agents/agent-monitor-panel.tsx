"use client";

import { X, Bot, Clock, Zap } from "lucide-react";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/shared/status-badge";
import { Progress } from "@/components/ui/progress";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useApi } from "@/hooks/api/use-api";
import type { AgentInstanceActivity, AgentActivityEvent } from "@/types/api";

interface AgentMonitorPanelProps {
  agent: AgentInstanceActivity;
  instances: AgentInstanceActivity[];
  realtimeEvent?: AgentActivityEvent;
  activityLog: AgentActivityEvent[];
  onClose: () => void;
}

interface AgentRunResponse {
  id: string;
  instanceId: string;
  triggerType: string;
  status: string;
  inputContext: string | null;
  output: string | null;
  tokensUsed: number;
  startedAt: string | null;
  completedAt: string | null;
  errorMessage: string | null;
}

interface AgentRunStepResponse {
  id: string;
  runId: string;
  stepNumber: number;
  stepType: string;
  input: string | null;
  output: string | null;
  tokensUsed: number;
  durationMs: number;
  status: string;
  createdAt: string;
}

function mapStatus(status: string): "idle" | "busy" | "quarantine" | "degraded" {
  switch (status) {
    case "ACTIVE": return "busy";
    case "QUARANTINE": return "quarantine";
    case "TERMINATED": return "degraded";
    default: return "idle";
  }
}

function mapStatusLabel(status: string): string {
  switch (status) {
    case "ACTIVE": return "Aktiv";
    case "INACTIVE": return "Inaktiv";
    case "QUARANTINE": return "Quarantäne";
    case "TERMINATED": return "Beendet";
    default: return status;
  }
}

function formatModel(model: string): string {
  if (model.includes("opus")) return "Opus 4.6";
  if (model.includes("sonnet")) return "Sonnet 4.6";
  if (model.includes("haiku")) return "Haiku 4.5";
  return model;
}

function formatTime(iso: string | null): string {
  if (!iso) return "–";
  const d = new Date(iso);
  return d.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" });
}

function truncate(text: string, maxLen: number): string {
  if (text.length <= maxLen) return text;
  return text.slice(0, maxLen) + "\u2026";
}

function activityLabel(event: AgentActivityEvent): string {
  switch (event.type) {
    case "THINKING": return "Denkt\u2026";
    case "TOOL_CALL": return event.detail ? `Tool: ${event.detail}` : "Tool-Aufruf\u2026";
    case "TOOL_RESULT": return event.detail ? `Ergebnis: ${event.detail}` : "Ergebnis erhalten";
    case "DELEGATION_START": return "Delegiert\u2026";
    case "DELEGATION_END": return "Delegation fertig";
    default: return "";
  }
}

function eventTypeLabel(type: string): string {
  switch (type) {
    case "THINKING": return "Denken";
    case "TOOL_CALL": return "Tool-Aufruf";
    case "TOOL_RESULT": return "Tool-Ergebnis";
    case "DELEGATION_START": return "Delegation";
    case "DELEGATION_END": return "Delegation fertig";
    case "IDLE": return "Bereit";
    default: return type;
  }
}

function eventTypeColor(type: string): string {
  switch (type) {
    case "THINKING": return "text-blue-400";
    case "TOOL_CALL": return "text-amber-400";
    case "TOOL_RESULT": return "text-green-400";
    case "DELEGATION_START": return "text-purple-400";
    case "DELEGATION_END": return "text-purple-400";
    case "IDLE": return "text-muted-foreground";
    default: return "text-muted-foreground";
  }
}

function formatTimestamp(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

export function AgentMonitorPanel({ agent, instances, realtimeEvent, activityLog, onClose }: AgentMonitorPanelProps) {
  const parentAgent = instances.find((i) => i.id === agent.parentInstanceId);

  const { data: runs } = useApi<AgentRunResponse[]>(
    `/api/agent-runs?instanceId=${agent.id}`
  );

  const latestRun = runs && runs.length > 0 ? runs[0] : null;

  const { data: steps } = useApi<AgentRunStepResponse[]>(
    latestRun ? `/api/agent-runs/${latestRun.id}/steps` : null
  );

  const budgetPercent = agent.dailyTokenBudget > 0
    ? Math.min(100, Math.round((agent.tokensUsedToday / agent.dailyTokenBudget) * 100))
    : 0;

  return (
    <div className="flex h-full w-[420px] shrink-0 flex-col overflow-hidden border-l bg-card">
      {/* Header */}
      <div className="flex items-center justify-between border-b px-4 py-3">
        <div className="flex items-center gap-2">
          <Bot className="h-5 w-5 text-primary" />
          <div>
            <h3 className="text-sm font-semibold">{agent.name}</h3>
            <div className="flex items-center gap-1.5 mt-0.5">
              <StatusBadge status={mapStatus(agent.status)}>
                {mapStatusLabel(agent.status)}
              </StatusBadge>
              <span className="rounded bg-muted px-1.5 py-0.5 font-mono text-[10px] text-muted-foreground">
                {formatModel(agent.model)}
              </span>
            </div>
          </div>
        </div>
        <Button variant="ghost" size="icon" className="h-7 w-7" onClick={onClose}>
          <X className="h-4 w-4" />
        </Button>
      </div>

      {/* KPI Strip */}
      <div className="grid grid-cols-2 gap-3 border-b px-4 py-3">
        <div>
          <div className="flex items-center gap-1 text-[10px] text-muted-foreground">
            <Zap className="h-3 w-3" />
            Tokens heute
          </div>
          <div className="mt-1 flex items-center gap-2">
            <Progress value={budgetPercent} className="h-1.5 flex-1" />
            <span className="font-mono text-[11px] text-muted-foreground">{budgetPercent}%</span>
          </div>
          <div className="mt-0.5 font-mono text-[10px] text-muted-foreground">
            {agent.tokensUsedToday.toLocaleString("de-DE")} / {agent.dailyTokenBudget.toLocaleString("de-DE")}
          </div>
        </div>
        <div>
          <div className="flex items-center gap-1 text-[10px] text-muted-foreground">
            <Clock className="h-3 w-3" />
            Letzte Aktivität
          </div>
          <div className="mt-1 font-mono text-xs">
            {agent.lastActivityAt ? formatTime(agent.lastActivityAt) : latestRun ? formatTime(latestRun.startedAt) : "Keine"}
          </div>
        </div>
      </div>

      {/* Info section */}
      <div className="space-y-2 border-b px-4 py-3 text-xs">
        {parentAgent && (
          <div className="text-muted-foreground">
            <span className="text-muted-foreground/60">Vorgesetzter: </span>
            {parentAgent.name}
          </div>
        )}
        <div className="text-muted-foreground">
          <span className="text-muted-foreground/60">Rolle: </span>
          {agent.templateRole}
        </div>
        <div className="text-muted-foreground">
          <span className="text-muted-foreground/60">Typ: </span>
          {agent.type === "PERSISTENT" ? "Persistent" : "Ephemeral"}
        </div>
        {realtimeEvent && (
          <div>
            <span className="text-[10px] font-medium uppercase tracking-wider text-muted-foreground/60">
              Live-Aktivität
            </span>
            <p className="mt-0.5 font-mono text-[11px] text-primary animate-pulse">
              {activityLabel(realtimeEvent)}
            </p>
          </div>
        )}
        {agent.currentTask && !realtimeEvent && (
          <div>
            <span className="text-[10px] font-medium uppercase tracking-wider text-muted-foreground/60">
              Aktueller Auftrag
            </span>
            <p className="mt-0.5 font-mono text-[11px] italic text-muted-foreground">
              {agent.currentTask}
            </p>
          </div>
        )}
      </div>

      {/* Thought log — live activity events + DB fallback */}
      <div className="min-h-0 flex-1 overflow-hidden">
        <div className="px-4 py-2">
          <span className="text-[10px] font-medium uppercase tracking-wider text-muted-foreground/60">
            Gedankenprotokoll
          </span>
          {activityLog.length > 0 && (
            <span className="ml-2 font-mono text-[9px] text-muted-foreground">
              {activityLog.length} Events
            </span>
          )}
        </div>
        <ScrollArea className="h-full px-4 pb-4">
          {activityLog.length > 0 ? (
            <div className="space-y-1.5">
              {activityLog.map((evt, i) => (
                <div key={`${evt.timestamp}-${i}`} className="rounded border bg-muted/30 px-2 py-1.5">
                  <div className="flex items-center justify-between">
                    <span className={`font-mono text-[10px] font-medium ${eventTypeColor(evt.type)}`}>
                      {eventTypeLabel(evt.type)}
                    </span>
                    <span className="font-mono text-[9px] text-muted-foreground">
                      {formatTimestamp(evt.timestamp)}
                    </span>
                  </div>
                  {evt.detail && (
                    <p className="mt-0.5 font-mono text-[10px] text-foreground/80">
                      {truncate(evt.detail, 120)}
                    </p>
                  )}
                  {evt.agentName && evt.agentName !== agent.name && (
                    <p className="mt-0.5 font-mono text-[9px] text-muted-foreground">
                      von {evt.agentName}
                    </p>
                  )}
                </div>
              ))}
            </div>
          ) : !steps || steps.length === 0 ? (
            <p className="py-4 text-center text-xs text-muted-foreground">
              {latestRun ? "Keine Steps vorhanden." : "Keine Aktivität."}
            </p>
          ) : (
            <div className="space-y-2">
              {steps.map((step) => (
                <div key={step.id} className="rounded border bg-muted/30 p-2">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-[10px] font-medium text-primary/80">
                      #{step.stepNumber} {step.stepType}
                    </span>
                    <span className="font-mono text-[9px] text-muted-foreground">
                      {step.tokensUsed > 0 && `${step.tokensUsed} tok`}
                      {step.durationMs > 0 && ` \u00B7 ${step.durationMs}ms`}
                    </span>
                  </div>
                  {step.input && (
                    <pre className="mt-1 overflow-x-auto whitespace-pre-wrap font-mono text-[10px] text-muted-foreground">
                      {truncate(step.input, 300)}
                    </pre>
                  )}
                  {step.output && (
                    <pre className="mt-1 overflow-x-auto whitespace-pre-wrap font-mono text-[10px] text-foreground/80">
                      {truncate(step.output, 300)}
                    </pre>
                  )}
                  {step.status === "FAILED" && (
                    <span className="mt-1 inline-block text-[10px] text-error">Fehlgeschlagen</span>
                  )}
                </div>
              ))}
            </div>
          )}
        </ScrollArea>
      </div>
    </div>
  );
}
