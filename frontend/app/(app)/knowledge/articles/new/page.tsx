"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { ArticleEditor } from "@/components/knowledge/article-editor";

export default function NewArticlePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const initialTitle = searchParams.get("title") || undefined;

  return (
    <ArticleEditor
      initialTitle={initialTitle}
      onSave={() => router.push("/knowledge")}
    />
  );
}
