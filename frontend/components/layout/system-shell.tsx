"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Bot,
  Building2,
  ChevronsLeft,
  ChevronsRight,
  ChevronRight,
  Key,
  LogOut,
  Shield,
} from "lucide-react";
import { ThemeToggle } from "@/components/shared/theme-toggle";
import { useAuth } from "@/contexts/auth-context";
import { SystemAgentPanel } from "./system-agent-panel";

const systemNavItems = [
  { label: "Unternehmen", href: "/system/companies", icon: Building2 },
  { label: "System-Agenten", href: "/system/agents", icon: Bot },
  { label: "API-Credentials", href: "/system/credentials", icon: Key },
];

const routeNames: Record<string, string> = {
  "/system/companies": "Unternehmen",
  "/system/agents": "System-Agenten",
  "/system/credentials": "API-Credentials",
};

export function SystemShell({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsed] = useState(false);
  const [agentPanelOpen, setAgentPanelOpen] = useState(false);
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();

  const pageName =
    routeNames[pathname] ??
    (pathname.startsWith("/system/companies/")
      ? "Unternehmensdetails"
      : pathname.startsWith("/system/agents/")
        ? "Agent-Details"
        : "System");

  const initials = user
    ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase()
    : "??";

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <TooltipProvider>
      <div className="flex h-screen overflow-hidden bg-background">
        {/* Sidebar */}
        <aside
          className={cn(
            "flex h-full flex-col bg-sidebar transition-all duration-300 ease-out",
            collapsed ? "w-[60px]" : "w-[240px]"
          )}
        >
          {/* Brand header */}
          <div className="flex h-14 items-center gap-2.5 border-b border-sidebar-border px-3">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-sidebar-primary/15">
              <Shield className="h-4 w-4 text-sidebar-primary" />
            </div>
            {!collapsed && (
              <div className="flex flex-col overflow-hidden">
                <span className="truncate text-sm font-bold tracking-tight text-sidebar-accent-foreground">
                  Owlsburg
                </span>
                <span className="truncate text-[10px] font-medium uppercase tracking-widest text-sidebar-foreground/50">
                  SYSTEM
                </span>
              </div>
            )}
          </div>

          {/* Navigation */}
          <nav className="flex-1 overflow-y-auto px-2 py-3">
            {!collapsed && (
              <span className="mb-1.5 block px-3 text-[10px] font-semibold uppercase tracking-widest text-sidebar-foreground/40">
                Verwaltung
              </span>
            )}
            <div className="space-y-0.5">
              {systemNavItems.map((item) => {
                const isActive =
                  pathname === item.href ||
                  pathname.startsWith(item.href + "/");
                const Icon = item.icon;

                const linkContent = (
                  <Link
                    href={item.href}
                    className={cn(
                      "group relative flex items-center gap-3 rounded-md px-3 py-2 text-[13px] font-medium transition-all duration-150",
                      isActive
                        ? "bg-sidebar-accent text-sidebar-accent-foreground"
                        : "text-sidebar-foreground/65 hover:bg-sidebar-accent/50 hover:text-sidebar-foreground",
                      collapsed && "justify-center px-0"
                    )}
                  >
                    {isActive && (
                      <span className="absolute left-0 top-1/2 h-4 w-[3px] -translate-y-1/2 rounded-r-full bg-sidebar-primary" />
                    )}
                    <Icon
                      className={cn(
                        "h-[18px] w-[18px] shrink-0 transition-colors",
                        isActive
                          ? "text-sidebar-primary"
                          : "text-sidebar-foreground/50 group-hover:text-sidebar-foreground/75"
                      )}
                    />
                    {!collapsed && <span>{item.label}</span>}
                  </Link>
                );

                if (collapsed) {
                  return (
                    <Tooltip key={item.href} delayDuration={0}>
                      <TooltipTrigger asChild>{linkContent}</TooltipTrigger>
                      <TooltipContent side="right" className="font-medium">
                        {item.label}
                      </TooltipContent>
                    </Tooltip>
                  );
                }

                return <div key={item.href}>{linkContent}</div>;
              })}
            </div>
          </nav>

          {/* Collapse toggle */}
          <div className="border-t border-sidebar-border p-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setCollapsed(!collapsed)}
              className={cn(
                "w-full text-sidebar-foreground/50 hover:bg-sidebar-accent/50 hover:text-sidebar-foreground",
                collapsed && "px-0"
              )}
            >
              {collapsed ? (
                <ChevronsRight className="h-4 w-4" />
              ) : (
                <>
                  <ChevronsLeft className="h-4 w-4" />
                  <span className="text-xs">Einklappen</span>
                </>
              )}
            </Button>
          </div>
        </aside>

        {/* Main area */}
        <div className="flex flex-1 flex-col overflow-hidden">
          {/* Topbar */}
          <header className="flex h-12 items-center justify-between border-b border-border bg-background/80 px-4 backdrop-blur-sm">
            <div className="flex items-center gap-1.5">
              <span className="text-xs font-medium text-muted-foreground">
                Owlsburg System
              </span>
              <ChevronRight className="h-3 w-3 text-muted-foreground/50" />
              <span className="text-[13px] font-semibold text-foreground">
                {pageName}
              </span>
            </div>

            <div className="flex items-center gap-1">
              {/* Agent Panel toggle */}
              <Tooltip delayDuration={0}>
                <TooltipTrigger asChild>
                  <Button
                    variant={agentPanelOpen ? "secondary" : "ghost"}
                    size="icon-sm"
                    onClick={() => setAgentPanelOpen(!agentPanelOpen)}
                  >
                    <Bot className={cn(
                      "h-4 w-4",
                      agentPanelOpen ? "text-primary" : "text-muted-foreground"
                    )} />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>System-CEO Chat</TooltipContent>
              </Tooltip>

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
                        {user
                          ? `${user.firstName} ${user.lastName}`
                          : "Unbekannt"}
                      </span>
                      <span className="text-xs text-muted-foreground">
                        {user?.email ?? ""}
                      </span>
                    </div>
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    onClick={handleLogout}
                    className="gap-2 text-destructive focus:text-destructive"
                  >
                    <LogOut className="h-3.5 w-3.5" />
                    Abmelden
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </header>

          {/* Content + Agent Panel */}
          <div className="flex flex-1 overflow-hidden">
            <main className="dot-grid flex-1 overflow-y-auto p-6">
              {children}
            </main>
            <SystemAgentPanel
              open={agentPanelOpen}
              onClose={() => setAgentPanelOpen(false)}
            />
          </div>
        </div>
      </div>
    </TooltipProvider>
  );
}
