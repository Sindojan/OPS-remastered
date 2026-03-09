"use client";

import { Bot } from "lucide-react";
import { cn } from "@/lib/utils";
import { StatusBadge } from "@/components/shared/status-badge";
import { Progress } from "@/components/ui/progress";
import type { AgentInstanceActivity, AgentActivityEvent } from "@/types/api";

interface AgentNodeCardProps {
  agent: AgentInstanceActivity;
  parentName?: string;
  isActive: boolean;
  isSelected: boolean;
  onSelect: (id: string) => void;
  realtimeEvent?: AgentActivityEvent;
}

function mapStatus(status: string): "idle" | "busy" | "quarantine" | "degraded" {
  switch (status) {
    case "ACTIVE":
      return "busy";
    case "QUARANTINE":
      return "quarantine";
    case "TERMINATED":
      return "degraded";
    default:
      return "idle";
  }
}

function mapStatusLabel(status: string): string {
  switch (status) {
    case "ACTIVE":
      return "Aktiv";
    case "INACTIVE":
      return "Inaktiv";
    case "QUARANTINE":
      return "Quarantäne";
    case "TERMINATED":
      return "Beendet";
    default:
      return status;
  }
}

function formatModel(model: string): string {
  if (model.includes("opus")) return "Opus";
  if (model.includes("sonnet")) return "Sonnet";
  if (model.includes("haiku")) return "Haiku";
  return model;
}

function truncate(text: string, maxLen: number): string {
  if (text.length <= maxLen) return text;
  return text.slice(0, maxLen) + "\u2026";
}

function activityLabel(event: AgentActivityEvent): string {
  switch (event.type) {
    case "THINKING":
      return "Denkt\u2026";
    case "TOOL_CALL":
      return event.detail ? `Tool: ${event.detail}` : "Tool-Aufruf\u2026";
    case "TOOL_RESULT":
      return event.detail ? `Ergebnis: ${event.detail}` : "Ergebnis erhalten";
    case "DELEGATION_START":
      return "Delegiert\u2026";
    case "DELEGATION_END":
      return "Delegation fertig";
    default:
      return "";
  }
}

export function AgentNodeCard({ agent, parentName, isActive, isSelected, onSelect, realtimeEvent }: AgentNodeCardProps) {
  const isCeo = agent.templateRole === "ceo";
  const isLead = agent.type === "PERSISTENT" && !isCeo;
  const budgetPercent = agent.dailyTokenBudget > 0
    ? Math.min(100, Math.round((agent.tokensUsedToday / agent.dailyTokenBudget) * 100))
    : 0;

  const hasRealtimeActivity = !!realtimeEvent;

  return (
    <div
      onClick={() => onSelect(agent.id)}
      className={cn(
        "group cursor-pointer rounded-lg border bg-card transition-all hover:shadow-md",
        isCeo ? "w-72 p-5" : isLead ? "w-60 p-4" : "w-52 p-3.5",
        isSelected && "ring-2 ring-primary",
        hasRealtimeActivity && "ring-2 ring-primary/50 animate-pulse",
        isActive && !hasRealtimeActivity && "border-primary/60 shadow-[0_0_8px_-2px] shadow-primary/20",
        !isSelected && !isActive && !hasRealtimeActivity && "hover:border-foreground/20"
      )}
    >
      <div className="flex items-center gap-2.5">
        <Bot className={cn("shrink-0 text-muted-foreground", isCeo ? "h-6 w-6" : "h-5 w-5")} />
        <span className={cn("truncate font-semibold", isCeo ? "text-base" : isLead ? "text-sm" : "text-xs")}>
          {agent.name}
        </span>
      </div>

      <div className="mt-3 flex items-center gap-2">
        <StatusBadge status={mapStatus(agent.status)}>
          {mapStatusLabel(agent.status)}
        </StatusBadge>
        <span className="rounded bg-muted px-2 py-0.5 font-mono text-[11px] text-muted-foreground">
          {formatModel(agent.model)}
        </span>
      </div>

      {realtimeEvent && (
        <div className="mt-2 truncate font-mono text-[11px] text-primary">
          {activityLabel(realtimeEvent)}
        </div>
      )}

      <div className={cn("mt-3 space-y-1.5 font-mono", isCeo ? "text-xs" : "text-[11px]")}>
        {parentName && (
          <div className="text-muted-foreground">
            <span className="text-muted-foreground/60">Vorgesetzter: </span>
            {parentName}
          </div>
        )}
        <div className="flex items-center gap-2">
          <span className="shrink-0 text-muted-foreground/60">Tokens:</span>
          <Progress value={budgetPercent} className="h-2 flex-1" />
          <span className="shrink-0 text-muted-foreground">{budgetPercent}%</span>
        </div>
        {agent.currentTask && !realtimeEvent && (
          <div className="italic text-muted-foreground">
            {truncate(agent.currentTask, isCeo ? 80 : 50)}
          </div>
        )}
      </div>
    </div>
  );
}
