"use client";

import { usePathname, useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Bot, ChevronRight, LogOut } from "lucide-react";
import { ThemeToggle } from "@/components/shared/theme-toggle";
import { useAuth } from "@/contexts/auth-context";
import { usePrimaryAgent } from "@/hooks/use-primary-agent";

interface TopbarProps {
  onAgentPanelToggle: () => void;
  agentPanelOpen: boolean;
}

const routeNames: Record<string, string> = {
  "/agents": "Agent Console",
  "/agents/hierarchy": "Agent Hierarchy",
  "/production": "Production Overview",
  "/production/planner": "Production Planner",
  "/machines": "Machines",
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
  const router = useRouter();
  const { user, logout } = useAuth();
  const { agent, loading: agentLoading } = usePrimaryAgent();
  const pageName = routeNames[pathname] ?? "Dashboard";

  const initials = user
    ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase()
    : "??";

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <header className="flex h-12 items-center justify-between border-b border-border bg-background/80 px-4 backdrop-blur-sm">
      <div className="flex items-center gap-1.5">
        <span className="text-xs font-medium text-muted-foreground">
          Owlsburg OPS
        </span>
        <ChevronRight className="h-3 w-3 text-muted-foreground/50" />
        <span className="text-[13px] font-semibold text-foreground">
          {pageName}
        </span>
      </div>

      <div className="flex items-center gap-1">
        {(agent || agentLoading) && (
          <Button
            variant={agentPanelOpen ? "default" : "ghost"}
            size="sm"
            onClick={onAgentPanelToggle}
            disabled={agentLoading}
            className={`gap-2 ${agentPanelOpen ? "shadow-sm shadow-primary/20" : ""}`}
          >
            <Bot className="h-3.5 w-3.5" />
            <span className="hidden text-xs sm:inline">
              {agentLoading ? "..." : agent?.name}
            </span>
          </Button>
        )}

        <ThemeToggle />

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon-sm">
              <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-[10px] font-bold text-primary">
                {initials}
              </div>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuLabel className="font-normal">
              <div className="flex flex-col gap-0.5">
                <span className="text-sm font-medium">
                  {user ? `${user.firstName} ${user.lastName}` : "Unknown"}
                </span>
                <span className="text-xs text-muted-foreground">
                  {user?.email ?? ""}
                </span>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={handleLogout} className="gap-2 text-destructive focus:text-destructive">
              <LogOut className="h-3.5 w-3.5" />
              Abmelden
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
