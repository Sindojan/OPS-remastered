"use client";

import { useState } from "react";
import { useSystemAgentActivity } from "@/hooks/api/use-system-agent-activity";
import { HierarchyCanvas } from "./hierarchy-canvas";
import { AgentMonitorPanel } from "./agent-monitor-panel";
import { Loader2, Wifi, WifiOff } from "lucide-react";

/**
 * API base prefix for system agent monitor panel.
 * The AgentMonitorPanel uses tenant-scoped /api/agent-runs – for system agents
 * we override with /api/system/agents to hit the correct endpoints.
 */
export function SystemHierarchyView() {
  const { snapshot, realtimeEvents, activityLog, connected } = useSystemAgentActivity();
  const [selectedAgentId, setSelectedAgentId] = useState<string | null>(null);

  const selectedAgent = snapshot?.instances.find((i) => i.id === selectedAgentId) ?? null;

  if (!snapshot) {
    return (
      <div className="flex items-center justify-center py-24">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        <span className="ml-2 text-sm text-muted-foreground">Verbinde mit System-Agent-Netzwerk&hellip;</span>
      </div>
    );
  }

  return (
    <div className="flex h-full gap-0">
      <div className="flex-1 overflow-auto px-6 pt-4">
        <div className="mb-4 flex items-center gap-2 px-1">
          {connected ? (
            <Wifi className="h-3.5 w-3.5 text-success" />
          ) : (
            <WifiOff className="h-3.5 w-3.5 text-error" />
          )}
          <span className="font-mono text-[11px] text-muted-foreground">
            {connected ? "Live" : "Getrennt"} &middot; {snapshot.instances.length} System-Agents
          </span>
        </div>
        <HierarchyCanvas
          instances={snapshot.instances}
          activeLinks={snapshot.activeLinks}
          realtimeEvents={realtimeEvents}
          selectedAgentId={selectedAgentId}
          onSelectAgent={setSelectedAgentId}
        />
      </div>

      {selectedAgent && (
        <AgentMonitorPanel
          agent={selectedAgent}
          instances={snapshot.instances}
          realtimeEvent={realtimeEvents.get(selectedAgent.id)}
          activityLog={activityLog.get(selectedAgent.id) || []}
          onClose={() => setSelectedAgentId(null)}
          apiBase="/api/system"
        />
      )}
    </div>
  );
}
