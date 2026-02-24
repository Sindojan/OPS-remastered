"use client";

import { useParams, useRouter } from "next/navigation";
import { ArticleEditor } from "@/components/knowledge/article-editor";
import { useKnowledgeArticle } from "@/hooks/api/use-knowledge";

export default function EditArticlePage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { data: article, loading } = useKnowledgeArticle(id);

  if (loading) return <div className="p-6">Laden...</div>;
  if (!article) return <div className="p-6">Artikel nicht gefunden</div>;

  return <ArticleEditor article={article} onSave={() => router.push(`/knowledge/articles/${id}`)} />;
}
