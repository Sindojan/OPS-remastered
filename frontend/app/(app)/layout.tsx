"use client";

import { useAuth } from "@/contexts/auth-context";
import { useRouter, usePathname } from "next/navigation";
import { useEffect } from "react";
import { AppShell } from "@/components/layout/app-shell";

// Paths blocked per role (everything else is allowed)
const ROLE_BLOCKED_PATHS: Record<string, string[]> = {
  WORKER: ["/settings"],
  TEAM_LEAD: ["/settings"],
};

// Route → module mapping for module-based access control
const PATH_TO_MODULE: Record<string, string> = {
  "/production": "production",
  "/machines": "machines",
  "/inventory": "inventory",
  "/employees": "people",
  "/customers": "customers",
  "/inbox": "inbox",
  "/parts-and-processes": "bom",
  "/knowledge": "knowledge",
};

function isPathAllowed(role: string, pathname: string, enabledModules: string[]): boolean {
  // Role-based blocking
  const blockedPaths = ROLE_BLOCKED_PATHS[role];
  if (blockedPaths?.some((p) => pathname === p || pathname.startsWith(p + "/"))) {
    return false;
  }

  // Module-based blocking
  for (const [pathPrefix, moduleId] of Object.entries(PATH_TO_MODULE)) {
    if (pathname === pathPrefix || pathname.startsWith(pathPrefix + "/")) {
      if (!enabledModules.includes(moduleId)) {
        return false;
      }
      break;
    }
  }

  return true;
}

function getDefaultRoute(role: string): string {
  if (role === "WORKER") return "/my-day";
  if (role === "SYSTEM_ADMIN") return "/system/companies";
  return "/my-day";
}

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { user, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace(`/login?redirect=${encodeURIComponent(pathname)}`);
      return;
    }
    if (!isLoading && isAuthenticated && user) {
      // SYSTEM_ADMIN should not access (app) routes
      if (user.role === "SYSTEM_ADMIN") {
        router.replace("/system/companies");
        return;
      }
      // Check role-based and module-based route access
      if (!isPathAllowed(user.role, pathname, user.enabledModules || [])) {
        router.replace(getDefaultRoute(user.role));
      }
    }
  }, [isAuthenticated, isLoading, user, router, pathname]);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="font-mono text-sm text-muted-foreground">Loading...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return <AppShell>{children}</AppShell>;
}
