"use client";

import { useMemo } from "react";
import { useAuth } from "@/contexts/auth-context";
import { useBudgetOverview } from "@/hooks/api/use-settings";
import { KpiCard } from "@/components/shared/kpi-card";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from "recharts";
import { DollarSign, Coins, Play, Info } from "lucide-react";
import type { AgentBudget } from "@/types/api";

function formatUsd(value: number): string {
  return `$${value.toFixed(2)}`;
}

function formatTokens(value: number): string {
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(1)}K`;
  return value.toString();
}

function formatDateShort(dateStr: string): string {
  const d = new Date(dateStr);
  return `${d.getDate()}.${d.getMonth() + 1}.`;
}

export function BudgetTab() {
  const { user } = useAuth();
  const { data: budget, loading } = useBudgetOverview();

  const agentRows: (AgentBudget & { id: string })[] = useMemo(() => {
    if (!budget?.currentMonth?.byAgent) return [];
    return budget.currentMonth.byAgent.map((a, i) => ({ ...a, id: `agent-${i}` }));
  }, [budget]);

  const chartData = useMemo(() => {
    if (!budget?.dailyUsage) return [];
    return budget.dailyUsage.map((d) => ({
      ...d,
      dateLabel: formatDateShort(d.date),
    }));
  }, [budget]);

  const totalRow = useMemo(() => {
    if (!budget?.currentMonth) return { runs: 0, tokens: 0, cost: 0 };
    return {
      runs: budget.currentMonth.totalRuns,
      tokens: budget.currentMonth.totalTokens,
      cost: budget.currentMonth.totalCostUsd,
    };
  }, [budget]);

  if (user?.role !== "ADMIN" && user?.role !== "MANAGER" && user?.role !== "SYSTEM_ADMIN") {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <DollarSign className="h-8 w-8 text-muted-foreground/40" />
        <p className="mt-2 text-sm text-muted-foreground">Nur Administratoren und Manager können das Budget einsehen.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 sm:grid-cols-3">
          <Skeleton className="h-28" />
          <Skeleton className="h-28" />
          <Skeleton className="h-28" />
        </div>
        <Skeleton className="h-64" />
        <Skeleton className="h-48" />
      </div>
    );
  }

  const columns: ColumnDef<AgentBudget & { id: string }>[] = [
    {
      id: "agentName",
      header: "Agent-Name",
      accessorKey: "agentName",
      cell: (row) => <span className="text-sm font-medium">{row.agentName}</span>,
    },
    {
      id: "runs",
      header: "Runs",
      accessorKey: "runs",
      cell: (row) => <span className="font-mono text-xs">{row.runs}</span>,
    },
    {
      id: "tokens",
      header: "Tokens",
      accessorKey: "tokens",
      cell: (row) => <span className="font-mono text-xs">{formatTokens(row.tokens)}</span>,
      sortFn: (a, b) => a.tokens - b.tokens,
    },
    {
      id: "cost",
      header: "Kosten (USD)",
      accessorKey: "costUsd",
      cell: (row) => <span className="font-mono text-xs font-medium">{formatUsd(row.costUsd)}</span>,
      sortFn: (a, b) => a.costUsd - b.costUsd,
    },
  ];

  return (
    <div className="space-y-6">
      {/* KPI Cards */}
      <div className="grid gap-4 sm:grid-cols-3">
        <KpiCard
          label="Kosten aktueller Monat"
          value={formatUsd(totalRow.cost)}
          sparkline={chartData.length > 0 ? { data: chartData.map((d) => d.costUsd) } : undefined}
        />
        <KpiCard
          label="Tokens aktueller Monat"
          value={formatTokens(totalRow.tokens)}
        />
        <KpiCard
          label="Runs aktueller Monat"
          value={totalRow.runs.toString()}
        />
      </div>

      {/* Chart */}
      {chartData.length > 0 && (
        <Card className="border-border/50">
          <CardContent className="pt-6">
            <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground mb-4">
              Kosten letzte 30 Tage (USD)
            </p>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--muted))" opacity={0.3} />
                  <XAxis
                    dataKey="dateLabel"
                    tick={{ fontSize: 10, fill: "hsl(var(--muted-foreground))" }}
                    tickLine={false}
                    axisLine={false}
                  />
                  <YAxis
                    tick={{ fontSize: 10, fill: "hsl(var(--muted-foreground))" }}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(v) => `$${v}`}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "hsl(var(--card))",
                      border: "1px solid hsl(var(--border))",
                      borderRadius: "8px",
                      fontSize: "12px",
                    }}
                    formatter={(value, name) => {
                      const numVal = typeof value === "number" ? value : 0;
                      if (name === "costUsd") return [formatUsd(numVal), "Kosten"];
                      return [numVal, name];
                    }}
                    labelFormatter={(label) => `Datum: ${label}`}
                  />
                  <Line
                    type="monotone"
                    dataKey="costUsd"
                    stroke="hsl(var(--primary))"
                    strokeWidth={2}
                    dot={false}
                    activeDot={{ r: 4, stroke: "hsl(var(--primary))", strokeWidth: 2 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Agent Table */}
      <DataTable<AgentBudget & { id: string }>
        data={agentRows}
        columns={columns}
        searchKey="agentName"
        searchPlaceholder="Agent suchen..."
        emptyState={{
          icon: <Coins className="h-8 w-8 text-muted-foreground/40" />,
          title: "Keine Budget-Daten",
          description: "Budget-Daten erscheinen hier, sobald Agenten genutzt werden.",
        }}
      />

      {/* Total */}
      {agentRows.length > 0 && (
        <div className="flex items-center justify-end gap-6 rounded-md border border-border/50 bg-muted/30 px-4 py-3">
          <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Gesamt</span>
          <span className="font-mono text-xs">{totalRow.runs} Runs</span>
          <span className="font-mono text-xs">{formatTokens(totalRow.tokens)} Tokens</span>
          <span className="font-mono text-sm font-bold">{formatUsd(totalRow.cost)}</span>
        </div>
      )}

      {/* Info */}
      <div className="flex items-start gap-2 rounded-md border border-border/50 bg-muted/20 px-4 py-3">
        <Info className="h-4 w-4 text-muted-foreground shrink-0 mt-0.5" />
        <p className="text-xs text-muted-foreground">
          Kosten werden in USD angezeigt (Anthropic API Preise).
        </p>
      </div>
    </div>
  );
}
