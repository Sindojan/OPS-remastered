"use client";

import { PageHeader } from "@/components/shared/page-header";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { HierarchyView } from "@/components/agents/hierarchy-view";

export default function AgentConsolePage() {
  return (
    <div className="flex h-full flex-col">
      <PageHeader
        title="Agent Konsole"
        description="Übersicht und Steuerung aller Agents"
        breadcrumb={["Agents", "Konsole"]}
      />
      <Tabs defaultValue="hierarchy" className="flex-1">
        <TabsList>
          <TabsTrigger value="hierarchy">Hierarchie</TabsTrigger>
        </TabsList>
        <TabsContent value="hierarchy" className="mt-4 flex-1">
          <HierarchyView />
        </TabsContent>
      </Tabs>
    </div>
  );
}
