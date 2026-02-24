"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import { useTheme } from "next-themes";
import MDEditor from "@uiw/react-md-editor";
import {
  ArrowLeft,
  Save,
  Send,
  Archive,
  Trash2,
  Loader2,
  ChevronRight,
  PanelRightClose,
  PanelRightOpen,
  X,
  Plus,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";

import {
  useKnowledgeCategories,
  useKnowledgeTags,
  useKnowledgeArticleMutations,
  useKnowledgeTagMutations,
} from "@/hooks/api/use-knowledge";
import type { KnowledgeArticleResponse, ArticleStatus } from "@/types/api";
import { toast } from "sonner";

// ─── Status Config ───────────────────────────────────────

const STATUS_CONFIG: Record<ArticleStatus, { label: string; className: string }> = {
  DRAFT: { label: "Entwurf", className: "bg-yellow-500/10 text-yellow-600 border-yellow-500/20" },
  PUBLISHED: { label: "Veröffentlicht", className: "bg-green-500/10 text-green-600 border-green-500/20" },
  ARCHIVED: { label: "Archiviert", className: "bg-gray-500/10 text-gray-500 border-gray-500/20" },
};

// ─── Draft Storage ───────────────────────────────────────

interface DraftData {
  title: string;
  content: string;
  excerpt: string;
  categoryId: string;
  tagIds: string[];
  savedAt: string;
}

function getDraftKey(id?: string): string {
  return `knowledge_draft_${id || "new"}`;
}

function loadDraft(id?: string): DraftData | null {
  try {
    const raw = localStorage.getItem(getDraftKey(id));
    if (!raw) return null;
    return JSON.parse(raw) as DraftData;
  } catch {
    return null;
  }
}

function saveDraft(id: string | undefined, data: DraftData) {
  try {
    localStorage.setItem(getDraftKey(id), JSON.stringify(data));
  } catch {
    // Ignore storage errors
  }
}

function clearDraft(id?: string) {
  try {
    localStorage.removeItem(getDraftKey(id));
  } catch {
    // Ignore storage errors
  }
}

// ─── Props ───────────────────────────────────────────────

interface ArticleEditorProps {
  article?: KnowledgeArticleResponse;
  initialTitle?: string;
  onSave: () => void;
}

// ─── Component ───────────────────────────────────────────

export function ArticleEditor({ article, initialTitle, onSave }: ArticleEditorProps) {
  const router = useRouter();
  const { resolvedTheme } = useTheme();

  // Data hooks
  const { data: categories } = useKnowledgeCategories();
  const { data: tags, refetch: refetchTags } = useKnowledgeTags();
  const mutations = useKnowledgeArticleMutations();
  const tagMutations = useKnowledgeTagMutations();

  // Form state
  const [title, setTitle] = useState(article?.title || initialTitle || "");
  const [content, setContent] = useState(article?.content || "");
  const [excerpt, setExcerpt] = useState(article?.excerpt || "");
  const [categoryId, setCategoryId] = useState(article?.categoryId || "");
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>(
    article?.tags.map((t) => t.id) || []
  );
  const [viewMode, setViewMode] = useState<"edit" | "preview" | "live">("live");
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [newTagName, setNewTagName] = useState("");

  // Dialog states
  const [showPublishDialog, setShowPublishDialog] = useState(false);
  const [showArchiveDialog, setShowArchiveDialog] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [showRestoreDialog, setShowRestoreDialog] = useState(false);
  const [draftToRestore, setDraftToRestore] = useState<DraftData | null>(null);

  // Autosave ref
  const autosaveRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // ─── Draft Restore Check ───────────────────────────────

  useEffect(() => {
    const draft = loadDraft(article?.id);
    if (draft) {
      const draftDate = new Date(draft.savedAt);
      const articleDate = article?.updatedAt ? new Date(article.updatedAt) : new Date(0);
      if (draftDate > articleDate) {
        setDraftToRestore(draft);
        setShowRestoreDialog(true);
      }
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleRestoreDraft = useCallback(() => {
    if (draftToRestore) {
      setTitle(draftToRestore.title);
      setContent(draftToRestore.content);
      setExcerpt(draftToRestore.excerpt);
      setCategoryId(draftToRestore.categoryId);
      setSelectedTagIds(draftToRestore.tagIds);
      toast.success("Entwurf wiederhergestellt");
    }
    setShowRestoreDialog(false);
    setDraftToRestore(null);
  }, [draftToRestore]);

  const handleDiscardDraft = useCallback(() => {
    clearDraft(article?.id);
    setShowRestoreDialog(false);
    setDraftToRestore(null);
  }, [article?.id]);

  // ─── Autosave ──────────────────────────────────────────

  useEffect(() => {
    autosaveRef.current = setInterval(() => {
      if (title || content) {
        saveDraft(article?.id, {
          title,
          content,
          excerpt,
          categoryId,
          tagIds: selectedTagIds,
          savedAt: new Date().toISOString(),
        });
      }
    }, 30000);

    return () => {
      if (autosaveRef.current) clearInterval(autosaveRef.current);
    };
  }, [title, content, excerpt, categoryId, selectedTagIds, article?.id]);

  // ─── Actions ───────────────────────────────────────────

  async function handleSave() {
    if (!title.trim()) {
      toast.error("Bitte geben Sie einen Titel ein");
      return;
    }
    try {
      const payload = {
        title,
        content,
        excerpt: excerpt || undefined,
        categoryId: categoryId || undefined,
        tagIds: selectedTagIds.length > 0 ? selectedTagIds : undefined,
      };
      if (article) {
        await mutations.updateArticle(article.id, payload);
      } else {
        await mutations.createArticle(payload);
      }
      clearDraft(article?.id);
      toast.success("Artikel gespeichert");
      onSave();
    } catch (err) {
      toast.error("Fehler beim Speichern", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }

  const handlePublish = useCallback(async () => {
    if (!article) return;
    try {
      await mutations.publishArticle(article.id);
      clearDraft(article.id);
      toast.success("Artikel veröffentlicht");
      router.push(`/knowledge/articles/${article.id}`);
    } catch (err) {
      toast.error("Fehler beim Veröffentlichen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
    setShowPublishDialog(false);
  }, [article, mutations, router]);

  const handleArchive = useCallback(async () => {
    if (!article) return;
    try {
      await mutations.archiveArticle(article.id);
      toast.success("Artikel archiviert");
    } catch (err) {
      toast.error("Fehler beim Archivieren", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
    setShowArchiveDialog(false);
  }, [article, mutations]);

  const handleDelete = useCallback(async () => {
    if (!article) return;
    try {
      await mutations.deleteArticle(article.id);
      clearDraft(article.id);
      toast.success("Artikel gelöscht");
      router.push("/knowledge");
    } catch (err) {
      toast.error("Fehler beim Löschen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
    setShowDeleteDialog(false);
  }, [article, mutations, router]);

  // ─── Tag Handling ──────────────────────────────────────

  const handleAddTag = useCallback(
    async (tagId: string) => {
      if (!selectedTagIds.includes(tagId)) {
        setSelectedTagIds((prev) => [...prev, tagId]);
      }
    },
    [selectedTagIds]
  );

  const handleRemoveTag = useCallback((tagId: string) => {
    setSelectedTagIds((prev) => prev.filter((id) => id !== tagId));
  }, []);

  const handleCreateTag = useCallback(async () => {
    if (!newTagName.trim()) return;
    try {
      const newTag = await tagMutations.createTag(newTagName.trim());
      if (newTag) {
        setSelectedTagIds((prev) => [...prev, newTag.id]);
        setNewTagName("");
        refetchTags();
        toast.success(`Tag "${newTag.name}" erstellt`);
      }
    } catch (err) {
      toast.error("Fehler beim Erstellen des Tags", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [newTagName, tagMutations, refetchTags]);

  // ─── Derived State ─────────────────────────────────────

  const currentStatus: ArticleStatus = article?.status || "DRAFT";
  const statusConfig = STATUS_CONFIG[currentStatus];
  const availableTags = (tags || []).filter((t) => !selectedTagIds.includes(t.id));
  const selectedTagObjects = (tags || []).filter((t) => selectedTagIds.includes(t.id));

  // ─── Render ────────────────────────────────────────────

  return (
    <div className="space-y-4">
      {/* Top bar */}
      <div className="flex items-center justify-between gap-4">
        <Button
          variant="ghost"
          size="sm"
          className="gap-1.5 text-muted-foreground"
          onClick={() => router.back()}
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Zurück
        </Button>

        <div className="flex items-center gap-2">
          {/* View mode toggle */}
          <div className="flex rounded-md border border-border/50">
            {(["edit", "live", "preview"] as const).map((mode) => (
              <button
                key={mode}
                onClick={() => setViewMode(mode)}
                className={`px-3 py-1.5 text-xs font-medium transition-colors first:rounded-l-md last:rounded-r-md ${
                  viewMode === mode
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {mode === "edit" ? "Editor" : mode === "preview" ? "Vorschau" : "Geteilt"}
              </button>
            ))}
          </div>

          {/* Sidebar toggle */}
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={() => setSidebarOpen(!sidebarOpen)}
          >
            {sidebarOpen ? (
              <PanelRightClose className="h-4 w-4" />
            ) : (
              <PanelRightOpen className="h-4 w-4" />
            )}
          </Button>

          {/* Status badge */}
          <Badge variant="outline" className={statusConfig.className}>
            {statusConfig.label}
          </Badge>

          {/* Delete (only DRAFT) */}
          {currentStatus === "DRAFT" && article && (
            <Button
              variant="ghost"
              size="sm"
              className="gap-1.5 text-destructive hover:text-destructive"
              onClick={() => setShowDeleteDialog(true)}
            >
              <Trash2 className="h-3.5 w-3.5" />
              Löschen
            </Button>
          )}

          {/* Archive (only PUBLISHED) */}
          {currentStatus === "PUBLISHED" && (
            <Button
              variant="outline"
              size="sm"
              className="gap-1.5"
              onClick={() => setShowArchiveDialog(true)}
            >
              <Archive className="h-3.5 w-3.5" />
              Archivieren
            </Button>
          )}

          {/* Publish (only DRAFT) */}
          {currentStatus === "DRAFT" && article && (
            <Button
              variant="outline"
              size="sm"
              className="gap-1.5"
              onClick={() => setShowPublishDialog(true)}
            >
              <Send className="h-3.5 w-3.5" />
              Veröffentlichen
            </Button>
          )}

          {/* Save */}
          <Button
            size="sm"
            className="gap-1.5"
            onClick={handleSave}
            disabled={mutations.loading || !title.trim()}
          >
            {mutations.loading ? (
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
            ) : (
              <Save className="h-3.5 w-3.5" />
            )}
            Speichern
          </Button>
        </div>
      </div>

      {/* Title input */}
      <Input
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        placeholder="Artikeltitel eingeben..."
        className="border-none bg-transparent text-2xl font-semibold tracking-tight placeholder:text-muted-foreground/50 focus-visible:ring-0 focus-visible:ring-offset-0 px-0"
      />

      {/* Main area: Editor + Sidebar */}
      <div className="flex gap-6">
        {/* Editor */}
        <div className="min-w-0 flex-1">
          <div data-color-mode={resolvedTheme === "dark" ? "dark" : "light"}>
            <MDEditor
              value={content}
              onChange={(val) => setContent(val || "")}
              height={600}
              preview={viewMode}
            />
          </div>
        </div>

        {/* Sidebar */}
        {sidebarOpen && (
          <aside className="w-[300px] shrink-0 space-y-6">
            {/* Category */}
            <div className="space-y-2">
              <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Kategorie
              </Label>
              <Select value={categoryId || "none"} onValueChange={(v) => setCategoryId(v === "none" ? "" : v)}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Kategorie auswählen..." />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">Keine Kategorie</SelectItem>
                  {(categories || []).map((cat) => (
                    <SelectItem key={cat.id} value={cat.id}>
                      <span className="flex items-center gap-2">
                        {cat.color && (
                          <span
                            className="inline-block h-2.5 w-2.5 rounded-full"
                            style={{ backgroundColor: cat.color }}
                          />
                        )}
                        {cat.name}
                      </span>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Tags */}
            <div className="space-y-2">
              <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Tags
              </Label>
              {/* Selected tags */}
              {selectedTagObjects.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                  {selectedTagObjects.map((tag) => (
                    <Badge
                      key={tag.id}
                      variant="secondary"
                      className="gap-1 text-xs"
                    >
                      {tag.name}
                      <button
                        onClick={() => handleRemoveTag(tag.id)}
                        className="ml-0.5 hover:text-destructive"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </Badge>
                  ))}
                </div>
              )}
              {/* Add existing tag */}
              {availableTags.length > 0 && (
                <div className="flex flex-wrap gap-1">
                  {availableTags.map((tag) => (
                    <button
                      key={tag.id}
                      onClick={() => handleAddTag(tag.id)}
                      className="rounded-md border border-border/50 px-2 py-0.5 text-xs text-muted-foreground transition-colors hover:border-primary/50 hover:text-foreground"
                    >
                      + {tag.name}
                    </button>
                  ))}
                </div>
              )}
              {/* Create new tag */}
              <div className="flex gap-1.5">
                <Input
                  value={newTagName}
                  onChange={(e) => setNewTagName(e.target.value)}
                  placeholder="Neuer Tag..."
                  className="text-sm"
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      handleCreateTag();
                    }
                  }}
                />
                <Button
                  variant="outline"
                  size="icon"
                  className="h-9 w-9 shrink-0"
                  onClick={handleCreateTag}
                  disabled={!newTagName.trim() || tagMutations.loading}
                >
                  <Plus className="h-3.5 w-3.5" />
                </Button>
              </div>
            </div>

            {/* Excerpt */}
            <div className="space-y-2">
              <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Kurzbeschreibung
              </Label>
              <Textarea
                value={excerpt}
                onChange={(e) => setExcerpt(e.target.value.slice(0, 500))}
                placeholder="Kurze Beschreibung des Artikels..."
                rows={4}
                className="text-sm resize-none"
              />
              <p className="text-xs text-muted-foreground text-right">
                {excerpt.length}/500
              </p>
            </div>
          </aside>
        )}
      </div>

      {/* Confirmation Dialogs */}
      <ConfirmationDialog
        open={showPublishDialog}
        title="Artikel veröffentlichen?"
        description="Der Artikel wird für alle Benutzer sichtbar. Sie können ihn später archivieren."
        onConfirm={handlePublish}
        onCancel={() => setShowPublishDialog(false)}
        confirmLabel="Veröffentlichen"
      />
      <ConfirmationDialog
        open={showArchiveDialog}
        title="Artikel archivieren?"
        description="Der Artikel wird aus der öffentlichen Ansicht entfernt, bleibt aber erhalten."
        onConfirm={handleArchive}
        onCancel={() => setShowArchiveDialog(false)}
        confirmLabel="Archivieren"
      />
      <ConfirmationDialog
        open={showDeleteDialog}
        title="Artikel löschen?"
        description="Der Entwurf wird unwiderruflich gelöscht. Diese Aktion kann nicht rückgängig gemacht werden."
        onConfirm={handleDelete}
        onCancel={() => setShowDeleteDialog(false)}
        variant="destructive"
        confirmLabel="Endgültig löschen"
      />
      <ConfirmationDialog
        open={showRestoreDialog}
        title="Entwurf wiederherstellen?"
        description="Es wurde ein neuerer lokaler Entwurf gefunden. Möchten Sie diesen wiederherstellen?"
        onConfirm={handleRestoreDraft}
        onCancel={handleDiscardDraft}
        confirmLabel="Wiederherstellen"
        cancelLabel="Verwerfen"
      />
    </div>
  );
}
