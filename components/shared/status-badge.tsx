import { cn } from "@/lib/utils";

type Status = "idle" | "busy" | "degraded" | "quarantine" | "success" | "error" | "warning";

interface StatusBadgeProps {
  status: Status;
  className?: string;
  children?: React.ReactNode;
}

const statusConfig: Record<Status, { label: string; bg: string; text: string; border: string; dot: string }> = {
  idle: {
    label: "Idle",
    bg: "bg-agent-idle/10",
    text: "text-agent-idle",
    border: "border-agent-idle/20",
    dot: "bg-agent-idle",
  },
  busy: {
    label: "Busy",
    bg: "bg-agent-busy/10",
    text: "text-agent-busy",
    border: "border-agent-busy/20",
    dot: "bg-agent-busy",
  },
  degraded: {
    label: "Degraded",
    bg: "bg-agent-degraded/10",
    text: "text-agent-degraded",
    border: "border-agent-degraded/20",
    dot: "bg-agent-degraded",
  },
  quarantine: {
    label: "Quarantine",
    bg: "bg-agent-quarantine/10",
    text: "text-agent-quarantine",
    border: "border-agent-quarantine/20",
    dot: "bg-agent-quarantine",
  },
  success: {
    label: "Success",
    bg: "bg-success/10",
    text: "text-success",
    border: "border-success/20",
    dot: "bg-success",
  },
  error: {
    label: "Error",
    bg: "bg-error/10",
    text: "text-error",
    border: "border-error/20",
    dot: "bg-error",
  },
  warning: {
    label: "Warning",
    bg: "bg-warning/10",
    text: "text-warning",
    border: "border-warning/20",
    dot: "bg-warning",
  },
};

export function StatusBadge({ status, className, children }: StatusBadgeProps) {
  const config = statusConfig[status];
  const isPulsing = status === "busy";

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-md border px-2 py-0.5 font-mono text-[11px] font-medium tracking-wide",
        config.bg,
        config.text,
        config.border,
        className
      )}
    >
      <span
        className={cn("h-1.5 w-1.5 rounded-full", config.dot)}
        style={isPulsing ? { animation: "status-pulse 2s ease-in-out infinite" } : undefined}
      />
      {children ?? config.label}
    </span>
  );
}
