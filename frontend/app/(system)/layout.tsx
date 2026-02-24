"use client";

import { useAuth } from "@/contexts/auth-context";
import { useRouter, usePathname } from "next/navigation";
import { useEffect } from "react";
import { SystemShell } from "@/components/layout/system-shell";

export default function SystemLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace(`/login?redirect=${encodeURIComponent(pathname)}`);
    }
    if (!isLoading && isAuthenticated && user?.role !== "SYSTEM_ADMIN") {
      router.replace("/login");
    }
  }, [isAuthenticated, isLoading, user, router, pathname]);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="font-mono text-sm text-muted-foreground">
          Loading...
        </div>
      </div>
    );
  }

  if (!isAuthenticated || user?.role !== "SYSTEM_ADMIN") {
    return null;
  }

  return <SystemShell>{children}</SystemShell>;
}
