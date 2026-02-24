"use client";

import { useState, useMemo, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import {
  Plus,
  Upload,
  FileText,
  File,
  FileSpreadsheet,
  Image,
  AlertTriangle,
  Download,
  Trash2,
  Eye,
  HardDrive,
  FolderUp,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/shared/kpi-card";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import {
  DomainStatusBadge,
} from "@/components/shared/domain-status-badge";
import { SkeletonCard } from "@/components/shared/skeleton-variants";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

import { toast } from "sonner";
import { useDocuments, useDocumentMutations } from "@/hooks/api/use-documents";
import type { DocumentResponse } from "@/types/api";
import { formatDate } from "@/lib/format";

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

const DOC_STATUSES = ["ACTIVE", "ARCHIVED"];
const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// ─── Component ──────────────────────────────────────────

export default function DocumentsPage() {
  const router = useRouter();

  // State
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [uploadOpen, setUploadOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<DocumentResponse | null>(null);

  // Data
  const { data: documents, loading, error, refetch } = useDocuments();
  const documentMutations = useDocumentMutations();

  // Upload state
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadTitle, setUploadTitle] = useState("");
  const [uploadDesc, setUploadDesc] = useState("");
  const [uploadCategory, setUploadCategory] = useState("");
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // ─── KPI Computations ──────────────────────────────────

  const kpis = useMemo(() => {
    if (!documents) return null;
    const total = documents.length;
    const totalSize = documents.reduce((sum, d) => sum + (d.fileSizeBytes ?? 0), 0);
    const oneWeekAgo = new Date();
    oneWeekAgo.setDate(oneWeekAgo.getDate() - 7);
    const recentCount = documents.filter(
      (d) => new Date(d.createdAt) >= oneWeekAgo,
    ).length;
    return { total, totalSize, recentCount };
  }, [documents]);

  // ─── Filtered Data ─────────────────────────────────────

  const filteredDocuments = useMemo(() => {
    if (!documents) return [];
    if (statusFilter === "ALL") return documents;
    return documents.filter((d) => d.status === statusFilter);
  }, [documents, statusFilter]);

  // ─── Table Columns ─────────────────────────────────────

  const columns: ColumnDef<DocumentResponse>[] = useMemo(
    () => [
      {
        id: "icon",
        header: "",
        cell: (row) => {
          const IconComp = getFileIcon(row.mimeType);
          return (
            <div className="flex h-7 w-7 items-center justify-center rounded bg-muted">
              <IconComp className="h-3.5 w-3.5 text-muted-foreground" />
            </div>
          );
        },
        headerClassName: "w-10",
        cellClassName: "w-10",
      },
      {
        id: "fileName",
        header: "Name",
        accessorKey: "fileName",
        sortable: true,
        cell: (row) => (
          <div className="min-w-0">
            <p className="truncate text-sm font-medium">{row.title || row.fileName}</p>
            {row.title && row.title !== row.fileName && (
              <p className="truncate text-[11px] text-muted-foreground">{row.fileName}</p>
            )}
          </div>
        ),
      },
      {
        id: "category",
        header: "Kategorie",
        cell: (row) => (
          <span className="text-xs text-muted-foreground">{row.category ?? "\u2013"}</span>
        ),
      },
      {
        id: "size",
        header: "Gr\u00f6\u00dfe",
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {formatFileSize(row.fileSizeBytes)}
          </span>
        ),
        sortFn: (a, b) => (a.fileSizeBytes ?? 0) - (b.fileSizeBytes ?? 0),
        sortable: true,
      },
      {
        id: "status",
        header: "Status",
        accessorKey: "status",
        sortable: true,
        cell: (row) => (
          <DomainStatusBadge variant={getDocStatusVariant(row.status)}>
            {getDocStatusLabel(row.status)}
          </DomainStatusBadge>
        ),
      },
      {
        id: "createdAt",
        header: "Hochgeladen am",
        accessorKey: "createdAt",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {formatDate(row.createdAt)}
          </span>
        ),
      },
    ],
    [],
  );

  // ─── Upload Handler ────────────────────────────────────

  const handleFileSelect = useCallback((file: File) => {
    setUploadFile(file);
    setUploadTitle(file.name.replace(/\.[^/.]+$/, ""));
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    const file = e.dataTransfer.files[0];
    if (file) handleFileSelect(file);
  }, [handleFileSelect]);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);

  const handleUpload = useCallback(async () => {
    if (!uploadFile || !uploadTitle) return;
    setUploading(true);
    try {
      await documentMutations.uploadDocument(
        uploadFile,
        uploadTitle,
        uploadDesc || undefined,
        uploadCategory || undefined,
      );
      toast.success("Dokument erfolgreich hochgeladen");
      setUploadOpen(false);
      setUploadFile(null);
      setUploadTitle("");
      setUploadDesc("");
      setUploadCategory("");
      refetch();
    } catch (err) {
      toast.error("Upload fehlgeschlagen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setUploading(false);
    }
  }, [uploadFile, uploadTitle, uploadDesc, uploadCategory, documentMutations, refetch]);

  const handleDelete = useCallback(async () => {
    if (!deleteTarget) return;
    try {
      await documentMutations.deleteDocument(deleteTarget.id);
      toast.success("Dokument gel\u00f6scht");
      setDeleteTarget(null);
      refetch();
    } catch (err) {
      toast.error("L\u00f6schen fehlgeschlagen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [deleteTarget, documentMutations, refetch]);

  const handleDownload = useCallback((doc: DocumentResponse) => {
    const token = localStorage.getItem("owlsburg_token");
    window.open(
      `${API_BASE}/api/documents/${doc.id}/download?token=${token}`,
      "_blank",
    );
  }, []);

  // ─── Error State ───────────────────────────────────────

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <AlertTriangle className="h-8 w-8 text-destructive" />
        <p className="text-sm text-muted-foreground">{error}</p>
        <Button variant="outline" size="sm" onClick={refetch}>
          Erneut versuchen
        </Button>
      </div>
    );
  }

  // ─── Render ────────────────────────────────────────────

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dokumente"
        description="Alle Dokumente verwalten und hochladen"
        breadcrumb={["Wissensdatenbank", "Dokumente"]}
        actions={
          <Button
            size="sm"
            className="gap-1.5"
            onClick={() => setUploadOpen(true)}
          >
            <Upload className="h-3.5 w-3.5" />
            Dokument hochladen
          </Button>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {loading ? (
          <>
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </>
        ) : (
          <>
            <KpiCard
              label="Dokumente gesamt"
              value={String(kpis?.total ?? 0)}
            />
            <KpiCard
              label="Gesamtspeicher"
              value={formatFileSize(kpis?.totalSize ?? 0)}
            />
            <KpiCard
              label="Hochgeladen diese Woche"
              value={String(kpis?.recentCount ?? 0)}
            />
          </>
        )}
      </div>

      {/* Data Table */}
      <DataTable<DocumentResponse>
        data={filteredDocuments}
        columns={columns}
        searchPlaceholder="Dokumente suchen..."
        searchKey="fileName"
        loading={loading}
        pageSize={15}
        onRowClick={(row) => router.push(`/knowledge/documents/${row.id}`)}
        filterSlots={
          <>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="h-9 w-40 text-sm">
                <SelectValue placeholder="Alle Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Alle Status</SelectItem>
                {DOC_STATUSES.map((s) => (
                  <SelectItem key={s} value={s}>
                    {getDocStatusLabel(s)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button
              size="sm"
              className="gap-1.5"
              onClick={() => setUploadOpen(true)}
            >
              <Upload className="h-3.5 w-3.5" />
              Hochladen
            </Button>
          </>
        }
        rowActions={[
          {
            label: "Herunterladen",
            icon: <Download className="h-3.5 w-3.5" />,
            onClick: handleDownload,
          },
          {
            label: "Details",
            icon: <Eye className="h-3.5 w-3.5" />,
            onClick: (row) => router.push(`/knowledge/documents/${row.id}`),
          },
          {
            label: "L\u00f6schen",
            icon: <Trash2 className="h-3.5 w-3.5" />,
            onClick: (row) => setDeleteTarget(row),
            variant: "destructive",
          },
        ]}
        emptyState={{
          icon: <FolderUp className="h-8 w-8 text-muted-foreground/40" />,
          title: "Noch keine Dokumente hochgeladen",
          description: "Laden Sie Ihr erstes Dokument hoch.",
          action: (
            <Button
              variant="outline"
              size="sm"
              className="mt-2 gap-1.5"
              onClick={() => setUploadOpen(true)}
            >
              <Upload className="mr-1.5 h-3.5 w-3.5" />
              Dokument hochladen
            </Button>
          ),
        }}
      />

      {/* ═══ Upload Dialog ═══ */}
      <Dialog open={uploadOpen} onOpenChange={setUploadOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Upload className="h-4 w-4 text-primary" />
              Dokument hochladen
            </DialogTitle>
            <DialogDescription>
              Datei per Drag & Drop oder \u00fcber den Button ausw\u00e4hlen.
            </DialogDescription>
          </DialogHeader>

          <div
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onClick={() => fileInputRef.current?.click()}
            className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed border-border p-8 transition-colors hover:border-primary/50 hover:bg-accent/30"
          >
            <Upload className="h-8 w-8 text-muted-foreground/50" />
            {uploadFile ? (
              <div className="text-center">
                <p className="text-sm font-medium">{uploadFile.name}</p>
                <p className="text-xs text-muted-foreground">
                  {formatFileSize(uploadFile.size)}
                </p>
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">
                Datei hierher ziehen oder klicken
              </p>
            )}
            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) handleFileSelect(file);
              }}
            />
          </div>

          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label htmlFor="doc-upload-title">Titel *</Label>
              <Input
                id="doc-upload-title"
                value={uploadTitle}
                onChange={(e) => setUploadTitle(e.target.value)}
                placeholder="Dokumenttitel"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="doc-upload-desc">Beschreibung</Label>
              <Input
                id="doc-upload-desc"
                value={uploadDesc}
                onChange={(e) => setUploadDesc(e.target.value)}
                placeholder="Optionale Beschreibung"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="doc-upload-cat">Kategorie</Label>
              <Input
                id="doc-upload-cat"
                value={uploadCategory}
                onChange={(e) => setUploadCategory(e.target.value)}
                placeholder="z.B. Handbuch, Richtlinie, Vorlage"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setUploadOpen(false)}
            >
              Abbrechen
            </Button>
            <Button
              size="sm"
              disabled={!uploadFile || !uploadTitle || uploading}
              onClick={handleUpload}
            >
              {uploading ? "Wird hochgeladen..." : "Hochladen"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ═══ Delete Confirmation ═══ */}
      <ConfirmationDialog
        open={!!deleteTarget}
        title="Dokument l\u00f6schen"
        description={`M\u00f6chten Sie "${deleteTarget?.fileName}" wirklich l\u00f6schen? Diese Aktion kann nicht r\u00fcckg\u00e4ngig gemacht werden.`}
        variant="destructive"
        confirmLabel="L\u00f6schen"
        cancelLabel="Abbrechen"
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
