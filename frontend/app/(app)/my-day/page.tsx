"use client";

import { useState, useEffect, useCallback, useRef, useMemo } from "react";
import {
  Clock,
  Play,
  Square,
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
import { useMyDay, usePeopleMutations } from "@/hooks/api/use-people";
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

  if (diffMs < 0) return { text: "Überfällig!", isOverdue: true };
  if (diffHours < 24) return { text: `Fällig in ${diffHours}h`, isOverdue: false };
  return { text: `Fällig in ${diffDays}d`, isOverdue: false };
}

// ─── Module check helper ────────────────────────────────

function hasModule(enabledModules: string[] | undefined, moduleId: string): boolean {
  return enabledModules?.includes(moduleId) ?? false;
}

// ─── Page Component ─────────────────────────────────────

export default function MyDayPage() {
  const { user } = useAuth();

  const isWorkerOrLead = user?.role === "WORKER" || user?.role === "TEAM_LEAD";
  const isManagerOrAdmin = user?.role === "MANAGER" || user?.role === "ADMIN";
  const enabledModules = user?.enabledModules;

  const employeeId = user?.employeeId ?? null;

  // ── My-Day data from backend ──
  const { data: myDay, refetch: refetchMyDay } = useMyDay(employeeId);
  const { clockIn, clockOut, jobStart, jobEnd, loading: mutating } = usePeopleMutations();

  // ── Clock-In state derived from backend ──
  const clockedIn = myDay?.clockedIn ?? false;
  const clockInTime = useMemo(() => {
    if (!myDay?.clockedIn || !myDay.entries) return null;
    // Find the last CLOCK_IN entry
    const lastClockIn = [...myDay.entries]
      .reverse()
      .find((e) => e.type === "CLOCK_IN");
    return lastClockIn ? new Date(lastClockIn.timestamp) : null;
  }, [myDay]);
  const elapsed = useTimer(clockedIn, clockInTime);

  // ── Collapsible completed jobs ──
  const [showCompleted, setShowCompleted] = useState(false);

  // ── Data fetching (conditionally based on modules) ──
  const showProduction = hasModule(enabledModules, "production");
  const showMachines = hasModule(enabledModules, "machines");
  const showInventory = hasModule(enabledModules, "inventory");
  const showInbox = hasModule(enabledModules, "inbox");
  const showPeople = hasModule(enabledModules, "people");

  const { data: jobsPage, loading: jobsLoading } = useApi<PageResponse<JobResponse>>(
    showProduction ? "/api/jobs?status=IN_PRODUCTION,RELEASED&size=20" : null
  );
  const jobs = jobsPage?.content ?? [];

  const { data: completedJobsPage } = useApi<PageResponse<JobResponse>>(
    showProduction ? "/api/jobs?status=COMPLETED&size=10" : null
  );
  const completedJobs = completedJobsPage?.content ?? [];

  const { data: criticalArticles, loading: criticalLoading } =
    useApi<CriticalArticleResponse[]>(showInventory ? "/api/stock/critical" : null);

  const { data: machines, loading: machinesLoading } =
    useApi<MachineResponse[]>(showMachines ? "/api/machines" : null);

  const { data: conversationsPage, loading: conversationsLoading } =
    useApi<PageResponse<ConversationResponse>>(showInbox ? "/api/conversations?size=1" : null);

  const { data: absences, loading: absencesLoading } =
    useApi<AbsenceResponse[]>(showPeople ? "/api/absences?status=APPROVED" : null);

  // ── Derived counts ──
  const openJobsCount = jobs.length;
  const activeMachines = useMemo(
    () => machines?.filter((m) => m.status === "IN_USE" || m.status === "AVAILABLE").length ?? 0,
    [machines]
  );
  const criticalCount = criticalArticles?.length ?? 0;
  const openTickets = conversationsPage?.totalElements ?? 0;

  // ── Clock handlers ──
  const handleClockIn = useCallback(async () => {
    if (!employeeId) return;
    try {
      await clockIn(employeeId);
      toast.success("Erfolgreich eingestempelt");
      refetchMyDay();
    } catch {
      toast.error("Einstempeln fehlgeschlagen");
    }
  }, [employeeId, clockIn, refetchMyDay]);

  const handleClockOut = useCallback(async () => {
    if (!employeeId) return;
    try {
      await clockOut(employeeId);
      toast.success("Erfolgreich ausgestempelt");
      refetchMyDay();
    } catch {
      toast.error("Ausstempeln fehlgeschlagen");
    }
  }, [employeeId, clockOut, refetchMyDay]);

  // ── Job Start/End handlers ──
  const [jobActionLoading, setJobActionLoading] = useState<string | null>(null);

  const handleJobStart = useCallback(async (jobId: string) => {
    if (!employeeId) return;
    setJobActionLoading(jobId);
    try {
      await jobStart(employeeId, jobId);
      toast.success("Auftrag gestartet");
      refetchMyDay();
    } catch {
      toast.error("Auftrag starten fehlgeschlagen");
    } finally {
      setJobActionLoading(null);
    }
  }, [employeeId, jobStart, refetchMyDay]);

  const handleJobEnd = useCallback(async (jobId: string) => {
    if (!employeeId) return;
    setJobActionLoading(jobId);
    try {
      await jobEnd(employeeId, jobId);
      toast.success("Auftrag beendet");
      refetchMyDay();
    } catch {
      toast.error("Auftrag beenden fehlgeschlagen");
    } finally {
      setJobActionLoading(null);
    }
  }, [employeeId, jobEnd, refetchMyDay]);

  // ── Quick access links (filtered by modules) ──
  const quickAccessLinks = useMemo(() => {
    const links = [];
    if (showProduction) links.push({ label: "Produktion", href: "/production", icon: Factory, color: "text-primary" });
    if (showMachines) links.push({ label: "Maschinen", href: "/machines", icon: Cog, color: "text-amber-500" });
    if (showInventory) links.push({ label: "Lager", href: "/inventory", icon: Boxes, color: "text-emerald-500" });
    if (showInbox) links.push({ label: "Inbox", href: "/inbox", icon: Inbox, color: "text-blue-500" });
    return links;
  }, [showProduction, showMachines, showInventory, showInbox]);

  return (
    <div className="space-y-6">
      {/* ═══ Bereich 1: Persönlicher Status ═══ */}
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

            {/* Clock-In/Out für WORKER / TEAM_LEAD */}
            {!isManagerOrAdmin && employeeId && (
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
                      disabled={mutating}
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
                      disabled={mutating}
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
      {isWorkerOrLead && !employeeId && (
        <Card className="border-dashed border-amber-500/30 bg-amber-500/5">
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-amber-500/10">
              <UserCircle className="h-6 w-6 text-amber-500" />
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">
                Mitarbeiterprofil nicht verknüpft
              </p>
              <p className="text-xs text-muted-foreground">
                Dein Mitarbeiterprofil wurde noch nicht verknüpft. Bitte wende dich an deinen Administrator.
                Die Zeiterfassung ist nicht verfügbar.
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* ═══ Bereich 2: Aufgaben / KPI-Übersicht ═══ */}
      {isManagerOrAdmin ? (
        <>
          {/* Manager/Admin: KPI Cards */}
          <div>
            <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Übersicht
            </h2>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {showProduction && (
                <KpiCard
                  label="Offene Jobs"
                  value={String(openJobsCount)}
                  loading={jobsLoading}
                />
              )}
              {showMachines && (
                <KpiCard
                  label="Maschinen aktiv"
                  value={String(activeMachines)}
                  loading={machinesLoading}
                />
              )}
              {showInventory && (
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
              )}
              {showInbox && (
                <KpiCard
                  label="Offene Tickets"
                  value={String(openTickets)}
                  loading={conversationsLoading}
                />
              )}
            </div>
          </div>

          {/* Schnellzugriff */}
          {quickAccessLinks.length > 0 && (
            <div>
              <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Schnellzugriff
              </h2>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                {quickAccessLinks.map((item) => (
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
          )}
        </>
      ) : showProduction ? (
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
                    Keine offenen Aufträge vorhanden
                  </p>
                </CardContent>
              </Card>
            ) : (
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                {jobs.map((job) => {
                  const deadline = getDeadlineText(job.deadline);
                  return (
                    <Link key={job.id} href={`/production/jobs/${job.id}`}>
                      <Card className="relative overflow-hidden cursor-pointer transition-shadow duration-200 hover:shadow-md">
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
                          {employeeId && (
                            <div className="mt-4" onClick={(e) => e.preventDefault()}>
                              {job.status === "IN_PRODUCTION" ? (
                                <Button
                                  variant="outline"
                                  size="sm"
                                  className="gap-1.5 border-amber-500/30 text-amber-600 hover:bg-amber-500/10 dark:text-amber-400"
                                  disabled={jobActionLoading === job.id}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleJobEnd(job.id);
                                  }}
                                >
                                  <Square className="h-3 w-3" />
                                  Auftrag beenden
                                </Button>
                              ) : (
                                <Button
                                  variant="outline"
                                  size="sm"
                                  className="gap-1.5 border-primary/30 text-primary hover:bg-primary/10"
                                  disabled={jobActionLoading === job.id}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleJobStart(job.id);
                                  }}
                                >
                                  <Play className="h-3 w-3" />
                                  Auftrag starten
                                </Button>
                              )}
                            </div>
                          )}
                        </CardContent>
                      </Card>
                    </Link>
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
                  Erledigte Aufträge ({completedJobs.length})
                </button>
                {showCompleted && (
                  <div className="mt-3 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                    {completedJobs.map((job) => (
                      <Link key={job.id} href={`/production/jobs/${job.id}`}>
                        <Card className="relative overflow-hidden opacity-60 cursor-pointer hover:opacity-80 transition-opacity">
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
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </>
      ) : null}

      {/* ═══ Bereich 3: Info-Cards ═══ */}
      {(showInventory || showMachines || showPeople) && (
        <div>
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
            Aktuelle Informationen
          </h2>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            {/* Kritische Artikel */}
            {showInventory && (
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
                          <span className="truncate text-foreground text-xs">
                            {item.articleName ?? item.articleNumber ?? item.articleId.slice(0, 8)}
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
                      Alle Bestände im grünen Bereich
                    </div>
                  )}
                </CardContent>
              </Card>
            )}

            {/* Fällige Wartungen */}
            {showMachines && (
              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                    <Wrench className="h-4 w-4" />
                    Fällige Wartungen
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
                        Keine Wartungen fällig
                      </div>
                    );
                  })()}
                </CardContent>
              </Card>
            )}

            {/* Abwesenheiten heute */}
            {showPeople && (
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
                          <span className="truncate text-foreground text-xs">
                            {a.employeeFirstName && a.employeeLastName
                              ? `${a.employeeFirstName} ${a.employeeLastName}`
                              : a.employeeId.slice(0, 8)}
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
            )}
          </div>
        </div>
      )}
    </div>
  );
}
