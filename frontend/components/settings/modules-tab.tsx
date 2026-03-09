"use client";

import { useCallback } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Package } from "lucide-react";
import { toast } from "sonner";
import { useModules, useModuleMutations } from "@/hooks/api/use-settings";
import { useAuth } from "@/contexts/auth-context";

export function ModulesTab() {
  const { data: modules, loading, refetch } = useModules();
  const mutations = useModuleMutations();
  const { refreshModules } = useAuth();

  const handleToggle = useCallback(
    async (moduleId: string, enabled: boolean) => {
      try {
        await mutations.toggle(moduleId, enabled);
        toast.success(
          enabled
            ? `Modul aktiviert`
            : `Modul deaktiviert`
        );
        refetch();
        await refreshModules();
      } catch (err) {
        toast.error("Fehler beim Umschalten des Moduls", {
          description:
            err instanceof Error ? err.message : "Unbekannter Fehler",
        });
      }
    },
    [mutations, refetch, refreshModules]
  );

  if (loading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-10 w-64" />
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-16 w-full" />
        ))}
      </div>
    );
  }

  return (
    <Card className="border-border/50">
      <CardHeader className="pb-4">
        <CardTitle className="text-base">Feature-Module</CardTitle>
        <p className="text-sm text-muted-foreground">
          Aktivieren oder deaktivieren Sie Module für Ihren Tenant. Deaktivierte
          Module sind in der Navigation, API und für Agenten nicht verfuegbar.
          Daten bleiben bei Deaktivierung erhalten.
        </p>
      </CardHeader>
      <CardContent>
        <div className="rounded-md border border-border/50">
          <div className="grid grid-cols-[1fr_auto] gap-4 border-b border-border/50 px-4 py-2.5 text-xs font-medium uppercase tracking-wider text-muted-foreground">
            <span>Modul</span>
            <span>Status</span>
          </div>
          {(modules || []).map((mod) => (
            <div
              key={mod.id}
              className="grid grid-cols-[1fr_auto] items-center gap-4 border-b border-border/50 px-4 py-3 last:border-b-0"
            >
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-md bg-primary/10">
                  <Package className="h-4 w-4 text-primary" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium">{mod.label}</span>
                    {mod.core && (
                      <Badge
                        variant="secondary"
                        className="text-[10px] px-1.5 py-0"
                      >
                        Kern
                      </Badge>
                    )}
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {mod.description}
                  </p>
                </div>
              </div>
              <Switch
                checked={mod.enabled}
                onCheckedChange={(checked) => handleToggle(mod.id, checked)}
                disabled={mod.core}
              />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
