"use client";

import { usePathname } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Bot, ChevronRight, User } from "lucide-react";
import { ThemeToggle } from "@/components/shared/theme-toggle";

interface TopbarProps {
  onAgentPanelToggle: () => void;
  agentPanelOpen: boolean;
}

const routeNames: Record<string, string> = {
  "/agents": "Agent Console",
  "/agents/hierarchy": "Agent Hierarchy",
  "/production": "Production Overview",
  "/production/planner": "Production Planner",
  "/inventory": "Inventory",
  "/parts": "Parts & Components",
  "/process-plans": "Process Plans",
  "/inbox": "Inbox",
  "/reports": "Reports",
  "/employees": "Employees",
  "/my-day": "My Day",
  "/knowledge": "Knowledge Base",
  "/settings": "Settings",
};

export function Topbar({ onAgentPanelToggle, agentPanelOpen }: TopbarProps) {
  const pathname = usePathname();
  const pageName = routeNames[pathname] ?? "Dashboard";

  return (
    <header className="flex h-12 items-center justify-between border-b border-border bg-background/80 px-4 backdrop-blur-sm">
      <div className="flex items-center gap-1.5">
        <span className="text-xs font-medium text-muted-foreground">
          Sindojan
        </span>
        <ChevronRight className="h-3 w-3 text-muted-foreground/50" />
        <span className="text-[13px] font-semibold text-foreground">
          {pageName}
        </span>
      </div>

      <div className="flex items-center gap-1">
        <Button
          variant={agentPanelOpen ? "default" : "ghost"}
          size="sm"
          onClick={onAgentPanelToggle}
          className={`gap-2 ${agentPanelOpen ? "shadow-sm shadow-primary/20" : ""}`}
        >
          <Bot className="h-3.5 w-3.5" />
          <span className="hidden text-xs sm:inline">CEO Agent</span>
        </Button>

        <ThemeToggle />

        <Button variant="ghost" size="icon-sm">
          <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-[10px] font-bold text-primary">
            PH
          </div>
        </Button>
      </div>
    </header>
  );
}
