"use client";

import { PageHeader } from "@/components/shared/page-header";
import { SystemHierarchyView } from "@/components/agents/system-hierarchy-view";

export default function SystemAgentsPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="System-Agenten"
        description="Live-Hierarchie aller System-Agenten (CEO + 8 Leads)"
      />
      <div className="h-[calc(100vh-14rem)]">
        <SystemHierarchyView />
      </div>
    </div>
  );
}
