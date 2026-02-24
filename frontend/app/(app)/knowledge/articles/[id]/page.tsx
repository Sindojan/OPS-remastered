"use client";

import { useMemo } from "react";
import { useParams, useRouter } from "next/navigation";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeHighlight from "rehype-highlight";
import { ArrowLeft, Pencil, Clock, User } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

import { useKnowledgeArticle } from "@/hooks/api/use-knowledge";
import { formatDate } from "@/lib/format";
import type { ArticleStatus } from "@/types/api";

// ─── Status Badge Helpers ────────────────────────────────

const STATUS_CONFIG: Record<ArticleStatus, { label: string; className: string }> = {
  DRAFT: { label: "Entwurf", className: "bg-yellow-500/10 text-yellow-600 border-yellow-500/20" },
  PUBLISHED: { label: "Veröffentlicht", className: "bg-green-500/10 text-green-600 border-green-500/20" },
  ARCHIVED: { label: "Archiviert", className: "bg-gray-500/10 text-gray-500 border-gray-500/20" },
};

// ─── TOC extraction ──────────────────────────────────────

interface TocEntry {
  level: number;
  text: string;
  id: string;
}

function extractToc(markdown: string): TocEntry[] {
  const headingRegex = /^(#{2,3})\s+(.+)$/gm;
  const entries: TocEntry[] = [];
  let match;
  while ((match = headingRegex.exec(markdown)) !== null) {
    const level = match[1].length;
    const text = match[2].trim();
    const id = text
      .toLowerCase()
      .replace(/[^\w\s-]/g, "")
      .replace(/\s+/g, "-");
    entries.push({ level, text, id });
  }
  return entries;
}

// ─── Component ───────────────────────────────────────────

export default function ArticleDetailPage() {
  const params = useParams();
  const router = useRouter();
  const articleId = params.id as string;

  const { data: article, loading } = useKnowledgeArticle(articleId);

  const toc = useMemo(() => {
    if (!article?.content) return [];
    return extractToc(article.content);
  }, [article?.content]);

  // Loading skeleton
  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-6 w-96" />
        <div className="flex gap-8">
          <div className="flex-1 space-y-4">
            <Skeleton className="h-96 w-full" />
          </div>
          <div className="w-[280px] space-y-3">
            <Skeleton className="h-6 w-40" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-4 w-full" />
          </div>
        </div>
      </div>
    );
  }

  if (!article) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <p className="text-sm text-muted-foreground">Artikel nicht gefunden</p>
        <Button variant="outline" size="sm" onClick={() => router.push("/knowledge")}>
          Zur Wissensdatenbank
        </Button>
      </div>
    );
  }

  const statusConfig = STATUS_CONFIG[article.status] || STATUS_CONFIG.DRAFT;

  return (
    <div className="space-y-6">
      {/* Back button */}
      <Button
        variant="ghost"
        size="sm"
        className="gap-1.5 text-muted-foreground"
        onClick={() => router.push("/knowledge")}
      >
        <ArrowLeft className="h-3.5 w-3.5" />
        Zurück zur Wissensdatenbank
      </Button>

      {/* Article header */}
      <div className="space-y-3">
        <div className="flex items-start justify-between gap-4">
          <h1 className="text-2xl font-semibold tracking-tight">{article.title}</h1>
          <Button
            variant="outline"
            size="sm"
            className="gap-1.5 shrink-0"
            onClick={() => router.push(`/knowledge/articles/${article.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
            Bearbeiten
          </Button>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Status badge */}
          <Badge variant="outline" className={statusConfig.className}>
            {statusConfig.label}
          </Badge>

          {/* Category badge */}
          {article.categoryName && (
            <Badge
              variant="outline"
              style={
                article.categoryColor
                  ? { borderColor: article.categoryColor, color: article.categoryColor }
                  : undefined
              }
            >
              {article.categoryName}
            </Badge>
          )}

          {/* Tags */}
          {article.tags.map((tag) => (
            <Badge key={tag.id} variant="secondary" className="text-xs">
              {tag.name}
            </Badge>
          ))}
        </div>

        {/* Author + Date */}
        <div className="flex items-center gap-4 text-xs text-muted-foreground">
          {article.authorName && (
            <span className="flex items-center gap-1">
              <User className="h-3 w-3" />
              {article.authorName}
            </span>
          )}
          <span className="flex items-center gap-1">
            <Clock className="h-3 w-3" />
            {article.status === "PUBLISHED" && article.publishedAt
              ? `Veröffentlicht am ${formatDate(article.publishedAt)}`
              : `Erstellt am ${formatDate(article.createdAt)}`}
          </span>
        </div>
      </div>

      {/* Content + Sidebar */}
      <div className="flex gap-8">
        {/* Main content */}
        <div className="min-w-0 flex-1">
          {article.content ? (
            <div className="prose prose-sm dark:prose-invert max-w-none">
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                rehypePlugins={[rehypeHighlight]}
                components={{
                  h2: ({ children, ...props }) => {
                    const text = typeof children === "string" ? children : String(children);
                    const id = text
                      .toLowerCase()
                      .replace(/[^\w\s-]/g, "")
                      .replace(/\s+/g, "-");
                    return <h2 id={id} {...props}>{children}</h2>;
                  },
                  h3: ({ children, ...props }) => {
                    const text = typeof children === "string" ? children : String(children);
                    const id = text
                      .toLowerCase()
                      .replace(/[^\w\s-]/g, "")
                      .replace(/\s+/g, "-");
                    return <h3 id={id} {...props}>{children}</h3>;
                  },
                }}
              >
                {article.content}
              </ReactMarkdown>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground italic">Kein Inhalt vorhanden.</p>
          )}

          {/* Footer */}
          <div className="mt-8 border-t border-border/50 pt-4">
            <p className="text-xs text-muted-foreground">
              Zuletzt aktualisiert: {formatDate(article.updatedAt)}
            </p>
          </div>
        </div>

        {/* Sidebar - TOC */}
        {toc.length > 0 && (
          <aside className="hidden w-[280px] shrink-0 lg:block">
            <div className="sticky top-6">
              <h4 className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Inhaltsverzeichnis
              </h4>
              <nav className="space-y-1">
                {toc.map((entry) => (
                  <a
                    key={entry.id}
                    href={`#${entry.id}`}
                    className={`block text-sm text-muted-foreground transition-colors hover:text-foreground ${
                      entry.level === 3 ? "pl-4" : ""
                    }`}
                    onClick={(e) => {
                      e.preventDefault();
                      const el = document.getElementById(entry.id);
                      el?.scrollIntoView({ behavior: "smooth" });
                    }}
                  >
                    {entry.text}
                  </a>
                ))}
              </nav>
            </div>
          </aside>
        )}
      </div>
    </div>
  );
}
