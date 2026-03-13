"use client";

import { useState } from "react";
import { useApi, useMutation } from "@/hooks/api/use-api";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { Label } from "@/components/ui/label";
import { Plus, Trash2, Loader2, Key } from "lucide-react";
import type { ApiCredential } from "@/types/system-agent";

export default function SystemCredentialsPage() {
  const { data: credentials, loading, refetch } = useApi<ApiCredential[]>("/api/system/credentials");
  const { mutate } = useMutation<unknown, unknown>();
  const [addOpen, setAddOpen] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [form, setForm] = useState({ serviceName: "", value: "", description: "" });
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    if (!form.serviceName || !form.value) return;
    setSaving(true);
    try {
      await mutate("put", `/api/system/credentials/${form.serviceName}`, {
        value: form.value,
        description: form.description || null,
      });
      setAddOpen(false);
      setForm({ serviceName: "", value: "", description: "" });
      refetch();
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteId) return;
    await mutate("delete", `/api/system/credentials/${deleteId}`);
    setDeleteId(null);
    refetch();
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="API-Credentials"
        description="Verschlüsselte API-Schlüssel für externe Dienste (Social Media, Search, GitHub, etc.)"
      />

      <div className="flex justify-end">
        <Button size="sm" onClick={() => setAddOpen(true)}>
          <Plus className="h-4 w-4 mr-1.5" />
          Credential hinzufügen
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Service</TableHead>
                <TableHead>Beschreibung</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Aktualisiert</TableHead>
                <TableHead className="w-12" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {credentials?.map((c) => (
                <TableRow key={c.id}>
                  <TableCell className="font-mono font-medium">{c.serviceName}</TableCell>
                  <TableCell className="text-muted-foreground">{c.description || "—"}</TableCell>
                  <TableCell>
                    <Badge variant={c.hasValue ? "default" : "secondary"}>
                      {c.hasValue ? (
                        <><Key className="h-3 w-3 mr-1" />Konfiguriert</>
                      ) : (
                        "Leer"
                      )}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-xs">
                    {new Date(c.updatedAt).toLocaleDateString("de-DE")}
                  </TableCell>
                  <TableCell>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => setDeleteId(c.serviceName)}
                    >
                      <Trash2 className="h-3.5 w-3.5 text-muted-foreground hover:text-destructive" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {(!credentials || credentials.length === 0) && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground py-8">
                    Keine Credentials konfiguriert
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Add/Edit Dialog */}
      <Dialog open={addOpen} onOpenChange={setAddOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>API-Credential hinzufügen</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label>Service-Name</Label>
              <Input
                placeholder="z.B. twitter, linkedin, search_api, github_token"
                value={form.serviceName}
                onChange={(e) => setForm({ ...form, serviceName: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>API-Key / Wert</Label>
              <Input
                type="password"
                placeholder="Verschlüsselt gespeichert"
                value={form.value}
                onChange={(e) => setForm({ ...form, value: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Beschreibung (optional)</Label>
              <Input
                placeholder="z.B. Twitter API v2 Bearer Token"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAddOpen(false)}>
              Abbrechen
            </Button>
            <Button onClick={handleSave} disabled={!form.serviceName || !form.value || saving}>
              {saving && <Loader2 className="h-4 w-4 animate-spin mr-1.5" />}
              Speichern
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmationDialog
        open={!!deleteId}
        onCancel={() => setDeleteId(null)}
        title="Credential löschen"
        description={`Möchten Sie das Credential "${deleteId}" wirklich löschen?`}
        onConfirm={handleDelete}
        confirmLabel="Löschen"
        variant="destructive"
      />
    </div>
  );
}
