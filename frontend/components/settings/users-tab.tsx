"use client";

import { useState, useCallback, useMemo } from "react";
import { DataTable, type ColumnDef, type RowAction } from "@/components/shared/data-table";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { useAuth } from "@/contexts/auth-context";
import { usePagedApi, useApi, useMutation } from "@/hooks/api/use-api";
import { apiClient } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { toast } from "sonner";
import {
  Users,
  Plus,
  Pencil,
  Trash2,
  KeyRound,
  UserCheck,
  UserX,
  Loader2,
  Copy,
  RefreshCw,
} from "lucide-react";
import type { UserResponse, AgentInstance, ApiResponse } from "@/types/api";

const ROLE_OPTIONS = [
  { value: "ADMIN", label: "Administrator" },
  { value: "MANAGER", label: "Manager" },
  { value: "TEAM_LEAD", label: "Teamleiter" },
  { value: "WORKER", label: "Mitarbeiter" },
];

const ROLE_BADGE_VARIANT: Record<string, string> = {
  ADMIN: "bg-error/10 text-error border-error/20",
  MANAGER: "bg-warning/10 text-warning border-warning/20",
  TEAM_LEAD: "bg-info/10 text-info border-info/20",
  WORKER: "bg-muted text-muted-foreground border-border",
};

function generatePassword(length = 12): string {
  const chars = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%";
  let password = "";
  for (let i = 0; i < length; i++) {
    password += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return password;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function UsersTab() {
  const { user: currentUser } = useAuth();
  const { data: users, loading, refetch } = usePagedApi<UserResponse>("/api/users?size=500");
  const { data: instances } = useApi<AgentInstance[]>("/api/agent-instances");
  const { mutate } = useMutation<unknown, unknown>();

  // Dialog states
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [showCredentialsDialog, setShowCredentialsDialog] = useState(false);
  const [showResetDialog, setShowResetDialog] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [showToggleDialog, setShowToggleDialog] = useState(false);

  const [selectedUser, setSelectedUser] = useState<UserResponse | null>(null);
  const [saving, setSaving] = useState(false);

  // Create form
  const [createForm, setCreateForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    role: "WORKER",
    password: generatePassword(),
  });

  // Edit form
  const [editForm, setEditForm] = useState({
    firstName: "",
    lastName: "",
    role: "WORKER",
    primaryAgentInstanceId: undefined as string | undefined,
  });

  // Credentials display
  const [credentials, setCredentials] = useState({ email: "", password: "" });

  // Filter
  const [roleFilter, setRoleFilter] = useState<string | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);

  const filteredUsers = useMemo(() => {
    if (!users) return [];
    return users.filter((u) => {
      if (roleFilter && u.role !== roleFilter) return false;
      if (statusFilter === "active" && !u.active) return false;
      if (statusFilter === "inactive" && u.active) return false;
      return true;
    });
  }, [users, roleFilter, statusFilter]);

  const handleCreate = useCallback(async () => {
    setSaving(true);
    try {
      await apiClient.post<ApiResponse<UserResponse>>("/api/users", {
        firstName: createForm.firstName,
        lastName: createForm.lastName,
        email: createForm.email,
        role: createForm.role,
        password: createForm.password,
      });
      setCredentials({ email: createForm.email, password: createForm.password });
      setShowCreateDialog(false);
      setShowCredentialsDialog(true);
      setCreateForm({ firstName: "", lastName: "", email: "", role: "WORKER", password: generatePassword() });
      refetch();
    } catch (err) {
      toast.error("Fehler beim Erstellen des Benutzers", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setSaving(false);
    }
  }, [createForm, refetch]);

  const handleEdit = useCallback(async () => {
    if (!selectedUser) return;
    setSaving(true);
    try {
      await apiClient.patch<ApiResponse<UserResponse>>(`/api/users/${selectedUser.id}`, {
        firstName: editForm.firstName,
        lastName: editForm.lastName,
        role: editForm.role,
        primaryAgentInstanceId: editForm.primaryAgentInstanceId || undefined,
      });
      toast.success("Benutzer aktualisiert");
      setShowEditDialog(false);
      refetch();
    } catch (err) {
      toast.error("Fehler beim Aktualisieren", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setSaving(false);
    }
  }, [selectedUser, editForm, refetch]);

  const handleResetPassword = useCallback(async () => {
    if (!selectedUser) return;
    setSaving(true);
    try {
      const newPassword = generatePassword();
      await apiClient.post<ApiResponse<unknown>>(`/api/users/${selectedUser.id}/reset-password`, {
        newPassword,
      });
      setCredentials({ email: selectedUser.email, password: newPassword });
      setShowResetDialog(false);
      setShowCredentialsDialog(true);
    } catch (err) {
      toast.error("Fehler beim Zurücksetzen des Passworts", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setSaving(false);
    }
  }, [selectedUser]);

  const handleToggleStatus = useCallback(async () => {
    if (!selectedUser) return;
    setSaving(true);
    try {
      await apiClient.patch<ApiResponse<UserResponse>>(`/api/users/${selectedUser.id}/status`, {
        active: !selectedUser.active,
      });
      toast.success(selectedUser.active ? "Benutzer deaktiviert" : "Benutzer aktiviert");
      setShowToggleDialog(false);
      refetch();
    } catch (err) {
      toast.error("Fehler beim Ändern des Status", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setSaving(false);
    }
  }, [selectedUser, refetch]);

  const handleDelete = useCallback(async () => {
    if (!selectedUser) return;
    setSaving(true);
    try {
      await mutate("delete", `/api/users/${selectedUser.id}`);
      toast.success("Benutzer gelöscht");
      setShowDeleteDialog(false);
      refetch();
    } catch (err) {
      toast.error("Fehler beim Löschen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setSaving(false);
    }
  }, [selectedUser, mutate, refetch]);

  const openEditDialog = useCallback((u: UserResponse) => {
    setSelectedUser(u);
    setEditForm({
      firstName: u.firstName,
      lastName: u.lastName,
      role: u.role,
      primaryAgentInstanceId: undefined,
    });
    setShowEditDialog(true);
  }, []);

  const columns: ColumnDef<UserResponse>[] = [
    {
      id: "name",
      header: "Name",
      accessorKey: "firstName",
      cell: (row) => (
        <span className="font-medium text-sm">{row.firstName} {row.lastName}</span>
      ),
    },
    {
      id: "email",
      header: "E-Mail",
      accessorKey: "email",
      cell: (row) => (
        <span className="font-mono text-xs text-muted-foreground">{row.email}</span>
      ),
    },
    {
      id: "role",
      header: "Rolle",
      accessorKey: "role",
      cell: (row) => (
        <Badge variant="outline" className={ROLE_BADGE_VARIANT[row.role] || ""}>
          {ROLE_OPTIONS.find((r) => r.value === row.role)?.label || row.role}
        </Badge>
      ),
    },
    {
      id: "status",
      header: "Status",
      accessorKey: "active",
      cell: (row) => (
        <Badge variant="outline" className={row.active
          ? "bg-success/10 text-success border-success/20"
          : "bg-muted text-muted-foreground border-border"
        }>
          {row.active ? "Aktiv" : "Deaktiviert"}
        </Badge>
      ),
    },
    {
      id: "createdAt",
      header: "Erstellt am",
      accessorKey: "createdAt",
      cell: (row) => (
        <span className="font-mono text-xs text-muted-foreground">
          {formatDate(row.createdAt)}
        </span>
      ),
    },
  ];

  const rowActions: RowAction<UserResponse>[] = [
    {
      label: "Bearbeiten",
      icon: <Pencil className="h-3.5 w-3.5" />,
      onClick: (row) => openEditDialog(row),
    },
    {
      label: "Passwort zurücksetzen",
      icon: <KeyRound className="h-3.5 w-3.5" />,
      onClick: (row) => { setSelectedUser(row); setShowResetDialog(true); },
    },
    {
      label: "Deaktivieren/Aktivieren",
      icon: <UserX className="h-3.5 w-3.5" />,
      onClick: (row) => { setSelectedUser(row); setShowToggleDialog(true); },
    },
    {
      label: "Löschen",
      icon: <Trash2 className="h-3.5 w-3.5" />,
      onClick: (row) => { setSelectedUser(row); setShowDeleteDialog(true); },
      variant: "destructive",
    },
  ];

  if (currentUser?.role !== "ADMIN" && currentUser?.role !== "SYSTEM_ADMIN") {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <Users className="h-8 w-8 text-muted-foreground/40" />
        <p className="mt-2 text-sm text-muted-foreground">Nur Administratoren können Benutzer verwalten.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <DataTable<UserResponse>
        data={filteredUsers}
        columns={columns}
        loading={loading}
        searchKey="email"
        searchPlaceholder="Benutzer suchen..."
        rowActions={rowActions}
        filterSlots={
          <>
            <Select value={roleFilter} onValueChange={(v) => setRoleFilter(v === "all" ? undefined : v)}>
              <SelectTrigger className="h-9 w-40">
                <SelectValue placeholder="Alle Rollen" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Alle Rollen</SelectItem>
                {ROLE_OPTIONS.map((r) => (
                  <SelectItem key={r.value} value={r.value}>{r.label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select value={statusFilter} onValueChange={(v) => setStatusFilter(v === "all" ? undefined : v)}>
              <SelectTrigger className="h-9 w-36">
                <SelectValue placeholder="Alle Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Alle Status</SelectItem>
                <SelectItem value="active">Aktiv</SelectItem>
                <SelectItem value="inactive">Deaktiviert</SelectItem>
              </SelectContent>
            </Select>
            <div className="flex-1" />
            <Button size="sm" className="gap-1.5" onClick={() => setShowCreateDialog(true)}>
              <Plus className="h-3.5 w-3.5" />
              Neuer Benutzer
            </Button>
          </>
        }
        emptyState={{
          icon: <Users className="h-8 w-8 text-muted-foreground/40" />,
          title: "Keine Benutzer gefunden",
          description: "Erstellen Sie einen neuen Benutzer über den Button oben.",
        }}
      />

      {/* Create Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Neuer Benutzer</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label className="text-xs">Vorname</Label>
                <Input
                  value={createForm.firstName}
                  onChange={(e) => setCreateForm((f) => ({ ...f, firstName: e.target.value }))}
                  placeholder="Max"
                />
              </div>
              <div className="space-y-1.5">
                <Label className="text-xs">Nachname</Label>
                <Input
                  value={createForm.lastName}
                  onChange={(e) => setCreateForm((f) => ({ ...f, lastName: e.target.value }))}
                  placeholder="Mustermann"
                />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">E-Mail</Label>
              <Input
                type="email"
                value={createForm.email}
                onChange={(e) => setCreateForm((f) => ({ ...f, email: e.target.value }))}
                placeholder="max@example.com"
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Rolle</Label>
              <Select value={createForm.role} onValueChange={(v) => setCreateForm((f) => ({ ...f, role: v }))}>
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ROLE_OPTIONS.map((r) => (
                    <SelectItem key={r.value} value={r.value}>{r.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Passwort</Label>
              <div className="flex gap-2">
                <Input
                  value={createForm.password}
                  onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))}
                  className="font-mono text-sm"
                />
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setCreateForm((f) => ({ ...f, password: generatePassword() }))}
                  className="gap-1 shrink-0"
                >
                  <RefreshCw className="h-3.5 w-3.5" />
                  Generieren
                </Button>
              </div>
            </div>
          </div>
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setShowCreateDialog(false)}>Abbrechen</Button>
            <Button
              onClick={handleCreate}
              disabled={saving || !createForm.firstName || !createForm.lastName || !createForm.email || !createForm.password}
              className="gap-1.5"
            >
              {saving && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
              Erstellen
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Credentials Dialog */}
      <Dialog open={showCredentialsDialog} onOpenChange={setShowCredentialsDialog}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <UserCheck className="h-4 w-4 text-success" />
              Zugangsdaten
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <p className="text-xs text-muted-foreground">
              Bitte kopieren Sie die Zugangsdaten jetzt. Das Passwort wird nur einmal angezeigt.
            </p>
            <div className="space-y-2 rounded-md border bg-muted/30 p-3">
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">E-Mail</span>
                <button onClick={() => { navigator.clipboard.writeText(credentials.email); toast.success("Kopiert"); }}>
                  <Copy className="h-3 w-3 text-muted-foreground hover:text-foreground" />
                </button>
              </div>
              <p className="font-mono text-sm">{credentials.email}</p>
              <div className="flex items-center justify-between mt-2">
                <span className="text-xs text-muted-foreground">Passwort</span>
                <button onClick={() => { navigator.clipboard.writeText(credentials.password); toast.success("Kopiert"); }}>
                  <Copy className="h-3 w-3 text-muted-foreground hover:text-foreground" />
                </button>
              </div>
              <p className="font-mono text-sm">{credentials.password}</p>
            </div>
          </div>
          <DialogFooter>
            <Button onClick={() => setShowCredentialsDialog(false)}>Schließen</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={showEditDialog} onOpenChange={setShowEditDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Benutzer bearbeiten</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label className="text-xs">Vorname</Label>
                <Input
                  value={editForm.firstName}
                  onChange={(e) => setEditForm((f) => ({ ...f, firstName: e.target.value }))}
                />
              </div>
              <div className="space-y-1.5">
                <Label className="text-xs">Nachname</Label>
                <Input
                  value={editForm.lastName}
                  onChange={(e) => setEditForm((f) => ({ ...f, lastName: e.target.value }))}
                />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Rolle</Label>
              <Select value={editForm.role} onValueChange={(v) => setEditForm((f) => ({ ...f, role: v }))}>
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ROLE_OPTIONS.map((r) => (
                    <SelectItem key={r.value} value={r.value}>{r.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Primary Agent</Label>
              <Select
                value={editForm.primaryAgentInstanceId}
                onValueChange={(v) => setEditForm((f) => ({ ...f, primaryAgentInstanceId: v === "none" ? undefined : v }))}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Standard (Rollen-Default)" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">Standard (Rollen-Default)</SelectItem>
                  {(instances || []).filter((i) => i.status === "ACTIVE").map((inst) => (
                    <SelectItem key={inst.id} value={inst.id}>{inst.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setShowEditDialog(false)}>Abbrechen</Button>
            <Button onClick={handleEdit} disabled={saving} className="gap-1.5">
              {saving && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
              Speichern
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Reset Password Confirmation */}
      <ConfirmationDialog
        open={showResetDialog}
        title="Passwort zurücksetzen?"
        description={`Das Passwort für ${selectedUser?.email || ""} wird zurückgesetzt. Das neue Passwort wird einmalig angezeigt.`}
        onConfirm={handleResetPassword}
        onCancel={() => setShowResetDialog(false)}
        confirmLabel="Zurücksetzen"
      />

      {/* Toggle Status Confirmation */}
      <ConfirmationDialog
        open={showToggleDialog}
        title={selectedUser?.active ? "Benutzer deaktivieren?" : "Benutzer aktivieren?"}
        description={selectedUser?.active
          ? `${selectedUser?.email || ""} kann sich nach der Deaktivierung nicht mehr anmelden.`
          : `${selectedUser?.email || ""} kann sich nach der Aktivierung wieder anmelden.`
        }
        onConfirm={handleToggleStatus}
        onCancel={() => setShowToggleDialog(false)}
        variant={selectedUser?.active ? "destructive" : "default"}
        confirmLabel={selectedUser?.active ? "Deaktivieren" : "Aktivieren"}
      />

      {/* Delete Confirmation */}
      <ConfirmationDialog
        open={showDeleteDialog}
        title="Benutzer löschen?"
        description={`${selectedUser?.email || ""} wird unwiderruflich gelöscht. Diese Aktion kann nicht rückgängig gemacht werden.`}
        onConfirm={handleDelete}
        onCancel={() => setShowDeleteDialog(false)}
        variant="destructive"
        confirmLabel="Endgültig löschen"
      />
    </div>
  );
}
