"use client";

import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/contexts/auth-context";
import { useNotificationSettings, useNotificationSettingsMutations } from "@/hooks/api/use-settings";
import { Switch } from "@/components/ui/switch";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "sonner";
import { Bell, Save, Loader2, Bot, Factory, Inbox, Mail } from "lucide-react";
import type { NotificationSettingsResponse } from "@/types/api";

interface NotificationRowProps {
  label: string;
  description?: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
}

function NotificationRow({ label, description, checked, onCheckedChange, disabled }: NotificationRowProps) {
  return (
    <div className="flex items-center justify-between py-3">
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium">{label}</p>
        {description && <p className="text-xs text-muted-foreground">{description}</p>}
      </div>
      <Switch
        checked={checked}
        onCheckedChange={onCheckedChange}
        disabled={disabled}
      />
    </div>
  );
}

export function NotificationsTab() {
  const { user } = useAuth();
  const { data: settings, loading, refetch } = useNotificationSettings();
  const mutations = useNotificationSettingsMutations();

  const [form, setForm] = useState<NotificationSettingsResponse>({
    agentRunCompleted: true,
    agentRunFailed: true,
    stockBelowMinimum: true,
    machineIncident: true,
    jobOverdue: true,
    absenceRequest: true,
    inboxNewMessage: true,
    inApp: true,
    emailNotifications: false,
  });
  const [isDirty, setIsDirty] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (settings) {
      setForm(settings);
      setIsDirty(false);
    }
  }, [settings]);

  const updateField = useCallback((key: keyof NotificationSettingsResponse, value: boolean) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    setIsDirty(true);
  }, []);

  const handleSave = useCallback(async () => {
    setSaving(true);
    try {
      await mutations.save(form);
      toast.success("Benachrichtigungen gespeichert");
      setIsDirty(false);
      refetch();
    } catch (err) {
      toast.error("Fehler beim Speichern", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setSaving(false);
    }
  }, [form, mutations, refetch]);

  const isManager = user?.role === "ADMIN" || user?.role === "MANAGER" || user?.role === "SYSTEM_ADMIN";

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-48 w-full" />
        <Skeleton className="h-32 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Agents */}
      <Card className="border-border/50">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm flex items-center gap-1.5">
            <Bot className="h-3.5 w-3.5" />
            Agenten
          </CardTitle>
        </CardHeader>
        <CardContent className="divide-y divide-border/50">
          <NotificationRow
            label="Agent-Run abgeschlossen"
            description="Benachrichtigung, wenn ein Agent-Run erfolgreich beendet wurde."
            checked={form.agentRunCompleted}
            onCheckedChange={(v) => updateField("agentRunCompleted", v)}
          />
          <NotificationRow
            label="Agent-Run fehlgeschlagen"
            description="Benachrichtigung bei einem fehlgeschlagenen Agent-Run."
            checked={form.agentRunFailed}
            onCheckedChange={(v) => updateField("agentRunFailed", v)}
          />
        </CardContent>
      </Card>

      {/* Operations - hide some rows for WORKER/TEAM_LEAD */}
      <Card className="border-border/50">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm flex items-center gap-1.5">
            <Factory className="h-3.5 w-3.5" />
            Betrieb
          </CardTitle>
        </CardHeader>
        <CardContent className="divide-y divide-border/50">
          {isManager && (
            <>
              <NotificationRow
                label="Bestand unter Minimum"
                description="Warnung, wenn ein Artikel unter den Mindestbestand fällt."
                checked={form.stockBelowMinimum}
                onCheckedChange={(v) => updateField("stockBelowMinimum", v)}
              />
              <NotificationRow
                label="Maschinenstörung"
                description="Benachrichtigung bei einem gemeldeten Maschinenstillstand."
                checked={form.machineIncident}
                onCheckedChange={(v) => updateField("machineIncident", v)}
              />
              <NotificationRow
                label="Job überfällig"
                description="Warnung, wenn ein Job seine Deadline überschritten hat."
                checked={form.jobOverdue}
                onCheckedChange={(v) => updateField("jobOverdue", v)}
              />
              <NotificationRow
                label="Abwesenheitsantrag"
                description="Benachrichtigung bei neuen Abwesenheitsanträgen."
                checked={form.absenceRequest}
                onCheckedChange={(v) => updateField("absenceRequest", v)}
              />
            </>
          )}
        </CardContent>
      </Card>

      {/* Inbox */}
      <Card className="border-border/50">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm flex items-center gap-1.5">
            <Inbox className="h-3.5 w-3.5" />
            Posteingang
          </CardTitle>
        </CardHeader>
        <CardContent className="divide-y divide-border/50">
          <NotificationRow
            label="Neue Nachricht"
            description="Benachrichtigung bei neuen Nachrichten im Posteingang."
            checked={form.inboxNewMessage}
            onCheckedChange={(v) => updateField("inboxNewMessage", v)}
          />
        </CardContent>
      </Card>

      {/* Channels */}
      <Card className="border-border/50">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm flex items-center gap-1.5">
            <Mail className="h-3.5 w-3.5" />
            Kanäle
          </CardTitle>
        </CardHeader>
        <CardContent className="divide-y divide-border/50">
          <NotificationRow
            label="In-App-Benachrichtigungen"
            description="Benachrichtigungen innerhalb der Anwendung."
            checked={form.inApp}
            onCheckedChange={() => {}}
            disabled
          />
          <NotificationRow
            label="E-Mail-Benachrichtigungen"
            description="Benachrichtigungen per E-Mail."
            checked={form.emailNotifications}
            onCheckedChange={(v) => updateField("emailNotifications", v)}
          />
        </CardContent>
      </Card>

      {/* Save Button */}
      {isDirty && (
        <div className="flex items-center gap-3">
          <Button onClick={handleSave} disabled={saving} className="gap-2">
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            {saving ? "Speichern..." : "Speichern"}
          </Button>
        </div>
      )}
    </div>
  );
}
