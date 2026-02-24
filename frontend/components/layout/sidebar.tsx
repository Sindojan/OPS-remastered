"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  Bot,
  Building2,
  Factory,
  Package,
  Puzzle,
  Inbox,
  BarChart3,
  Users,
  BookOpen,
  Settings,
  ChevronsLeft,
  ChevronsRight,
  Zap,
  Cog,
  Shield,
} from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { Sun } from "lucide-react";

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

interface NavItem {
  label: string;
  href: string;
  icon: typeof Bot;
}

interface NavSection {
  label: string;
  items: NavItem[];
}

const ALL_NAV_SECTIONS: NavSection[] = [
  {
    label: "Betrieb",
    items: [
      { label: "Mein Tag", href: "/my-day", icon: Sun },
      { label: "Konsole", href: "/agents", icon: Bot },
      { label: "Produktion", href: "/production", icon: Factory },
      { label: "Maschinen", href: "/machines", icon: Cog },
      { label: "Lager", href: "/inventory", icon: Package },
      { label: "Teile & Prozesse", href: "/parts-and-processes", icon: Puzzle },
    ],
  },
  {
    label: "Kommunikation",
    items: [
      { label: "Posteingang", href: "/inbox", icon: Inbox },
      { label: "Berichte", href: "/reports", icon: BarChart3 },
    ],
  },
  {
    label: "Organisation",
    items: [
      { label: "Kunden", href: "/customers", icon: Building2 },
      { label: "Mitarbeiter", href: "/employees", icon: Users },
      { label: "Wissensdatenbank", href: "/knowledge", icon: BookOpen },
      { label: "Einstellungen", href: "/settings", icon: Settings },
    ],
  },
];

// Paths hidden per role (everything else is visible)
const ROLE_HIDDEN_PATHS: Record<string, string[]> = {
  WORKER: ["/settings"],
  TEAM_LEAD: ["/settings"],
  // MANAGER and ADMIN see everything
};

function getNavSectionsForRole(role: string): NavSection[] {
  const hiddenPaths = ROLE_HIDDEN_PATHS[role];
  if (!hiddenPaths) return ALL_NAV_SECTIONS;

  const sections: NavSection[] = [];
  for (const section of ALL_NAV_SECTIONS) {
    const filteredItems = section.items.filter(
      (item) => !hiddenPaths.includes(item.href)
    );
    if (filteredItems.length > 0) {
      sections.push({ ...section, items: filteredItems });
    }
  }
  return sections;
}

export function Sidebar({ collapsed, onToggle }: SidebarProps) {
  const pathname = usePathname();
  const { user } = useAuth();
  const isSystemAdmin = user?.role === "SYSTEM_ADMIN";
  const navSections = getNavSectionsForRole(user?.role || "WORKER");

  return (
    <aside
      className={cn(
        "flex h-full flex-col bg-sidebar transition-all duration-300 ease-out",
        collapsed ? "w-[60px]" : "w-[240px]"
      )}
    >
      {/* Brand header */}
      <div className="flex h-14 items-center gap-2.5 border-b border-sidebar-border px-3">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-sidebar-primary/15">
          <Zap className="h-4 w-4 text-sidebar-primary" />
        </div>
        {!collapsed && (
          <div className="flex flex-col overflow-hidden">
            <span className="truncate text-sm font-bold tracking-tight text-sidebar-accent-foreground">
              Owlsburg
            </span>
            <span className="truncate text-[10px] font-medium uppercase tracking-widest text-sidebar-foreground/50">
              OPS
            </span>
          </div>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto px-2 py-3">
        {navSections.map((section, sIdx) => (
          <div key={sIdx} className={cn(sIdx > 0 && "mt-5")}>
            {!collapsed && (
              <span className="mb-1.5 block px-3 text-[10px] font-semibold uppercase tracking-widest text-sidebar-foreground/40">
                {section.label}
              </span>
            )}
            {collapsed && sIdx > 0 && (
              <div className="mx-2 mb-2 border-t border-sidebar-border" />
            )}
            <div className="space-y-0.5">
              {section.items.map((item) => {
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
                    <Icon className={cn(
                      "h-[18px] w-[18px] shrink-0 transition-colors",
                      isActive ? "text-sidebar-primary" : "text-sidebar-foreground/50 group-hover:text-sidebar-foreground/75"
                    )} />
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
          </div>
        ))}
      </nav>

      {/* System Admin Link */}
      {isSystemAdmin && (
        <div className="border-t border-sidebar-border px-2 py-2">
          {(() => {
            const isActive =
              pathname === "/system/companies" ||
              pathname.startsWith("/system/companies/");
            const linkContent = (
              <Link
                href="/system/companies"
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
                <Shield
                  className={cn(
                    "h-[18px] w-[18px] shrink-0 transition-colors",
                    isActive
                      ? "text-sidebar-primary"
                      : "text-sidebar-foreground/50 group-hover:text-sidebar-foreground/75"
                  )}
                />
                {!collapsed && <span>System</span>}
              </Link>
            );

            if (collapsed) {
              return (
                <Tooltip delayDuration={0}>
                  <TooltipTrigger asChild>{linkContent}</TooltipTrigger>
                  <TooltipContent side="right" className="font-medium">
                    System
                  </TooltipContent>
                </Tooltip>
              );
            }

            return linkContent;
          })()}
        </div>
      )}

      {/* Collapse toggle */}
      <div className="border-t border-sidebar-border p-2">
        <Button
          variant="ghost"
          size="sm"
          onClick={onToggle}
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
  );
}
