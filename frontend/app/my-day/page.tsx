"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import {
  Clock,
  Play,
  Square,
  Briefcase,
  Package,
  Wrench,
  UserCircle,
} from "lucide-react";

import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

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

// ─── Component ──────────────────────────────────────────

export default function MyDayPage() {
  const [clockedIn, setClockedIn] = useState(false);
  const [clockInTime, setClockInTime] = useState<Date | null>(null);
  const elapsed = useTimer(clockedIn, clockInTime);

  const handleClockIn = useCallback(() => {
    try {
      setClockedIn(true);
      setClockInTime(new Date());
      toast.success("Clocked in successfully");
    } catch (err) {
      toast.error("Failed to clock in", { description: err instanceof Error ? err.message : "Unknown error" });
    }
  }, []);

  const handleClockOut = useCallback(() => {
    try {
      setClockedIn(false);
      setClockInTime(null);
      toast.success("Clocked out successfully");
    } catch (err) {
      toast.error("Failed to clock out", { description: err instanceof Error ? err.message : "Unknown error" });
    }
  }, []);

  return (
    <div className="space-y-6">
      <PageHeader
        title="My Day"
        description="Your daily dashboard and time tracking"
      />

      {/* ═══ Shift & Clock Section ═══ */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {/* Shift Info */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              <Clock className="h-4 w-4" />
              Current Shift
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-muted">
                <Clock className="h-6 w-6 text-muted-foreground" />
              </div>
              <div>
                <p className="text-sm font-medium text-foreground">
                  Default Shift
                </p>
                <p className="font-mono text-xs text-muted-foreground">
                  06:00 – 14:00
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Clock In/Out */}
        <Card
          className={
            clockedIn
              ? "border-primary/30 bg-primary/5"
              : ""
          }
        >
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              <Clock className="h-4 w-4" />
              Time Tracking
            </CardTitle>
          </CardHeader>
          <CardContent>
            {clockedIn ? (
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-foreground">
                    Clocked In
                  </p>
                  <p className="mt-1 font-mono text-3xl font-bold tracking-tight text-primary">
                    {elapsed}
                  </p>
                  <p className="mt-0.5 font-mono text-[11px] text-muted-foreground">
                    since{" "}
                    {clockInTime?.toLocaleTimeString("de-DE", {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </p>
                </div>
                <Button
                  variant="destructive"
                  size="lg"
                  className="gap-2"
                  onClick={handleClockOut}
                >
                  <Square className="h-4 w-4" />
                  Clock Out
                </Button>
              </div>
            ) : (
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground">
                    Not clocked in
                  </p>
                  <p className="mt-0.5 font-mono text-xs text-muted-foreground">
                    Press Clock In to start your day
                  </p>
                </div>
                <Button
                  size="lg"
                  className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
                  onClick={handleClockIn}
                >
                  <Play className="h-4 w-4" />
                  Clock In
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* ═══ My Jobs Today ═══ */}
      <div>
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
          My Jobs Today
        </h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {/* Placeholder Job Cards */}
          {[
            {
              number: "JOB-001",
              title: "Front Seat Assembly",
              status: "IN_PRODUCTION",
            },
            {
              number: "JOB-002",
              title: "Leather Cutting - Batch 14",
              status: "RELEASED",
            },
            {
              number: "JOB-003",
              title: "Quality Check - Headrests",
              status: "RELEASED",
            },
          ].map((job) => (
            <Card key={job.number} className="relative overflow-hidden">
              <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
              <CardContent className="pt-6">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="font-mono text-xs font-semibold text-primary">
                      {job.number}
                    </p>
                    <p className="mt-1 text-sm font-medium text-foreground">
                      {job.title}
                    </p>
                    <p className="mt-0.5 font-mono text-[11px] text-muted-foreground">
                      Status: {job.status.replace(/_/g, " ")}
                    </p>
                  </div>
                  <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-muted">
                    <Briefcase className="h-4 w-4 text-muted-foreground" />
                  </div>
                </div>
                <div className="mt-4 flex gap-2">
                  {job.status === "IN_PRODUCTION" ? (
                    <Button
                      variant="outline"
                      size="sm"
                      className="gap-1.5 border-amber-500/30 text-amber-600 hover:bg-amber-500/10 dark:text-amber-400"
                    >
                      <Square className="h-3 w-3" />
                      End Job
                    </Button>
                  ) : (
                    <Button
                      variant="outline"
                      size="sm"
                      className="gap-1.5 border-primary/30 text-primary hover:bg-primary/10"
                    >
                      <Play className="h-3 w-3" />
                      Start Job
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>

      {/* ═══ Status Cards ═══ */}
      <div>
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-muted-foreground">
          Status Overview
        </h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {/* Material Availability */}
          <Card>
            <CardContent className="flex items-center gap-4 pt-6">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-emerald-500/10">
                <Package className="h-6 w-6 text-emerald-500" />
              </div>
              <div>
                <p className="text-sm font-medium text-foreground">
                  Material Availability
                </p>
                <p className="font-mono text-xs text-muted-foreground">
                  All materials available for today&apos;s jobs
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Maintenance Due */}
          <Card>
            <CardContent className="flex items-center gap-4 pt-6">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-amber-500/10">
                <Wrench className="h-6 w-6 text-amber-500" />
              </div>
              <div>
                <p className="text-sm font-medium text-foreground">
                  Maintenance Due
                </p>
                <p className="font-mono text-xs text-muted-foreground">
                  No maintenance tasks scheduled for today
                </p>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* ═══ Employee Profile Link ═══ */}
      <Card className="border-dashed">
        <CardContent className="flex items-center gap-4 pt-6">
          <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-muted">
            <UserCircle className="h-6 w-6 text-muted-foreground" />
          </div>
          <div className="flex-1">
            <p className="text-sm font-medium text-foreground">
              Connect your employee profile
            </p>
            <p className="text-xs text-muted-foreground">
              Connect your employee profile in Settings to see personalized data,
              your assigned jobs, and real-time clock status.
            </p>
          </div>
          <Button variant="outline" size="sm" disabled>
            Coming Soon
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
