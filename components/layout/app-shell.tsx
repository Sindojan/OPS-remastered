"use client";

import { useState } from "react";
import { TooltipProvider } from "@/components/ui/tooltip";
import { Sidebar } from "./sidebar";
import { Topbar } from "./topbar";
import { AgentPanel } from "./agent-panel";

export function AppShell({ children }: { children: React.ReactNode }) {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [agentPanelOpen, setAgentPanelOpen] = useState(false);

  return (
    <TooltipProvider>
      <div className="flex h-screen overflow-hidden bg-background">
        <Sidebar
          collapsed={sidebarCollapsed}
          onToggle={() => setSidebarCollapsed(!sidebarCollapsed)}
        />
        <div className="flex flex-1 flex-col overflow-hidden">
          <Topbar
            onAgentPanelToggle={() => setAgentPanelOpen(!agentPanelOpen)}
            agentPanelOpen={agentPanelOpen}
          />
          <div className="flex flex-1 overflow-hidden">
            <main className="dot-grid flex-1 overflow-y-auto p-6">
              {children}
            </main>
            <AgentPanel
              open={agentPanelOpen}
              onClose={() => setAgentPanelOpen(false)}
            />
          </div>
        </div>
      </div>
    </TooltipProvider>
  );
}
