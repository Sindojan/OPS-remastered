-- Knowledge categories (MUST come before articles since articles reference it)
CREATE TABLE knowledge_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES public.tenants(id),
    name        VARCHAR(100) NOT NULL,
    color       VARCHAR(7),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_knowledge_categories_tenant_id ON knowledge_categories(tenant_id);

-- Knowledge articles
CREATE TABLE knowledge_articles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES public.tenants(id),
    title           VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL,
    content         TEXT,
    excerpt         VARCHAR(500),
    status          VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    category_id     UUID REFERENCES knowledge_categories(id),
    author_id       UUID REFERENCES public.users(id),
    published_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, slug)
);
CREATE INDEX idx_knowledge_articles_tenant_id ON knowledge_articles(tenant_id);
CREATE INDEX idx_knowledge_articles_status ON knowledge_articles(tenant_id, status);
CREATE INDEX idx_knowledge_articles_slug ON knowledge_articles(tenant_id, slug);
CREATE INDEX idx_knowledge_articles_category ON knowledge_articles(category_id);

-- Knowledge tags
CREATE TABLE knowledge_tags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES public.tenants(id),
    name        VARCHAR(100) NOT NULL,
    UNIQUE(tenant_id, name)
);
CREATE INDEX idx_knowledge_tags_tenant_id ON knowledge_tags(tenant_id);

-- Article-Tag join table
CREATE TABLE knowledge_article_tags (
    article_id  UUID NOT NULL REFERENCES knowledge_articles(id) ON DELETE CASCADE,
    tag_id      UUID NOT NULL REFERENCES knowledge_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, tag_id)
);

-- Extend documents table
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS category_id UUID REFERENCES knowledge_categories(id),
    ADD COLUMN IF NOT EXISTS excerpt VARCHAR(500);

-- Document-Tag join table (using knowledge_tags)
CREATE TABLE document_knowledge_tags (
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    tag_id      UUID NOT NULL REFERENCES knowledge_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, tag_id)
);

-- RLS on all new tables
DO $$
DECLARE
  t TEXT;
BEGIN
  FOR t IN VALUES ('knowledge_categories'), ('knowledge_articles'), ('knowledge_tags')
  LOOP
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t);
    EXECUTE format('ALTER TABLE public.%I FORCE ROW LEVEL SECURITY', t);
    EXECUTE format(
      'CREATE POLICY tenant_isolation ON public.%I USING (tenant_id = current_tenant_id())',
      t
    );
  END LOOP;
END $$;
