"use client";

import { useState, useEffect, useCallback, useRef, useMemo } from "react";
import {
  Clock,
  Play,
  Square,
  Sparkles,
  Package,
  Wrench,
  UserX,
  Factory,
  Cog,
  Boxes,
  Inbox,
  AlertTriangle,
  CheckCircle2,
  ChevronDown,
  UserCircle,
} from "lucide-react";
import Link from "next/link";
import { toast } from "sonner";

import { useAuth } from "@/contexts/auth-context";
import { useApi } from "@/hooks/api/use-api";
import { usePrimaryAgent } from "@/hooks/use-primary-agent";
import { KpiCard } from "@/components/shared/kpi-card";
import {
  DomainStatusBadge,
  getJobStatusVariant,
} from "@/components/shared/domain-status-badge";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

import type {
  JobResponse,
  PageResponse,
  CriticalArticleResponse,
  MachineResponse,
  AbsenceResponse,
  ConversationResponse,
} from "@/types/api";
import { humanizeStatus } from "@/lib/format";

// ─── Timer Hook ─────────────────────────────────────────

function useTimer(isRunning: boolean, startTime: Date | null) {
  const [elapsed, setElapsed] = useState("00:00:00");
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (isRunning && startTime) {
      const update = () => {
        const diff = Date.now() - startTime.getTime();
        const hours = Math.floor(diff / 3600000);
        const minutes = Math.floor((diff % 3600000) / 60000);
        const seconds = Math.floor((diff % 60000) / 1000);
        setElapsed(
          `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`
        );
      };
      update();
      intervalRef.current = setInterval(update, 1000);
    } else {
      setElapsed("00:00:00");
    }
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [isRunning, startTime]);

  return elapsed;
}

// ─── Greeting Helper ────────────────────────────────────

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour >= 5 && hour < 11) return "Guten Morgen";
  if (hour >= 11 && hour < 17) return "Guten Tag";
  return "Guten Abend";
}

function getFormattedDate(): string {
  return new Date().toLocaleDateString("de-DE", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

// ─── Priority Color Helper ──────────────────────────────

function getPriorityBarColor(priority: number): string {
  if (priority >= 4) return "bg-red-500";
  if (priority >= 3) return "bg-amber-500";
  return "bg-emerald-500";
}

function getDeadlineText(deadline: string | null): { text: string; isOverdue: boolean } | null {
  if (!deadline) return null;
  const now = Date.now();
  const due = new Date(deadline).getTime();
  const diffMs = due - now;
  const diffHours = Math.round(diffMs / (1000 * 60 * 60));
  const diffDays = Math.round(diffMs / (1000 * 60 * 60 * 24));

  if (diffMs < 0) return { text: "Ueberfaellig!", isOverdue: true };
  if (diffHours < 24) return { text: `Faellig in ${diffHours}h`, isOverdue: false };
  return { text: `Faellig in ${diffDays}d`, isOverdue: false };
}

// ─── Page Component ─────────────────────────────────────

export default function MyDayPage() {
  const { user } = useAuth();
  const { agent } = usePrimaryAgent();

  const isWorkerOrLead = user?.role === "WORKER" || user?.role === "TEAM_LEAD";
  const isManagerOrAdmin = user?.role === "MANAGER" || user?.role === "ADMIN";

  // ── Clock-In/Out (local toggle) ──
  const [clockedIn, setClockedIn] = useState(false);
  const [clockInTime, setClockInTime] = useState<Date | null>(null);
  const elapsed = useTimer(clockedIn, clockInTime);

  // ── Collapsible completed jobs ──
  const [showCompleted, setShowCompleted] = useState(false);

  // ── Data fetching ──
  const { data: jobsPage, loading: jobsLoading } = useApi<PageResponse<JobResponse>>(
    "/api/jobs?status=IN_PRODUCTION,RELEASED&size=20"
  );
  const jobs = jobsPage?.content ?? [];

  const { data: completedJobsPage } = useApi<PageResponse<JobResponse>>(
    "/api/jobs?status=COMPLETED&size=10"
  );
  const completedJobs = completedJobsPage?.content ?? [];

  const { data: criticalArticles, loading: criticalLoading } =
    useApi<CriticalArticleResponse[]>("/api/stock/critical");

  const { data: machines, loading: machinesLoading } =
    useApi<MachineResponse[]>("/api/machines");

  const { data: conversationsPage, loading: conversationsLoading } =
    useApi<PageResponse<ConversationResponse>>("/api/conversations?size=1000");
  const conversations = conversationsPage?.content ?? [];

  const { data: absences, loading: absencesLoading } =
    useApi<AbsenceResponse[]>("/api/absences?status=APPROVED");

  // ── Derived counts ──
  const openJobsCount = jobs.length;
  const activeMachines = useMemo(
    () => machines?.filter((m) => m.status === "IN_USE" || m.status === "AVAILABLE").length ?? 0,
    [machines]
  );
  const criticalCount = criticalArticles?.length ?? 0;
  const openTickets = useMemo(
    () => conversations.filter((c) => c.status === "OPEN" || c.status === "IN_PROGRESS").length,
    [conversations]
  );

  // ── Clock handlers ──
  const handleClockIn = useCallback(() => {
    setClockedIn(true);
    setClockInTime(new Date());
    toast.success("Erfolgreich eingestempelt");
  }, []);

  const handleClockOut = useCallback(() => {
    setClockedIn(false);
    setClockInTime(null);
    toast.success("Erfolgreich ausgestempelt");
  }, []);

  return (
    <div className="space-y-6">
      {/* ═══ Bereich 1: Persoenlicher Status ═══ */}
      <Card className="relative overflow-hidden">
        <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/30 to-transparent" />
        <CardContent className="pt-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">
                {getGreeting()}, {user?.firstName ?? "Nutzer"}
              </h1>
              <p className="mt-1 text-sm text-muted-foreground">
                {getFormattedDate()}
              </p>
            </div>

            {/* Clock-In/Out fuer WORKER / TEAM_LEAD */}
            {!isManagerOrAdmin && (
              <div className="flex items-center gap-4">
                {clockedIn ? (
                  <>
                    <div className="text-right">
                      <DomainStatusBadge variant="success" pulse>
                        Anwesend seit{" "}
                        {clockInTime?.toLocaleTimeString("de-DE", {
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </DomainStatusBadge>
                      <p className="mt-1 font-mono text-3xl font-bold tracking-tight text-primary">
                        {elapsed}
                      </p>
                    </div>
                    <Button
                      variant="destructive"
                      size="lg"
                      className="gap-2"
                      onClick={handleClockOut}
                    >
                      <Square className="h-4 w-4" />
                      Ausstempeln
                    </Button>
                  </>
                ) : (
                  <>
                    <DomainStatusBadge variant="neutral">
                      Noch nicht eingestempelt
                    </DomainStatusBadge>
                    <Button
                      size="lg"
                      className="gap-2"
                      onClick={handleClockIn}
                    >
                      <Play className="h-4 w-4" />
                      Einstempeln
                    </Button>
                  </>
                )}
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* ═══ Profil-Hinweis (WORKER ohne Employee-Profil) ═══ */}
      {isWorkerOrLead && (
        <Card className="border-dashed border-amber-500/30 bg-amber-500/5">
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-amber-500/10">
              <UserCircle className="h-6 w-6 text-amber-500" />
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">
                Mitarbeiterprofil nicht verknuepft
              </p>
              <p className="text-xs text-muted-foreground">
                Dein Mitarbeiterprofil wurde noch nicht verknuepft. Bitte wende dich an deinen Administrator.
                Die Zeiterfassung funktioniert vorerst nur lokal.
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* ═══ Bereich 2: Aufgaben / KPI-Uebersicht ═══ */}
      {isManagerOrAdmin ? (
        <>
          {/* Manager/Admin: KPI Cards */}
          <div>
            <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Uebersicht
            </h2>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <KpiCard
                label="Offene Jobs"
                value={String(openJobsCount)}
                loading={jobsLoading}
              />
              <KpiCard
                label="Maschinen aktiv"
                value={String(activeMachines)}
                loading={machinesLoading}
              />
              <KpiCard
                label="Kritische Artikel"
                value={String(criticalCount)}
                loading={criticalLoading}
                trend={
                  criticalCount > 0
                    ? { direction: "down", value: `${criticalCount} unter Minimum` }
                    : undefined
                }
              />
              <KpiCard
                label="Offene Tickets"
                value={String(openTickets)}
                loading={conversationsLoading}
              />
            </div>
          </div>

          {/* Schnellzugriff */}
          <div>
            <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Schnellzugriff
            </h2>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {[
                { label: "Produktion", href: "/production", icon: Factory, color: "text-primary" },
                { label: "Maschinen", href: "/machines", icon: Cog, color: "text-amber-500" },
                { label: "Lager", href: "/inventory", icon: Boxes, color: "text-emerald-500" },
                { label: "Inbox", href: "/inbox", icon: Inbox, color: "text-blue-500" },
              ].map((item) => (
                <Link key={item.href} href={item.href}>
                  <Card className="group cursor-pointer transition-shadow duration-200 hover:shadow-md">
                    <CardContent className="flex items-center gap-3 pt-6">
                      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted transition-colors group-hover:bg-primary/10">
                        <item.icon className={`h-5 w-5 ${item.color}`} />
                      </div>
                      <span className="text-sm font-medium text-foreground">
                        {item.label}
                      </span>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          </div>
        </>
      ) : (
        <>
          {/* Worker/Team Lead: Jobs */}
          <div>
            <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Meine Aufgaben heute
            </h2>

            {jobsLoading ? (
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                {[1, 2, 3].map((i) => (
                  <Card key={i} className="relative overflow-hidden">
                    <CardContent className="space-y-3 pt-6">
                      <Skeleton className="h-4 w-24" />
                      <Skeleton className="h-5 w-40" />
                      <Skeleton className="h-3 w-20" />
                      <Skeleton className="h-8 w-32" />
                    </CardContent>
                  </Card>
                ))}
              </div>
            ) : jobs.length === 0 ? (
              <Card className="border-dashed">
                <CardContent className="flex flex-col items-center justify-center py-12">
                  <CheckCircle2 className="h-10 w-10 text-muted-foreground/40" />
                  <p className="mt-3 text-sm text-muted-foreground">
                    Keine offenen Auftraege vorhanden
                  </p>
                </CardContent>
              </Card>
            ) : (
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                {jobs.map((job) => {
                  const deadline = getDeadlineText(job.deadline);
                  return (
                    <Card key={job.id} className="relative overflow-hidden">
                      {/* Priority bar left */}
                      <div
                        className={`absolute inset-y-0 left-0 w-1 ${getPriorityBarColor(job.priority)}`}
                      />
                      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
                      <CardContent className="pl-5 pt-6">
                        <div className="flex items-start justify-between gap-2">
                          <div className="min-w-0">
                            <p className="font-mono text-xs font-semibold text-primary">
                              {job.jobNumber}
                            </p>
                            <p className="mt-1 truncate text-sm font-medium text-foreground">
                              {job.title}
                            </p>
                            <div className="mt-2 flex flex-wrap items-center gap-2">
                              <DomainStatusBadge
                                variant={getJobStatusVariant(job.status)}
                                pulse={job.status === "IN_PRODUCTION"}
                              >
                                {humanizeStatus(job.status)}
                              </DomainStatusBadge>
                              {deadline && (
                                <span
                                  className={`font-mono text-[11px] font-medium ${
                                    deadline.isOverdue
                                      ? "text-red-500"
                                      : "text-muted-foreground"
                                  }`}
                                >
                                  {deadline.isOverdue && (
                                    <AlertTriangle className="mr-0.5 inline h-3 w-3" />
                                  )}
                                  {deadline.text}
                                </span>
                              )}
                            </div>
                          </div>
                          <span className="font-mono text-[11px] text-muted-foreground">
                            x{job.quantity}
                          </span>
                        </div>
                        <div className="mt-4">
                          {job.status === "IN_PRODUCTION" ? (
                            <Button
                              variant="outline"
                              size="sm"
                              className="gap-1.5 border-amber-500/30 text-amber-600 hover:bg-amber-500/10 dark:text-amber-400"
                            >
                              <Square className="h-3 w-3" />
                              Auftrag beenden
                            </Button>
                          ) : (
                            <Button
                              variant="outline"
                              size="sm"
                              className="gap-1.5 border-primary/30 text-primary hover:bg-primary/10"
                            >
                              <Play className="h-3 w-3" />
                              Auftrag starten
                            </Button>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            )}

            {/* Collapsible completed jobs */}
            {completedJobs.length > 0 && (
              <div className="mt-4">
                <button
                  onClick={() => setShowCompleted(!showCompleted)}
                  className="flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
                >
                  <ChevronDown
                    className={`h-4 w-4 transition-transform ${showCompleted ? "rotate-180" : ""}`}
                  />
                  Erledigte Auftraege ({completedJobs.length})
                </button>
                {showCompleted && (
                  <div className="mt-3 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                    {completedJobs.map((job) => (
                      <Card key={job.id} className="relative overflow-hidden opacity-60">
                        <div className="absolute inset-y-0 left-0 w-1 bg-emerald-500" />
                        <CardContent className="pl-5 pt-6">
                          <p className="font-mono text-xs font-semibold text-muted-foreground">
                            {job.jobNumber}
                          </p>
                          <p className="mt-1 truncate text-sm font-medium text-foreground">
                            {job.title}
                          </p>
                          <div className="mt-2">
                            <DomainStatusBadge variant="success">
                              {humanizeStatus(job.status)}
                            </DomainStatusBadge>
                          </div>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </>
      )}

      {/* ═══ Bereich 3: Info-Cards ═══ */}
      <div>
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
          Aktuelle Informationen
        </h2>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {/* Kritische Artikel */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                <Package className="h-4 w-4" />
                Kritische Artikel
              </CardTitle>
            </CardHeader>
            <CardContent>
              {criticalLoading ? (
                <div className="space-y-2">
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-3/4" />
                </div>
              ) : criticalArticles && criticalArticles.length > 0 ? (
                <ul className="space-y-2">
                  {criticalArticles.slice(0, 5).map((item) => (
                    <li
                      key={`${item.articleId}-${item.warehouseLocationId}`}
                      className="flex items-center justify-between text-sm"
                    >
                      <span className="truncate text-foreground font-mono text-xs">
                        {item.articleId.slice(0, 8)}...
                      </span>
                      <span className="shrink-0 font-mono text-xs font-semibold text-red-500">
                        -{item.deficit}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                  Alle Bestaende im gruenen Bereich
                </div>
              )}
            </CardContent>
          </Card>

          {/* Faellige Wartungen */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                <Wrench className="h-4 w-4" />
                Faellige Wartungen
              </CardTitle>
            </CardHeader>
            <CardContent>
              {machinesLoading ? (
                <div className="space-y-2">
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-3/4" />
                </div>
              ) : (() => {
                const maintenanceMachines = machines?.filter(
                  (m) => m.status === "MAINTENANCE"
                ) ?? [];
                return maintenanceMachines.length > 0 ? (
                  <ul className="space-y-2">
                    {maintenanceMachines.slice(0, 5).map((m) => (
                      <li
                        key={m.id}
                        className="flex items-center justify-between text-sm"
                      >
                        <span className="truncate text-foreground">
                          {m.name}
                        </span>
                        <DomainStatusBadge variant="warning">
                          Wartung
                        </DomainStatusBadge>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                    Keine Wartungen faellig
                  </div>
                );
              })()}
            </CardContent>
          </Card>

          {/* Abwesenheiten heute */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                <UserX className="h-4 w-4" />
                Abwesenheiten heute
              </CardTitle>
            </CardHeader>
            <CardContent>
              {absencesLoading ? (
                <div className="space-y-2">
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-3/4" />
                </div>
              ) : absences && absences.length > 0 ? (
                <ul className="space-y-2">
                  {absences.slice(0, 5).map((a) => (
                    <li
                      key={a.id}
                      className="flex items-center justify-between text-sm"
                    >
                      <span className="truncate text-foreground font-mono text-xs">
                        {a.employeeId.slice(0, 8)}...
                      </span>
                      <DomainStatusBadge variant="warning">
                        {humanizeStatus(a.type)}
                      </DomainStatusBadge>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                  Alle anwesend
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* ═══ Bereich 4: Agent Suggestions ═══ */}
      <Card className="relative overflow-hidden bg-muted/30">
        <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/40 to-transparent" />
        <CardContent className="pt-6">
          <div className="flex items-start gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10">
              <Sparkles className="h-5 w-5 text-primary" />
            </div>
            <div className="min-w-0 flex-1">
              <h3 className="text-sm font-semibold text-foreground">
                Vorschlaege von {agent?.name ?? "Ihrem Agent"}
              </h3>
              <div className="mt-2 flex items-center gap-2">
                <span className="relative flex h-2 w-2">
                  <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-75" />
                  <span className="relative inline-flex h-2 w-2 rounded-full bg-primary" />
                </span>
                <p className="text-sm text-muted-foreground">
                  Ihr Agent analysiert gerade die aktuelle Situation...
                </p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
