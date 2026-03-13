"use client";

import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import {
  useSystemAgentDetail,
  useSystemAgentRuns,
  useSystemAgentMemories,
  useSystemAgentIncidents,
} from "@/hooks/api/use-system-agents";
import { PageHeader } from "@/components/shared/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ArrowLeft, Loader2 } from "lucide-react";

type Tab = "overview" | "runs" | "memory" | "incidents" | "config";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function SystemAgentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [tab, setTab] = useState<Tab>("overview");

  const { data: agent, loading } = useSystemAgentDetail(id);
  const { data: runsData } = useSystemAgentRuns(tab === "runs" ? id : null);
  const { data: memories } = useSystemAgentMemories(tab === "memory" ? id : null);
  const { data: incidents } = useSystemAgentIncidents(tab === "incidents" ? id : null);

  if (loading || !agent) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  const tabs: { key: Tab; label: string }[] = [
    { key: "overview", label: "Übersicht" },
    { key: "runs", label: "Runs" },
    { key: "memory", label: "Memory" },
    { key: "incidents", label: "Incidents" },
    { key: "config", label: "Konfiguration" },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => router.push("/system/agents")}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <PageHeader
          title={agent.name}
          description={`${agent.templateName} · ${agent.role || "—"}`}
        />
      </div>

      {/* Tab bar */}
      <div className="flex gap-1 border-b">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              tab === t.key
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Tab content */}
      {tab === "overview" && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          <InfoCard label="Status">
            <Badge variant={agent.status === "ACTIVE" ? "default" : "secondary"}>
              {agent.status === "ACTIVE" ? "Aktiv" : "Inaktiv"}
            </Badge>
          </InfoCard>
          <InfoCard label="Aktivität">
            <Badge
              variant="outline"
              className={
                agent.activityStatus === "BUSY"
                  ? "border-amber-500/50 text-amber-500"
                  : agent.activityStatus === "ERROR"
                    ? "border-destructive/50 text-destructive"
                    : "border-emerald-500/50 text-emerald-500"
              }
            >
              {agent.activityStatus}
            </Badge>
          </InfoCard>
          <InfoCard label="Model">
            <span className="font-mono text-sm">{agent.model}</span>
          </InfoCard>
          <InfoCard label="Tools">
            <span>{agent.allowedTools.length} Tools</span>
            {agent.hasToolOverride && (
              <Badge variant="outline" className="ml-2 text-xs">Override</Badge>
            )}
          </InfoCard>
          <InfoCard label="Token-Budget (täglich)">
            <span className="font-mono">{agent.dailyTokenBudget.toLocaleString("de-DE")}</span>
          </InfoCard>
          <InfoCard label="Max Tokens/Run">
            <span className="font-mono">{agent.maxTokensPerRun.toLocaleString("de-DE")}</span>
          </InfoCard>
          <InfoCard label="Offene Incidents">
            <span className={agent.openIncidents > 0 ? "text-destructive font-bold" : ""}>
              {agent.openIncidents}
            </span>
          </InfoCard>
          <InfoCard label="Letzte Aktivität">
            <span className="text-sm">{formatDate(agent.lastActivity)}</span>
          </InfoCard>
        </div>
      )}

      {tab === "runs" && (
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Status</TableHead>
                <TableHead>Trigger</TableHead>
                <TableHead>Tokens</TableHead>
                <TableHead>Kosten</TableHead>
                <TableHead>Gestartet</TableHead>
                <TableHead>Abgeschlossen</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {runsData?.content?.map((run) => (
                <TableRow key={run.id}>
                  <TableCell>
                    <Badge
                      variant={
                        run.status === "SUCCESS"
                          ? "default"
                          : run.status === "FAILED"
                            ? "destructive"
                            : "secondary"
                      }
                    >
                      {run.status}
                    </Badge>
                  </TableCell>
                  <TableCell className="font-mono text-xs">{run.triggerType}</TableCell>
                  <TableCell className="font-mono">{run.tokensUsed.toLocaleString("de-DE")}</TableCell>
                  <TableCell className="font-mono">
                    {run.costUsd != null ? `$${Number(run.costUsd).toFixed(4)}` : "—"}
                  </TableCell>
                  <TableCell className="text-xs">{formatDate(run.startedAt)}</TableCell>
                  <TableCell className="text-xs">{formatDate(run.completedAt)}</TableCell>
                </TableRow>
              ))}
              {(!runsData?.content || runsData.content.length === 0) && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                    Keine Runs vorhanden
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      )}

      {tab === "memory" && (
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Typ</TableHead>
                <TableHead>Kategorie</TableHead>
                <TableHead>Schlüssel</TableHead>
                <TableHead>Wert</TableHead>
                <TableHead>Wichtigkeit</TableHead>
                <TableHead>Erstellt</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {memories?.map((m) => (
                <TableRow key={m.id}>
                  <TableCell>
                    <Badge variant="outline" className="text-xs">{m.type}</Badge>
                  </TableCell>
                  <TableCell className="font-mono text-xs">{m.category}</TableCell>
                  <TableCell className="font-medium text-sm">{m.key}</TableCell>
                  <TableCell className="max-w-[300px] truncate text-sm text-muted-foreground">
                    {m.value}
                  </TableCell>
                  <TableCell className="font-mono">{m.importance}/10</TableCell>
                  <TableCell className="text-xs">{formatDate(m.createdAt)}</TableCell>
                </TableRow>
              ))}
              {(!memories || memories.length === 0) && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                    Keine Memories vorhanden
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      )}

      {tab === "incidents" && (
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Typ</TableHead>
                <TableHead>Beschreibung</TableHead>
                <TableHead>Erstellt</TableHead>
                <TableHead>Gelöst</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {incidents?.map((i) => (
                <TableRow key={i.id}>
                  <TableCell>
                    <Badge variant={i.resolvedAt ? "secondary" : "destructive"} className="text-xs">
                      {i.type}
                    </Badge>
                  </TableCell>
                  <TableCell className="max-w-[400px] truncate text-sm">{i.description}</TableCell>
                  <TableCell className="text-xs">{formatDate(i.createdAt)}</TableCell>
                  <TableCell className="text-xs">{formatDate(i.resolvedAt)}</TableCell>
                </TableRow>
              ))}
              {(!incidents || incidents.length === 0) && (
                <TableRow>
                  <TableCell colSpan={4} className="text-center text-muted-foreground py-8">
                    Keine Incidents vorhanden
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      )}

      {tab === "config" && (
        <div className="space-y-4">
          <div className="rounded-lg border bg-card p-4 space-y-3">
            <h3 className="text-sm font-semibold">System-Prompt</h3>
            <pre className="text-xs text-muted-foreground whitespace-pre-wrap max-h-60 overflow-y-auto rounded bg-muted p-3">
              {agent.customSystemPrompt || "(Template-Standard)"}
            </pre>
          </div>
          <div className="rounded-lg border bg-card p-4 space-y-3">
            <h3 className="text-sm font-semibold">Zugewiesene Tools ({agent.allowedTools.length})</h3>
            <div className="flex flex-wrap gap-1.5">
              {agent.allowedTools.map((t) => (
                <Badge key={t} variant="outline" className="text-xs font-mono">
                  {t}
                </Badge>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function InfoCard({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="rounded-lg border bg-card p-4">
      <div className="text-xs text-muted-foreground mb-1">{label}</div>
      <div className="flex items-center gap-2">{children}</div>
    </div>
  );
}
