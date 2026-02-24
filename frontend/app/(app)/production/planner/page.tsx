"use client";

import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useJobs, useStations } from "@/hooks/api/use-jobs";
import type { JobResponse, StationResponse } from "@/types/api";
import { formatDate, daysUntil, humanizeStatus } from "@/lib/format";
import { PageHeader } from "@/components/shared/page-header";
import { SkeletonCard } from "@/components/shared/skeleton-variants";
import {
  DomainStatusBadge,
  getJobStatusVariant,
  getPriorityVariant,
} from "@/components/shared/domain-status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import {
  ChevronLeft,
  ChevronRight,
  Calendar,
  AlertCircle,
  RefreshCw,
  Factory,
} from "lucide-react";

function getWeekLabel(date: Date): string {
  const startOfYear = new Date(date.getFullYear(), 0, 1);
  const dayCount = Math.floor((date.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24));
  const weekNumber = Math.ceil((dayCount + startOfYear.getDay() + 1) / 7);
  return `CW ${weekNumber} / ${date.getFullYear()}`;
}

function getWeekRange(date: Date): { start: Date; end: Date } {
  const day = date.getDay();
  const diff = date.getDate() - day + (day === 0 ? -6 : 1);
  const start = new Date(date);
  start.setDate(diff);
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setDate(start.getDate() + 6);
  end.setHours(23, 59, 59, 999);
  return { start, end };
}

function getPriorityBorderColor(priority: number): string {
  if (priority >= 5) return "border-l-red-500";
  if (priority >= 4) return "border-l-orange-500";
  if (priority >= 3) return "border-l-amber-500";
  if (priority >= 2) return "border-l-blue-500";
  return "border-l-slate-500";
}

function getCapacityColor(percent: number): string {
  if (percent > 90) return "bg-red-500";
  if (percent > 70) return "bg-amber-500";
  return "bg-emerald-500";
}

export default function ProductionPlannerPage() {
  const router = useRouter();
  const { data: jobs, loading: jobsLoading, error: jobsError, refetch: refetchJobs } = useJobs();
  const { data: stations, loading: stationsLoading, error: stationsError, refetch: refetchStations } = useStations();

  const [weekOffset, setWeekOffset] = useState(0);

  // Current week
  const currentWeekDate = useMemo(() => {
    const d = new Date();
    d.setDate(d.getDate() + weekOffset * 7);
    return d;
  }, [weekOffset]);

  const weekRange = useMemo(() => getWeekRange(currentWeekDate), [currentWeekDate]);
  const weekLabel = useMemo(() => getWeekLabel(currentWeekDate), [currentWeekDate]);

  // Active jobs (not completed/cancelled) assigned to stations
  const activeJobs = useMemo(() => {
    if (!jobs) return [];
    return jobs.filter(
      (j) =>
        j.status !== "COMPLETED" &&
        j.status !== "CANCELLED" &&
        j.assignedStationId
    );
  }, [jobs]);

  // Group jobs by station
  const jobsByStation = useMemo(() => {
    const map = new Map<string, JobResponse[]>();
    for (const job of activeJobs) {
      if (!job.assignedStationId) continue;
      const existing = map.get(job.assignedStationId) ?? [];
      existing.push(job);
      map.set(job.assignedStationId, existing);
    }
    return map;
  }, [activeJobs]);

  // Unassigned jobs
  const unassignedJobs = useMemo(() => {
    if (!jobs) return [];
    return jobs.filter(
      (j) =>
        j.status !== "COMPLETED" &&
        j.status !== "CANCELLED" &&
        !j.assignedStationId
    );
  }, [jobs]);

  const loading = jobsLoading || stationsLoading;
  const error = jobsError || stationsError;

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <AlertCircle className="h-10 w-10 text-destructive/60" />
        <p className="text-sm text-muted-foreground">{error}</p>
        <Button
          variant="outline"
          size="sm"
          onClick={() => {
            refetchJobs();
            refetchStations();
          }}
          className="gap-2"
        >
          <RefreshCw className="h-3.5 w-3.5" />
          Retry
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* ─── Header ──────────────────────────────────────── */}
      <PageHeader
        title="Capacity Planner"
        description="Overview of station workloads and job assignments"
      />

      {/* ─── Week Navigation ─────────────────────────────── */}
      <div className="flex items-center gap-3">
        <Button variant="outline" size="icon-sm" onClick={() => setWeekOffset((p) => p - 1)}>
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <div className="flex items-center gap-2 rounded-md border bg-muted/30 px-4 py-1.5">
          <Calendar className="h-3.5 w-3.5 text-muted-foreground" />
          <span className="font-mono text-sm font-semibold">{weekLabel}</span>
          <span className="text-xs text-muted-foreground">
            ({formatDate(weekRange.start.toISOString())} - {formatDate(weekRange.end.toISOString())})
          </span>
        </div>
        <Button variant="outline" size="icon-sm" onClick={() => setWeekOffset((p) => p + 1)}>
          <ChevronRight className="h-4 w-4" />
        </Button>
        {weekOffset !== 0 && (
          <Button variant="ghost" size="sm" onClick={() => setWeekOffset(0)} className="text-xs">
            Today
          </Button>
        )}
      </div>

      {/* ─── Station Grid ────────────────────────────────── */}
      {loading ? (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <SkeletonCard key={i} className="h-48" />
          ))}
        </div>
      ) : stations && stations.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 xl:grid-cols-3">
          {stations.map((station) => {
            const stationJobs = jobsByStation.get(station.id) ?? [];
            const capacity = station.capacityPerShift ?? 10;
            const utilization = Math.min(Math.round((stationJobs.length / capacity) * 100), 100);

            return (
              <Card
                key={station.id}
                className="overflow-hidden transition-shadow hover:shadow-md cursor-pointer"
                onClick={() => router.push(`/production/stations/${station.id}`)}
              >
                {/* Station header */}
                <div className="border-b p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Factory className="h-4 w-4 text-muted-foreground" />
                      <h3 className="text-sm font-semibold">{station.name}</h3>
                    </div>
                    <DomainStatusBadge
                      variant={
                        station.status === "ACTIVE"
                          ? "success"
                          : station.status === "MAINTENANCE"
                            ? "warning"
                            : "neutral"
                      }
                    >
                      {station.status}
                    </DomainStatusBadge>
                  </div>
                  {station.description && (
                    <p className="mt-1 text-xs text-muted-foreground truncate">{station.description}</p>
                  )}

                  {/* Capacity bar */}
                  <div className="mt-3 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
                        Capacity
                      </span>
                      <span className="font-mono text-[11px] font-semibold">
                        {stationJobs.length}/{capacity} ({utilization}%)
                      </span>
                    </div>
                    <div className="relative h-2 w-full overflow-hidden rounded-full bg-muted">
                      <div
                        className={`h-full rounded-full transition-all ${getCapacityColor(utilization)}`}
                        style={{ width: `${utilization}%` }}
                      />
                    </div>
                  </div>
                </div>

                {/* Jobs list */}
                <div className="max-h-[200px] overflow-y-auto p-2">
                  {stationJobs.length === 0 ? (
                    <p className="py-4 text-center text-xs text-muted-foreground/60">
                      No jobs assigned
                    </p>
                  ) : (
                    <div className="space-y-1.5">
                      {stationJobs.map((job) => {
                        const days = daysUntil(job.deadline);
                        return (
                          <div
                            key={job.id}
                            className={`flex items-center gap-2 rounded-md border border-l-[3px] bg-background/50 px-3 py-2 transition-colors hover:bg-accent/30 cursor-pointer ${getPriorityBorderColor(job.priority)}`}
                            onClick={(e) => {
                              e.stopPropagation();
                              router.push(`/production/jobs/${job.id}`);
                            }}
                          >
                            <div className="min-w-0 flex-1">
                              <div className="flex items-center gap-2">
                                <span className="font-mono text-[11px] font-semibold text-foreground/80">
                                  {job.jobNumber}
                                </span>
                                <DomainStatusBadge variant={getJobStatusVariant(job.status)}>
                                  {humanizeStatus(job.status)}
                                </DomainStatusBadge>
                              </div>
                              <p className="mt-0.5 truncate text-xs text-muted-foreground">
                                {job.title}
                              </p>
                            </div>
                            {days !== null && (
                              <span
                                className={`shrink-0 font-mono text-[10px] font-semibold ${
                                  days < 0
                                    ? "text-red-500"
                                    : days < 3
                                      ? "text-amber-500"
                                      : "text-muted-foreground"
                                }`}
                              >
                                {days < 0 ? `${Math.abs(days)}d late` : `${days}d`}
                              </span>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </Card>
            );
          })}

          {/* Unassigned jobs card */}
          {unassignedJobs.length > 0 && (
            <Card className="overflow-hidden border-dashed">
              <div className="border-b p-4">
                <div className="flex items-center gap-2">
                  <AlertCircle className="h-4 w-4 text-amber-500" />
                  <h3 className="text-sm font-semibold">Unassigned</h3>
                  <span className="ml-auto font-mono text-xs text-muted-foreground">
                    {unassignedJobs.length} job(s)
                  </span>
                </div>
              </div>
              <div className="max-h-[200px] overflow-y-auto p-2">
                <div className="space-y-1.5">
                  {unassignedJobs.map((job) => (
                    <div
                      key={job.id}
                      className={`flex items-center gap-2 rounded-md border border-l-[3px] bg-background/50 px-3 py-2 transition-colors hover:bg-accent/30 cursor-pointer ${getPriorityBorderColor(job.priority)}`}
                      onClick={() => router.push(`/production/jobs/${job.id}`)}
                    >
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-[11px] font-semibold text-foreground/80">
                            {job.jobNumber}
                          </span>
                          <DomainStatusBadge variant={getPriorityVariant(job.priority)}>
                            P{job.priority}
                          </DomainStatusBadge>
                        </div>
                        <p className="mt-0.5 truncate text-xs text-muted-foreground">
                          {job.title}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </Card>
          )}
        </div>
      ) : (
        <Card className="flex flex-col items-center justify-center gap-3 py-16">
          <Factory className="h-10 w-10 text-muted-foreground/30" />
          <p className="text-sm font-medium text-muted-foreground">No stations configured</p>
          <p className="text-xs text-muted-foreground/60">
            Create stations to start planning production capacity.
          </p>
        </Card>
      )}
    </div>
  );
}
