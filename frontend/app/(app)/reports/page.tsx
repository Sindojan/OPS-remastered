"use client";

import { useState, useMemo, useCallback } from "react";
import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/shared/kpi-card";
import { SkeletonCard } from "@/components/shared/skeleton-variants";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  useJobs,
  useMachines,
  useArticles,
  useEmployees,
  useConversations,
  useCriticalArticles,
} from "@/hooks/api";
import type {
  JobResponse,
  JobStatus,
  MachineStatus,
  ConversationStatus,
} from "@/types/api";
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  Area,
  AreaChart,
} from "recharts";
import { Download, FileSpreadsheet } from "lucide-react";
import { toast } from "sonner";

// ── Color maps ──────────────────────────────────────────

const JOB_STATUS_COLORS: Record<JobStatus, string> = {
  DRAFT: "#94a3b8",
  RELEASED: "#3b82f6",
  IN_PRODUCTION: "#00D4B4",
  ON_HOLD: "#f59e0b",
  COMPLETED: "#22c55e",
  CANCELLED: "#ef4444",
};

const MACHINE_STATUS_COLORS: Record<MachineStatus, string> = {
  AVAILABLE: "#22c55e",
  IN_USE: "#00D4B4",
  MAINTENANCE: "#f59e0b",
  BLOCKED: "#ef4444",
  DECOMMISSIONED: "#6b7280",
};

const JOB_STATUS_LABELS: Record<JobStatus, string> = {
  DRAFT: "Entwurf",
  RELEASED: "Freigegeben",
  IN_PRODUCTION: "In Produktion",
  ON_HOLD: "Angehalten",
  COMPLETED: "Abgeschlossen",
  CANCELLED: "Storniert",
};

const MACHINE_STATUS_LABELS: Record<MachineStatus, string> = {
  AVAILABLE: "Verfuegbar",
  IN_USE: "In Betrieb",
  MAINTENANCE: "Wartung",
  BLOCKED: "Gesperrt",
  DECOMMISSIONED: "Ausgemustert",
};

type DateRange = "today" | "week" | "month";

// ── Custom Tooltip ──────────────────────────────────────

function ChartTooltip({
  active,
  payload,
  label,
}: {
  active?: boolean;
  payload?: Array<{ value: number; name: string; color: string }>;
  label?: string;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg border bg-card px-3 py-2 shadow-lg">
      {label && (
        <p className="mb-1 text-xs text-muted-foreground">{label}</p>
      )}
      {payload.map((entry, i) => (
        <div key={i} className="flex items-center gap-2">
          <div
            className="h-2 w-2 rounded-full"
            style={{ backgroundColor: entry.color }}
          />
          <span className="font-mono text-sm text-foreground">
            {entry.value}
          </span>
        </div>
      ))}
    </div>
  );
}

// ── Pie Label ───────────────────────────────────────────

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function renderPieLabel(props: any) {
  const { cx, cy, midAngle, outerRadius, value } = props;
  if (!value || value === 0) return null;
  const RADIAN = Math.PI / 180;
  const radius = (outerRadius ?? 90) + 20;
  const x = (cx ?? 0) + radius * Math.cos(-(midAngle ?? 0) * RADIAN);
  const y = (cy ?? 0) + radius * Math.sin(-(midAngle ?? 0) * RADIAN);
  return (
    <text
      x={x}
      y={y}
      fill="var(--muted-foreground)"
      textAnchor={x > (cx ?? 0) ? "start" : "end"}
      dominantBaseline="central"
      className="font-mono text-xs"
    >
      {value}
    </text>
  );
}

// ── CSV Export ───────────────────────────────────────────

function downloadCsv(filename: string, headers: string[], rows: string[][]) {
  const csvContent = [
    headers.join(","),
    ...rows.map((row) =>
      row.map((cell) => `"${(cell ?? "").replace(/"/g, '""')}"`).join(",")
    ),
  ].join("\n");

  const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

// ── Main Page ───────────────────────────────────────────

export default function ReportsPage() {
  const [dateRange, setDateRange] = useState<DateRange>("month");

  const { data: jobs, loading: jobsLoading } = useJobs();
  const { data: machines, loading: machinesLoading } = useMachines();
  const { data: articles, loading: articlesLoading } = useArticles();
  const { data: employees, loading: employeesLoading } = useEmployees();
  const { data: conversations, loading: conversationsLoading } =
    useConversations();
  const { data: criticalArticles, loading: criticalLoading } =
    useCriticalArticles();

  const isLoading =
    jobsLoading ||
    machinesLoading ||
    articlesLoading ||
    employeesLoading ||
    conversationsLoading ||
    criticalLoading;

  // ── KPI computations ────────────────────────────────

  const kpis = useMemo(() => {
    const jobList = jobs ?? [];
    const machineList = machines ?? [];
    const employeeList = employees ?? [];
    const conversationList = conversations ?? [];
    const criticalList = criticalArticles ?? [];
    const articleList = articles ?? [];

    const openJobs = jobList.filter(
      (j) => j.status !== "COMPLETED" && j.status !== "CANCELLED"
    ).length;

    const inProduction = jobList.filter(
      (j) => j.status === "IN_PRODUCTION"
    ).length;

    const completedJobs = jobList.filter(
      (j) => j.status === "COMPLETED"
    ).length;

    const totalMachines = machineList.length;
    const availableMachines = machineList.filter(
      (m) => m.status === "AVAILABLE"
    ).length;
    const utilization =
      totalMachines > 0
        ? Math.round((availableMachines / totalMachines) * 100)
        : 0;

    const criticalStock = criticalList.length;

    const openTickets = conversationList.filter(
      (c) => c.status === "OPEN" || c.status === "IN_PROGRESS"
    ).length;

    const activeEmployees = employeeList.filter(
      (e) => e.status === "ACTIVE"
    ).length;

    const totalArticles = articleList.length;

    return {
      openJobs,
      inProduction,
      completedJobs,
      utilization,
      criticalStock,
      openTickets,
      activeEmployees,
      totalArticles,
    };
  }, [jobs, machines, employees, conversations, criticalArticles, articles]);

  // ── Chart data: Jobs by status ──────────────────────

  const jobsByStatus = useMemo(() => {
    const jobList = jobs ?? [];
    const statusCounts: Record<string, number> = {};
    const allStatuses: JobStatus[] = [
      "DRAFT",
      "RELEASED",
      "IN_PRODUCTION",
      "ON_HOLD",
      "COMPLETED",
      "CANCELLED",
    ];

    allStatuses.forEach((s) => (statusCounts[s] = 0));
    jobList.forEach((j) => {
      statusCounts[j.status] = (statusCounts[j.status] || 0) + 1;
    });

    return allStatuses
      .map((status) => ({
        name: JOB_STATUS_LABELS[status],
        value: statusCounts[status],
        status,
      }))
      .filter((d) => d.value > 0);
  }, [jobs]);

  // ── Chart data: Jobs completed per day (30 days) ────

  const jobsCompletedPerDay = useMemo(() => {
    const jobList = jobs ?? [];
    const now = new Date();
    const days: { date: string; count: number }[] = [];

    for (let i = 29; i >= 0; i--) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      const dateStr = d.toISOString().split("T")[0];
      days.push({ date: dateStr, count: 0 });
    }

    jobList.forEach((j) => {
      if (j.completedAt) {
        const dateStr = new Date(j.completedAt).toISOString().split("T")[0];
        const day = days.find((d) => d.date === dateStr);
        if (day) day.count++;
      }
    });

    return days.map((d) => ({
      ...d,
      label: new Date(d.date).toLocaleDateString("de-DE", {
        day: "2-digit",
        month: "2-digit",
      }),
    }));
  }, [jobs]);

  // ── Chart data: Machine status distribution ─────────

  const machineStatusData = useMemo(() => {
    const machineList = machines ?? [];
    const allStatuses: MachineStatus[] = [
      "AVAILABLE",
      "IN_USE",
      "MAINTENANCE",
      "BLOCKED",
      "DECOMMISSIONED",
    ];
    const counts: Record<string, number> = {};
    allStatuses.forEach((s) => (counts[s] = 0));
    machineList.forEach((m) => {
      counts[m.status] = (counts[m.status] || 0) + 1;
    });

    return allStatuses
      .map((status) => ({
        name: MACHINE_STATUS_LABELS[status],
        count: counts[status],
        fill: MACHINE_STATUS_COLORS[status],
        status,
      }))
      .filter((d) => d.count > 0);
  }, [machines]);

  // ── CSV Export handlers ─────────────────────────────

  const exportJobsCsv = useCallback(() => {
    try {
      const jobList = jobs ?? [];
      const headers = [
        "Auftragsnummer",
        "Titel",
        "Status",
        "Prioritaet",
        "Menge",
        "Frist",
        "Erstellt am",
        "Abgeschlossen am",
      ];
      const rows = jobList.map((j) => [
        j.jobNumber,
        j.title,
        j.status,
        String(j.priority),
        String(j.quantity),
        j.deadline ?? "",
        j.createdAt,
        j.completedAt ?? "",
      ]);
      downloadCsv("jobs-report.csv", headers, rows);
      toast.success("CSV exportiert");
    } catch (err) {
      toast.error("Export fehlgeschlagen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [jobs]);

  const exportInventoryCsv = useCallback(() => {
    try {
      const articleList = articles ?? [];
      const headers = [
        "Artikelnummer",
        "Name",
        "Beschreibung",
        "Mindestbestand",
        "Nachbestellpunkt",
        "Status",
        "Erstellt am",
      ];
      const rows = articleList.map((a) => [
        a.articleNumber,
        a.name,
        a.description ?? "",
        String(a.minStock ?? ""),
        String(a.reorderPoint ?? ""),
        a.status,
        a.createdAt,
      ]);
      downloadCsv("inventory-report.csv", headers, rows);
      toast.success("CSV exportiert");
    } catch (err) {
      toast.error("Export fehlgeschlagen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [articles]);

  // ── Render ──────────────────────────────────────────

  const dateRangeButtons: { label: string; value: DateRange }[] = [
    { label: "Heute", value: "today" },
    { label: "Diese Woche", value: "week" },
    { label: "Dieser Monat", value: "month" },
  ];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Berichte"
        description="Ueberblick ueber die wichtigsten Betriebskennzahlen"
        actions={
          <div className="flex items-center gap-1 rounded-lg border bg-muted/30 p-1">
            {dateRangeButtons.map((btn) => (
              <Button
                key={btn.value}
                variant={dateRange === btn.value ? "default" : "ghost"}
                size="sm"
                onClick={() => setDateRange(btn.value)}
                className={
                  dateRange === btn.value
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-muted-foreground hover:text-foreground"
                }
              >
                {btn.label}
              </Button>
            ))}
          </div>
        }
      />

      {/* ── KPI Cards ─────────────────────────────────── */}

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <KpiCard
            label="Offene Auftraege"
            value={String(kpis.openJobs)}
            trend={
              kpis.openJobs > 0
                ? { direction: "up", value: `${kpis.openJobs} aktiv` }
                : undefined
            }
          />
          <KpiCard
            label="In Produktion"
            value={String(kpis.inProduction)}
            trend={
              kpis.inProduction > 0
                ? { direction: "up", value: "laufend" }
                : { direction: "neutral", value: "ruhend" }
            }
          />
          <KpiCard
            label="Abgeschlossene Auftraege"
            value={String(kpis.completedJobs)}
            trend={{ direction: "up", value: "gesamt" }}
          />
          <KpiCard
            label="Maschinenverfuegbarkeit"
            value={String(kpis.utilization)}
            unit="%"
            trend={
              kpis.utilization >= 70
                ? { direction: "up", value: "gut" }
                : { direction: "down", value: "niedrig" }
            }
          />
          <KpiCard
            label="Kritischer Bestand"
            value={String(kpis.criticalStock)}
            trend={
              kpis.criticalStock > 0
                ? { direction: "down", value: `${kpis.criticalStock} unter Minimum` }
                : { direction: "up", value: "alles gut" }
            }
          />
          <KpiCard
            label="Offene Tickets"
            value={String(kpis.openTickets)}
            trend={
              kpis.openTickets > 5
                ? { direction: "down", value: "hohe Last" }
                : { direction: "neutral", value: "normal" }
            }
          />
          <KpiCard
            label="Aktive Mitarbeiter"
            value={String(kpis.activeEmployees)}
            trend={{ direction: "neutral", value: "im Dienst" }}
          />
          <KpiCard
            label="Artikel gesamt"
            value={String(kpis.totalArticles)}
            trend={{ direction: "neutral", value: "im Katalog" }}
          />
        </div>
      )}

      {/* ── Charts ────────────────────────────────────── */}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Chart 1: Jobs by Status (Donut) */}
        <Card className="py-0">
          <CardHeader className="pb-0 pt-5">
            <CardTitle className="text-sm font-semibold tracking-tight">
              Auftraege nach Status
            </CardTitle>
          </CardHeader>
          <CardContent className="pb-5">
            <div className="h-[280px] w-full">
              {jobsByStatus.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={jobsByStatus}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={90}
                      paddingAngle={2}
                      dataKey="value"
                      label={renderPieLabel}
                      stroke="none"
                    >
                      {jobsByStatus.map((entry) => (
                        <Cell
                          key={entry.status}
                          fill={JOB_STATUS_COLORS[entry.status]}
                        />
                      ))}
                    </Pie>
                    <Tooltip
                      content={({ active, payload, label }) => (
                        <ChartTooltip
                          active={active}
                          payload={payload as Array<{ value: number; name: string; color: string }>}
                          label={label as string}
                        />
                      )}
                    />
                    <Legend
                      verticalAlign="bottom"
                      height={36}
                      iconSize={8}
                      iconType="circle"
                      formatter={(value: string) => (
                        <span className="text-xs text-muted-foreground">
                          {value}
                        </span>
                      )}
                    />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="flex h-full items-center justify-center">
                  <p className="text-sm text-muted-foreground">Keine Auftragsdaten</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Chart 2: Jobs Completed (30 Days) */}
        <Card className="py-0">
          <CardHeader className="pb-0 pt-5">
            <CardTitle className="text-sm font-semibold tracking-tight">
              Abgeschlossene Auftraege (30 Tage)
            </CardTitle>
          </CardHeader>
          <CardContent className="pb-5">
            <div className="h-[280px] w-full">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart
                  data={jobsCompletedPerDay}
                  margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                >
                  <defs>
                    <linearGradient
                      id="completedGradient"
                      x1="0"
                      y1="0"
                      x2="0"
                      y2="1"
                    >
                      <stop offset="5%" stopColor="#00D4B4" stopOpacity={0.3} />
                      <stop
                        offset="95%"
                        stopColor="#00D4B4"
                        stopOpacity={0.0}
                      />
                    </linearGradient>
                  </defs>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="var(--border)"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="label"
                    tick={{ fill: "var(--muted-foreground)", fontSize: 10 }}
                    tickLine={false}
                    axisLine={false}
                    interval="preserveStartEnd"
                  />
                  <YAxis
                    tick={{ fill: "var(--muted-foreground)", fontSize: 10 }}
                    tickLine={false}
                    axisLine={false}
                    allowDecimals={false}
                  />
                  <Tooltip
                    content={({ active, payload, label }) => (
                      <ChartTooltip
                        active={active}
                        payload={payload as Array<{ value: number; name: string; color: string }>}
                        label={label as string}
                      />
                    )}
                  />
                  <Area
                    type="monotone"
                    dataKey="count"
                    stroke="#00D4B4"
                    strokeWidth={2}
                    fill="url(#completedGradient)"
                    dot={false}
                    activeDot={{
                      r: 4,
                      fill: "#00D4B4",
                      stroke: "var(--card)",
                      strokeWidth: 2,
                    }}
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        {/* Chart 3: Machine Status Distribution */}
        <Card className="py-0">
          <CardHeader className="pb-0 pt-5">
            <CardTitle className="text-sm font-semibold tracking-tight">
              Maschinenstatus
            </CardTitle>
          </CardHeader>
          <CardContent className="pb-5">
            <div className="h-[280px] w-full">
              {machineStatusData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={machineStatusData}
                    layout="vertical"
                    margin={{ top: 10, right: 30, left: 10, bottom: 0 }}
                  >
                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke="var(--border)"
                      horizontal={false}
                    />
                    <XAxis
                      type="number"
                      tick={{
                        fill: "var(--muted-foreground)",
                        fontSize: 10,
                      }}
                      tickLine={false}
                      axisLine={false}
                      allowDecimals={false}
                    />
                    <YAxis
                      type="category"
                      dataKey="name"
                      tick={{
                        fill: "var(--muted-foreground)",
                        fontSize: 11,
                      }}
                      tickLine={false}
                      axisLine={false}
                      width={100}
                    />
                    <Tooltip
                      content={({ active, payload, label }) => (
                        <ChartTooltip
                          active={active}
                          payload={payload as Array<{ value: number; name: string; color: string }>}
                          label={label as string}
                        />
                      )}
                    />
                    <Bar
                      dataKey="count"
                      radius={[0, 4, 4, 0]}
                      barSize={20}
                    >
                      {machineStatusData.map((entry) => (
                        <Cell
                          key={entry.status}
                          fill={MACHINE_STATUS_COLORS[entry.status]}
                        />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="flex h-full items-center justify-center">
                  <p className="text-sm text-muted-foreground">
                    Keine Maschinendaten
                  </p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* ── Export Section ─────────────────────────────── */}

      <Card className="py-0">
        <CardHeader className="pb-0 pt-5">
          <CardTitle className="text-sm font-semibold tracking-tight">
            Daten exportieren
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-3 pb-5">
          <Button
            variant="outline"
            size="sm"
            onClick={exportJobsCsv}
            disabled={!jobs || jobs.length === 0}
            className="gap-2"
          >
            <Download className="h-4 w-4" />
            Auftraege CSV exportieren
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={exportInventoryCsv}
            disabled={!articles || articles.length === 0}
            className="gap-2"
          >
            <FileSpreadsheet className="h-4 w-4" />
            Lager CSV exportieren
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
