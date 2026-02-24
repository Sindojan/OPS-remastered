"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ArrowLeft,
  Download,
  FileText,
  File,
  FileSpreadsheet,
  Image,
  AlertTriangle,
  Save,
  Trash2,
  Eye,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import {
  DomainStatusBadge,
} from "@/components/shared/domain-status-badge";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { SkeletonPanel } from "@/components/shared/skeleton-variants";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";

import { toast } from "sonner";
import { useApi } from "@/hooks/api/use-api";
import { useDocumentMutations } from "@/hooks/api/use-documents";
import type { DocumentResponse, ApiResponse } from "@/types/api";
import { formatDate } from "@/lib/format";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// ─── Helpers ──────────────────────────────────────────────

function getFileIcon(mimeType: string | null) {
  if (!mimeType) return FileText;
  if (mimeType.startsWith("image/")) return Image;
  if (mimeType.includes("pdf")) return FileText;
  if (mimeType.includes("spreadsheet") || mimeType.includes("excel")) return FileSpreadsheet;
  return File;
}

function formatFileSize(bytes: number | null) {
  if (!bytes) return "\u2013";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function getDocStatusVariant(status: string) {
  switch (status) {
    case "ACTIVE": return "success" as const;
    case "ARCHIVED": return "neutral" as const;
    case "DELETED": return "error" as const;
    default: return "neutral" as const;
  }
}

function getDocStatusLabel(status: string) {
  switch (status) {
    case "ACTIVE": return "Aktiv";
    case "ARCHIVED": return "Archiviert";
    case "DELETED": return "Gel\u00f6scht";
    default: return status;
  }
}

// ─── Component ──────────────────────────────────────────

export default function DocumentDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;

  const { data: document, loading, error, refetch } = useApi<DocumentResponse>(
    `/api/documents/${id}`,
  );
  const { data: previewData } = useApi<{ url: string }>(
    `/api/documents/${id}/preview`,
  );
  const documentMutations = useDocumentMutations();

  const [editTitle, setEditTitle] = useState("");
  const [editDesc, setEditDesc] = useState("");
  const [hasChanges, setHasChanges] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  // Sync edit fields with loaded document
  useEffect(() => {
    if (document) {
      setEditTitle(document.title);
      setEditDesc(document.description ?? "");
    }
  }, [document]);

  // Track changes
  useEffect(() => {
    if (!document) return;
    const changed =
      editTitle !== document.title ||
      editDesc !== (document.description ?? "");
    setHasChanges(changed);
  }, [editTitle, editDesc, document]);

  const handleSave = useCallback(async () => {
    if (!document || !hasChanges) return;
    setSaving(true);
    try {
      await documentMutations.updateDocument(document.id, {
        title: editTitle,
        description: editDesc || undefined,
      });
      toast.success("\u00c4nderungen gespeichert");
      refetch();
    } catch (err) {
      toast.error("Speichern fehlgeschlagen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setSaving(false);
    }
  }, [document, editTitle, editDesc, hasChanges, documentMutations, refetch]);

  const handleDownload = useCallback(() => {
    if (!document) return;
    const token = localStorage.getItem("owlsburg_token");
    window.open(
      `${API_BASE}/api/documents/${document.id}/download?token=${token}`,
      "_blank",
    );
  }, [document]);

  const handleDelete = useCallback(async () => {
    if (!document) return;
    try {
      await documentMutations.deleteDocument(document.id);
      toast.success("Dokument gel\u00f6scht");
      router.push("/knowledge/documents");
    } catch (err) {
      toast.error("L\u00f6schen fehlgeschlagen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [document, documentMutations, router]);

  // ─── Loading ───────────────────────────────────────────

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-3">
          <Skeleton className="h-8 w-8 rounded" />
          <Skeleton className="h-6 w-48" />
        </div>
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
          <div className="lg:col-span-3">
            <Skeleton className="h-96 w-full rounded-lg" />
          </div>
          <div className="lg:col-span-2">
            <SkeletonPanel />
          </div>
        </div>
      </div>
    );
  }

  // ─── Error ─────────────────────────────────────────────

  if (error || !document) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <AlertTriangle className="h-8 w-8 text-destructive" />
        <p className="text-sm text-muted-foreground">{error ?? "Dokument nicht gefunden"}</p>
        <Button variant="outline" size="sm" onClick={() => router.push("/knowledge/documents")}>
          Zur\u00fcck zu Dokumenten
        </Button>
      </div>
    );
  }

  const IconComp = getFileIcon(document.mimeType);
  const isImage = document.mimeType?.startsWith("image/") ?? false;
  const isPdf = document.mimeType?.includes("pdf") ?? false;
  const previewUrl = previewData?.url ?? null;

  return (
    <div className="space-y-6">
      <PageHeader
        title={document.title}
        breadcrumb={["Wissensdatenbank", "Dokumente", document.title]}
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => router.push("/knowledge/documents")}
            >
              <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
              Zur\u00fcck
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="gap-1.5"
              onClick={handleDownload}
            >
              <Download className="h-3.5 w-3.5" />
              Herunterladen
            </Button>
            <Button
              variant="destructive"
              size="sm"
              className="gap-1.5"
              onClick={() => setDeleteOpen(true)}
            >
              <Trash2 className="h-3.5 w-3.5" />
              L\u00f6schen
            </Button>
          </div>
        }
      />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
        {/* Preview Area */}
        <div className="lg:col-span-3">
          <Card className="flex min-h-96 items-center justify-center overflow-hidden p-0">
            {isImage && previewUrl ? (
              <img
                src={previewUrl}
                alt={document.title}
                className="max-h-[600px] w-full object-contain"
              />
            ) : isPdf && previewUrl ? (
              <iframe
                src={previewUrl}
                title={document.title}
                className="h-[600px] w-full border-0"
              />
            ) : (
              <div className="flex flex-col items-center justify-center gap-4 p-12">
                <IconComp className="h-16 w-16 text-muted-foreground/30" />
                <p className="text-sm text-muted-foreground">
                  Keine Vorschau verf\u00fcgbar
                </p>
                <Button
                  variant="outline"
                  size="sm"
                  className="gap-1.5"
                  onClick={handleDownload}
                >
                  <Download className="h-3.5 w-3.5" />
                  Datei herunterladen
                </Button>
              </div>
            )}
          </Card>
        </div>

        {/* Metadata Panel */}
        <div className="lg:col-span-2 space-y-4">
          <Card className="p-4 space-y-4">
            <h3 className="text-sm font-semibold">Dokumentdetails</h3>

            <div className="space-y-3">
              <div className="space-y-1.5">
                <Label htmlFor="doc-title" className="text-xs text-muted-foreground">
                  Titel
                </Label>
                <Input
                  id="doc-title"
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="doc-desc" className="text-xs text-muted-foreground">
                  Beschreibung
                </Label>
                <Input
                  id="doc-desc"
                  value={editDesc}
                  onChange={(e) => setEditDesc(e.target.value)}
                  placeholder="Optionale Beschreibung"
                />
              </div>

              {hasChanges && (
                <Button
                  size="sm"
                  className="w-full gap-1.5"
                  disabled={saving}
                  onClick={handleSave}
                >
                  <Save className="h-3.5 w-3.5" />
                  {saving ? "Wird gespeichert..." : "\u00c4nderungen speichern"}
                </Button>
              )}
            </div>

            <div className="h-px bg-border" />

            <div className="space-y-2.5">
              <DetailRow label="Dateiname" value={document.fileName} mono />
              <DetailRow label="Typ" value={document.mimeType ?? "\u2013"} />
              <DetailRow label="Gr\u00f6\u00dfe" value={formatFileSize(document.fileSizeBytes)} mono />
              <DetailRow label="Kategorie" value={document.category ?? "\u2013"} />
              <DetailRow label="Version" value={String(document.version)} mono />
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">Status</span>
                <DomainStatusBadge variant={getDocStatusVariant(document.status)}>
                  {getDocStatusLabel(document.status)}
                </DomainStatusBadge>
              </div>
              <DetailRow
                label="Hochgeladen am"
                value={formatDate(document.createdAt)}
                mono
              />
              <DetailRow
                label="Zuletzt ge\u00e4ndert"
                value={formatDate(document.updatedAt)}
                mono
              />
            </div>
          </Card>
        </div>
      </div>

      {/* Delete Confirmation */}
      <ConfirmationDialog
        open={deleteOpen}
        title="Dokument l\u00f6schen"
        description={`M\u00f6chten Sie "${document.fileName}" wirklich l\u00f6schen? Diese Aktion kann nicht r\u00fcckg\u00e4ngig gemacht werden.`}
        variant="destructive"
        confirmLabel="L\u00f6schen"
        cancelLabel="Abbrechen"
        onConfirm={handleDelete}
        onCancel={() => setDeleteOpen(false)}
      />
    </div>
  );
}

// ─── Sub-Component ───────────────────────────────────────

function DetailRow({
  label,
  value,
  mono,
}: {
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="shrink-0 text-xs text-muted-foreground">{label}</span>
      <span
        className={`truncate text-right text-xs ${mono ? "font-mono" : ""} text-foreground`}
      >
        {value}
      </span>
    </div>
  );
}
