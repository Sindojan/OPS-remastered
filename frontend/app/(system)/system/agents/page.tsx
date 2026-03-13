"use client";

import { useRouter } from "next/navigation";
import { useSystemAgents } from "@/hooks/api/use-system-agents";
import { PageHeader } from "@/components/shared/page-header";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Loader2 } from "lucide-react";

function ActivityBadge({ status }: { status: string }) {
  switch (status) {
    case "BUSY":
      return (
        <Badge variant="outline" className="border-amber-500/50 text-amber-500 animate-pulse">
          Beschäftigt
        </Badge>
      );
    case "ERROR":
      return (
        <Badge variant="outline" className="border-destructive/50 text-destructive">
          Fehler
        </Badge>
      );
    case "IDLE":
    default:
      return (
        <Badge variant="outline" className="border-emerald-500/50 text-emerald-500">
          Bereit
        </Badge>
      );
  }
}

export default function SystemAgentsPage() {
  const { data: agents, loading } = useSystemAgents();
  const router = useRouter();

  return (
    <div className="space-y-6">
      <PageHeader
        title="System-Agenten"
        description="Übersicht aller 9 System-Agenten (CEO + 8 Leads)"
      />

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Rolle</TableHead>
                <TableHead>Model</TableHead>
                <TableHead>Tools</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Aktivität</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {agents?.map((agent) => (
                <TableRow
                  key={agent.id}
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => router.push(`/system/agents/${agent.id}`)}
                >
                  <TableCell className="font-medium">{agent.name}</TableCell>
                  <TableCell className="text-muted-foreground font-mono text-xs">
                    {agent.role || "—"}
                  </TableCell>
                  <TableCell className="font-mono text-xs">
                    {agent.model?.includes("opus")
                      ? "Opus"
                      : agent.model?.includes("haiku")
                        ? "Haiku"
                        : "Sonnet"}
                  </TableCell>
                  <TableCell>{agent.toolCount}</TableCell>
                  <TableCell>
                    <Badge
                      variant={agent.status === "ACTIVE" ? "default" : "secondary"}
                    >
                      {agent.status === "ACTIVE" ? "Aktiv" : "Inaktiv"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <ActivityBadge status={agent.activityStatus} />
                  </TableCell>
                </TableRow>
              ))}
              {(!agents || agents.length === 0) && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                    Keine System-Agenten gefunden
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
