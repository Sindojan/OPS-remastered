"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { DomainStatusBadge } from "@/components/shared/domain-status-badge";
import { Save, Loader2, Plug, RefreshCw } from "lucide-react";
import { useOdooConfig, useOdooMutations } from "@/hooks/api/use-odoo";
import { toast } from "sonner";
import type { OdooConnectionTestResult } from "@/types/api";

export function OdooTab() {
  const { data: config, loading: configLoading, refetch } = useOdooConfig();
  const { saveConfig, testConnection, loading: saving } = useOdooMutations();

  const [baseUrl, setBaseUrl] = useState("");
  const [databaseName, setDatabaseName] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [odooVersion, setOdooVersion] = useState("19.0");
  const [testing, setTesting] = useState(false);

  useEffect(() => {
    if (config) {
      setBaseUrl(config.baseUrl || "");
      setDatabaseName(config.databaseName || "");
      setOdooVersion(config.odooVersion || "19.0");
    }
  }, [config]);

  const handleSave = async () => {
    if (!baseUrl.trim() || !databaseName.trim() || !apiKey.trim()) {
      toast.error("Bitte alle Pflichtfelder ausfüllen");
      return;
    }
    try {
      await saveConfig({ baseUrl, databaseName, apiKey, odooVersion });
      toast.success("Odoo-Konfiguration gespeichert");
      setApiKey("");
      refetch();
    } catch (err) {
      toast.error("Fehler beim Speichern", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  };

  const handleTest = async () => {
    setTesting(true);
    try {
      const result: OdooConnectionTestResult = await testConnection();
      if (result.success) {
        toast.success("Verbindung erfolgreich", {
          description: result.message,
        });
      } else {
        toast.error("Verbindung fehlgeschlagen", {
          description: result.message,
        });
      }
      refetch();
    } catch (err) {
      toast.error("Verbindungstest fehlgeschlagen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setTesting(false);
    }
  };

  const statusVariant = config?.connectionStatus === "CONNECTED"
    ? "success"
    : config?.connectionStatus === "ERROR"
      ? "error"
      : config?.connectionStatus === "PENDING"
        ? "warning"
        : "neutral";

  const statusLabel = config?.connectionStatus === "CONNECTED"
    ? "Verbunden"
    : config?.connectionStatus === "ERROR"
      ? "Fehler"
      : config?.connectionStatus === "PENDING"
        ? "Noch nicht getestet"
        : "Nicht konfiguriert";

  if (configLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Status */}
      <Card className="border-border/50">
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <Plug className="h-4 w-4" />
              Verbindungsstatus
            </CardTitle>
            <DomainStatusBadge variant={statusVariant}>
              {statusLabel}
            </DomainStatusBadge>
          </div>
          {config?.lastConnectedAt && (
            <p className="text-xs text-muted-foreground">
              Letzte Verbindung: {new Date(config.lastConnectedAt).toLocaleString("de-DE")}
            </p>
          )}
        </CardHeader>
      </Card>

      {/* Config Form */}
      <Card className="border-border/50">
        <CardHeader className="pb-4">
          <CardTitle className="text-base">Verbindungsdaten</CardTitle>
          <p className="text-sm text-muted-foreground">
            Verbindung zu einer Odoo 19+ Instanz konfigurieren. Der API-Key wird AES-256-GCM verschlüsselt gespeichert.
          </p>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label className="text-xs">Odoo URL *</Label>
              <Input
                value={baseUrl}
                onChange={(e) => setBaseUrl(e.target.value)}
                placeholder="https://meine-firma.odoo.com"
                className="text-sm"
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Datenbank *</Label>
              <Input
                value={databaseName}
                onChange={(e) => setDatabaseName(e.target.value)}
                placeholder="meine-firma"
                className="text-sm"
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label className="text-xs">API Key *</Label>
              <Input
                type="password"
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                placeholder={config?.hasApiKey ? "••••••••••••••••" : "Odoo API Key eingeben"}
                className="text-sm"
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Odoo Version</Label>
              <Select value={odooVersion} onValueChange={setOdooVersion}>
                <SelectTrigger className="text-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="19.0">19.0</SelectItem>
                  <SelectItem value="18.0">18.0</SelectItem>
                  <SelectItem value="17.0">17.0</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="flex items-center gap-3 pt-2">
            <Button onClick={handleSave} disabled={saving} className="gap-2">
              {saving ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Save className="h-4 w-4" />
              )}
              {saving ? "Speichern..." : "Speichern"}
            </Button>
            <Button
              variant="outline"
              onClick={handleTest}
              disabled={testing || !config?.hasApiKey}
              className="gap-2"
            >
              {testing ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <RefreshCw className="h-4 w-4" />
              )}
              {testing ? "Teste..." : "Verbindung testen"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
