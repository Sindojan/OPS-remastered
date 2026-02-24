"use client";

import { useState, useMemo, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import {
  Plus,
  Search,
  BookOpen,
  Tag,
  Upload,
  FileText,
  File,
  FileSpreadsheet,
  Image,
  AlertTriangle,
  X,
  Folder,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/shared/kpi-card";
import { SkeletonCard } from "@/components/shared/skeleton-variants";
import {
  DomainStatusBadge,
} from "@/components/shared/domain-status-badge";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
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
import {
  useKnowledgeArticles,
  useKnowledgeCategories,
  useKnowledgeSearch,
} from "@/hooks/api/use-knowledge";
import { useDocuments, useDocumentMutations } from "@/hooks/api/use-documents";
import type {
  KnowledgeArticleSummaryResponse,
  KnowledgeCategoryResponse,
  DocumentResponse,
} from "@/types/api";
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

function getArticleStatusVariant(status: string) {
  switch (status) {
    case "DRAFT": return "warning" as const;
    case "PUBLISHED": return "success" as const;
    case "ARCHIVED": return "neutral" as const;
    default: return "neutral" as const;
  }
}

function getArticleStatusLabel(status: string) {
  switch (status) {
    case "DRAFT": return "Entwurf";
    case "PUBLISHED": return "Ver\u00f6ffentlicht";
    case "ARCHIVED": return "Archiviert";
    default: return status;
  }
}

// ─── Component ──────────────────────────────────────────

export default function KnowledgePage() {
  const router = useRouter();

  // State
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [createArticleOpen, setCreateArticleOpen] = useState(false);

  // Data
  const { data: categories, loading: categoriesLoading } = useKnowledgeCategories();
  const { data: articles, loading: articlesLoading, refetch: refetchArticles } = useKnowledgeArticles(
    undefined,
    selectedCategory ?? undefined,
    undefined,
  );
  const { data: documents, loading: documentsLoading, refetch: refetchDocuments } = useDocuments();
  const { data: searchResults, loading: searchLoading } = useKnowledgeSearch(
    searchQuery.length >= 2 ? searchQuery : null,
  );

  // Upload state
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadTitle, setUploadTitle] = useState("");
  const [uploadDesc, setUploadDesc] = useState("");
  const [uploadCategory, setUploadCategory] = useState("");
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const dropZoneRef = useRef<HTMLDivElement>(null);

  // Create article state
  const [newArticleTitle, setNewArticleTitle] = useState("");

  const documentMutations = useDocumentMutations();

  // ─── KPI Computations ──────────────────────────────────

  const kpis = useMemo(() => {
    const totalArticles = articles?.length ?? 0;
    const publishedArticles = articles?.filter((a) => a.status === "PUBLISHED").length ?? 0;
    const totalDocs = documents?.length ?? 0;
    return { totalArticles, publishedArticles, totalDocs };
  }, [articles, documents]);

  // ─── Filtered Articles ─────────────────────────────────

  const filteredArticles = useMemo(() => {
    if (!articles) return [];
    let result = articles;
    if (selectedCategory) {
      result = result.filter((a) => a.categoryId === selectedCategory);
    }
    return result;
  }, [articles, selectedCategory]);

  // ─── Recent Documents ──────────────────────────────────

  const recentDocuments = useMemo(() => {
    if (!documents) return [];
    return [...documents]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 10);
  }, [documents]);

  // ─── Upload Handlers ──────────────────────────────────

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
      refetchDocuments();
    } catch (err) {
      toast.error("Upload fehlgeschlagen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setUploading(false);
    }
  }, [uploadFile, uploadTitle, uploadDesc, uploadCategory, documentMutations, refetchDocuments]);

  const handleCreateArticle = useCallback(async () => {
    if (!newArticleTitle.trim()) return;
    // Navigate to a future article editor - for now just show placeholder
    router.push(`/knowledge/articles/new?title=${encodeURIComponent(newArticleTitle)}`);
    setCreateArticleOpen(false);
    setNewArticleTitle("");
  }, [newArticleTitle, router]);

  // ─── Error State ───────────────────────────────────────

  const hasError = false; // articles/documents errors handled individually

  // ─── Render ────────────────────────────────────────────

  const isSearching = searchQuery.length >= 2;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Wissensdatenbank"
        description="Artikel, Dokumente und Wissen verwalten"
        actions={
          <Button
            size="sm"
            className="gap-1.5"
            onClick={() => setCreateArticleOpen(true)}
          >
            <Plus className="h-3.5 w-3.5" />
            Neuer Artikel
          </Button>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {articlesLoading || documentsLoading ? (
          <>
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </>
        ) : (
          <>
            <KpiCard
              label="Artikel gesamt"
              value={String(kpis.totalArticles)}
            />
            <KpiCard
              label="Ver\u00f6ffentlicht"
              value={String(kpis.publishedArticles)}
              trend={
                kpis.totalArticles > 0
                  ? {
                      direction: kpis.publishedArticles / kpis.totalArticles > 0.5 ? "up" : "down",
                      value: `${Math.round((kpis.publishedArticles / kpis.totalArticles) * 100)}%`,
                    }
                  : undefined
              }
            />
            <KpiCard
              label="Dokumente"
              value={String(kpis.totalDocs)}
            />
          </>
        )}
      </div>

      {/* Search Bar */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Artikel und Dokumente durchsuchen..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="pl-10"
        />
        {searchQuery && (
          <Button
            variant="ghost"
            size="icon-xs"
            className="absolute right-2 top-1/2 -translate-y-1/2"
            onClick={() => setSearchQuery("")}
          >
            <X className="h-3.5 w-3.5" />
          </Button>
        )}
      </div>

      {/* Search Results */}
      {isSearching && (
        <div className="space-y-3">
          <h3 className="text-sm font-medium text-muted-foreground">
            Suchergebnisse {searchLoading && "..."}
          </h3>
          {searchResults && searchResults.length > 0 ? (
            <div className="grid gap-2">
              {searchResults.map((result) => (
                <Card
                  key={`${result.type}-${result.id}`}
                  className="flex cursor-pointer items-center gap-3 p-3 transition-colors hover:bg-accent/50"
                  onClick={() => {
                    if (result.type === "article") {
                      router.push(`/knowledge/articles/${result.id}`);
                    } else {
                      router.push(`/knowledge/documents/${result.id}`);
                    }
                  }}
                >
                  {result.type === "article" ? (
                    <BookOpen className="h-4 w-4 shrink-0 text-primary" />
                  ) : (
                    <FileText className="h-4 w-4 shrink-0 text-muted-foreground" />
                  )}
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{result.title}</p>
                    {result.excerpt && (
                      <p className="truncate text-xs text-muted-foreground">{result.excerpt}</p>
                    )}
                  </div>
                  {result.category && (
                    <Badge variant="secondary" className="text-[10px]">
                      {result.category}
                    </Badge>
                  )}
                  <span className="shrink-0 font-mono text-[10px] text-muted-foreground">
                    {formatDate(result.updatedAt)}
                  </span>
                </Card>
              ))}
            </div>
          ) : !searchLoading ? (
            <p className="text-sm text-muted-foreground">Keine Ergebnisse gefunden.</p>
          ) : null}
        </div>
      )}

      {/* Category Pills */}
      {!isSearching && (
        <>
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => setSelectedCategory(null)}
              className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                !selectedCategory
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-border text-muted-foreground hover:border-primary/50 hover:text-foreground"
              }`}
            >
              Alle
            </button>
            {categoriesLoading ? (
              <>
                <Skeleton className="h-7 w-20 rounded-full" />
                <Skeleton className="h-7 w-24 rounded-full" />
                <Skeleton className="h-7 w-16 rounded-full" />
              </>
            ) : (
              categories?.map((cat) => (
                <button
                  key={cat.id}
                  onClick={() => setSelectedCategory(selectedCategory === cat.id ? null : cat.id)}
                  className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                    selectedCategory === cat.id
                      ? "border-primary bg-primary/10 text-primary"
                      : "border-border text-muted-foreground hover:border-primary/50 hover:text-foreground"
                  }`}
                  style={
                    cat.color && selectedCategory === cat.id
                      ? { borderColor: cat.color, backgroundColor: `${cat.color}15`, color: cat.color }
                      : undefined
                  }
                >
                  {cat.name}
                </button>
              ))
            )}
          </div>

          {/* Main Content: Articles + Documents Sidebar */}
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
            {/* Articles Grid */}
            <div className="lg:col-span-3 space-y-4">
              {articlesLoading ? (
                <div className="grid gap-4 sm:grid-cols-2">
                  {Array.from({ length: 4 }).map((_, i) => (
                    <Card key={i} className="p-4 space-y-3">
                      <Skeleton className="h-5 w-3/4" />
                      <Skeleton className="h-3 w-full" />
                      <Skeleton className="h-3 w-2/3" />
                      <div className="flex gap-2">
                        <Skeleton className="h-5 w-16 rounded-full" />
                        <Skeleton className="h-5 w-12 rounded-full" />
                      </div>
                    </Card>
                  ))}
                </div>
              ) : filteredArticles.length === 0 ? (
                <Card className="flex flex-col items-center justify-center gap-3 py-16">
                  <BookOpen className="h-8 w-8 text-muted-foreground/40" />
                  <p className="text-sm font-medium text-foreground/70">
                    Noch keine Artikel vorhanden
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Erstellen Sie Ihren ersten Wissensartikel.
                  </p>
                  <Button
                    variant="outline"
                    size="sm"
                    className="mt-2 gap-1.5"
                    onClick={() => setCreateArticleOpen(true)}
                  >
                    <Plus className="h-3.5 w-3.5" />
                    Ersten Artikel erstellen
                  </Button>
                </Card>
              ) : (
                <div className="grid gap-4 sm:grid-cols-2">
                  {filteredArticles.map((article) => (
                    <ArticleCard
                      key={article.id}
                      article={article}
                      onClick={() => router.push(`/knowledge/articles/${article.id}`)}
                    />
                  ))}
                </div>
              )}
            </div>

            {/* Documents Sidebar */}
            <div className="lg:col-span-2 space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="flex items-center gap-2 text-sm font-semibold">
                  <Folder className="h-4 w-4 text-muted-foreground" />
                  Dokumente
                  {documents && (
                    <span className="font-mono text-xs font-normal text-muted-foreground">
                      ({documents.length})
                    </span>
                  )}
                </h3>
                <div className="flex items-center gap-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-xs text-muted-foreground"
                    onClick={() => router.push("/knowledge/documents")}
                  >
                    Alle anzeigen
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="gap-1.5"
                    onClick={() => setUploadOpen(true)}
                  >
                    <Upload className="h-3 w-3" />
                    Hochladen
                  </Button>
                </div>
              </div>

              {documentsLoading ? (
                <div className="space-y-2">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <div key={i} className="flex items-center gap-3 rounded-md border p-2.5">
                      <Skeleton className="h-8 w-8 rounded" />
                      <div className="flex-1 space-y-1">
                        <Skeleton className="h-3.5 w-32" />
                        <Skeleton className="h-2.5 w-20" />
                      </div>
                    </div>
                  ))}
                </div>
              ) : recentDocuments.length === 0 ? (
                <Card className="flex flex-col items-center justify-center gap-3 py-12">
                  <FileText className="h-8 w-8 text-muted-foreground/40" />
                  <p className="text-sm font-medium text-foreground/70">
                    Noch keine Dokumente hochgeladen
                  </p>
                  <Button
                    variant="outline"
                    size="sm"
                    className="mt-2 gap-1.5"
                    onClick={() => setUploadOpen(true)}
                  >
                    <Upload className="h-3.5 w-3.5" />
                    Dokument hochladen
                  </Button>
                </Card>
              ) : (
                <div className="space-y-1">
                  {recentDocuments.map((doc) => (
                    <DocumentRow
                      key={doc.id}
                      document={doc}
                      onClick={() => router.push(`/knowledge/documents/${doc.id}`)}
                    />
                  ))}
                </div>
              )}
            </div>
          </div>
        </>
      )}

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

          {/* Drop Zone */}
          <div
            ref={dropZoneRef}
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
              <>
                <p className="text-sm text-muted-foreground">
                  Datei hierher ziehen oder klicken
                </p>
              </>
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
              <Label htmlFor="upload-title">Titel *</Label>
              <Input
                id="upload-title"
                value={uploadTitle}
                onChange={(e) => setUploadTitle(e.target.value)}
                placeholder="Dokumenttitel"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="upload-desc">Beschreibung</Label>
              <Input
                id="upload-desc"
                value={uploadDesc}
                onChange={(e) => setUploadDesc(e.target.value)}
                placeholder="Optionale Beschreibung"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="upload-cat">Kategorie</Label>
              <Input
                id="upload-cat"
                value={uploadCategory}
                onChange={(e) => setUploadCategory(e.target.value)}
                placeholder="z.B. Handbuch, Richtlinie, Vorlage"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              type="button"
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

      {/* ═══ Create Article Dialog ═══ */}
      <Dialog open={createArticleOpen} onOpenChange={setCreateArticleOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <BookOpen className="h-4 w-4 text-primary" />
              Neuer Artikel
            </DialogTitle>
            <DialogDescription>
              Neuen Wissensartikel erstellen.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label htmlFor="article-title">Titel *</Label>
            <Input
              id="article-title"
              value={newArticleTitle}
              onChange={(e) => setNewArticleTitle(e.target.value)}
              placeholder="Artikeltitel eingeben"
              onKeyDown={(e) => {
                if (e.key === "Enter") handleCreateArticle();
              }}
            />
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCreateArticleOpen(false)}
            >
              Abbrechen
            </Button>
            <Button
              size="sm"
              disabled={!newArticleTitle.trim()}
              onClick={handleCreateArticle}
            >
              Artikel erstellen
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ─── Sub-Components ──────────────────────────────────────

function ArticleCard({
  article,
  onClick,
}: {
  article: KnowledgeArticleSummaryResponse;
  onClick: () => void;
}) {
  return (
    <Card
      className="flex cursor-pointer flex-col gap-2.5 p-4 transition-colors hover:bg-accent/30"
      onClick={onClick}
    >
      <div className="flex items-start justify-between gap-2">
        <h4 className="line-clamp-1 text-sm font-semibold text-foreground">
          {article.title}
        </h4>
        {article.status !== "PUBLISHED" && (
          <DomainStatusBadge variant={getArticleStatusVariant(article.status)}>
            {getArticleStatusLabel(article.status)}
          </DomainStatusBadge>
        )}
      </div>

      {article.excerpt && (
        <p className="line-clamp-2 text-xs text-muted-foreground">
          {article.excerpt}
        </p>
      )}

      <div className="flex flex-wrap items-center gap-1.5">
        {article.categoryName && (
          <Badge
            variant="secondary"
            className="text-[10px]"
            style={
              article.categoryColor
                ? {
                    backgroundColor: `${article.categoryColor}15`,
                    color: article.categoryColor,
                    borderColor: `${article.categoryColor}30`,
                  }
                : undefined
            }
          >
            {article.categoryName}
          </Badge>
        )}
        {article.tags.map((tag) => (
          <span
            key={tag.id}
            className="inline-flex items-center gap-0.5 rounded border border-border px-1.5 py-0.5 text-[10px] text-muted-foreground"
          >
            <Tag className="h-2.5 w-2.5" />
            {tag.name}
          </span>
        ))}
      </div>

      <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
        {article.authorName && <span>{article.authorName}</span>}
        {article.authorName && <span>\u00b7</span>}
        <span className="font-mono">{formatDate(article.publishedAt ?? article.createdAt)}</span>
      </div>
    </Card>
  );
}

function DocumentRow({
  document,
  onClick,
}: {
  document: DocumentResponse;
  onClick: () => void;
}) {
  const IconComponent = getFileIcon(document.mimeType);

  return (
    <div
      className="flex cursor-pointer items-center gap-3 rounded-md border border-transparent p-2.5 transition-colors hover:border-border hover:bg-accent/30"
      onClick={onClick}
    >
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded bg-muted">
        <IconComponent className="h-4 w-4 text-muted-foreground" />
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium">{document.fileName}</p>
        <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
          <span className="font-mono">{formatFileSize(document.fileSizeBytes)}</span>
          <span>\u00b7</span>
          <span className="font-mono">{formatDate(document.createdAt)}</span>
        </div>
      </div>
    </div>
  );
}
