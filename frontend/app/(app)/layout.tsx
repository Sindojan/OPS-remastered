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

function isPathAllowed(role: string, pathname: string): boolean {
  const blockedPaths = ROLE_BLOCKED_PATHS[role];
  if (!blockedPaths) return true;
  return !blockedPaths.some((p) => pathname === p || pathname.startsWith(p + "/"));
}

function getDefaultRoute(role: string): string {
  if (role === "WORKER") return "/my-day";
  if (role === "SYSTEM_ADMIN") return "/system/companies";
  return "/production";
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
      // Check role-based route access
      if (!isPathAllowed(user.role, pathname)) {
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
