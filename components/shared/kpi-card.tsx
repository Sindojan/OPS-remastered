"use client";

import { cn } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { TrendingUp, TrendingDown, Minus } from "lucide-react";
import { LineChart, Line, ResponsiveContainer } from "recharts";

interface KpiCardProps {
  label: string;
  value: string;
  unit?: string;
  trend?: {
    direction: "up" | "down" | "neutral";
    value: string;
  };
  sparkline?: {
    data: number[];
  };
  loading?: boolean;
  className?: string;
}

export function KpiCard({
  label,
  value,
  unit,
  trend,
  sparkline,
  loading,
  className,
}: KpiCardProps) {
  if (loading) {
    return (
      <Card className={cn("relative overflow-hidden p-4", className)}>
        <div className="space-y-3">
          <Skeleton className="h-3.5 w-24" />
          <Skeleton className="h-8 w-20" />
          <Skeleton className="h-3 w-16" />
        </div>
      </Card>
    );
  }

  const trendColor =
    trend?.direction === "up"
      ? "text-success"
      : trend?.direction === "down"
        ? "text-error"
        : "text-muted-foreground";

  const TrendIcon =
    trend?.direction === "up"
      ? TrendingUp
      : trend?.direction === "down"
        ? TrendingDown
        : Minus;

  const sparklineData = sparkline?.data.map((v, i) => ({ v, i }));

  return (
    <Card
      className={cn(
        "group relative overflow-hidden p-4 transition-shadow duration-200 hover:shadow-md",
        className
      )}
    >
      {/* Subtle top accent line */}
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />

      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          {/* Label */}
          <p className="truncate text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
            {label}
          </p>

          {/* Value */}
          <div className="mt-1.5 flex items-baseline gap-1">
            <span className="font-mono text-2xl font-bold tracking-tight text-foreground">
              {value}
            </span>
            {unit && (
              <span className="text-xs font-medium text-muted-foreground">
                {unit}
              </span>
            )}
          </div>

          {/* Trend */}
          {trend && (
            <div className={cn("mt-2 flex items-center gap-1", trendColor)}>
              <TrendIcon className="h-3 w-3" />
              <span className="font-mono text-[11px] font-medium">
                {trend.value}
              </span>
            </div>
          )}
        </div>

        {/* Sparkline */}
        {sparklineData && sparklineData.length > 0 && (
          <div className="h-10 w-20 shrink-0 opacity-60 transition-opacity group-hover:opacity-100">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={sparklineData}>
                <Line
                  type="monotone"
                  dataKey="v"
                  stroke="#2ba8a0"
                  strokeWidth={1.5}
                  dot={false}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </Card>
  );
}
