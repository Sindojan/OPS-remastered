import { cn } from "@/lib/utils";

type StatusVariant = "success" | "warning" | "error" | "info" | "neutral" | "primary";

interface DomainStatusBadgeProps {
  children: React.ReactNode;
  variant?: StatusVariant;
  className?: string;
  pulse?: boolean;
}

const variantStyles: Record<StatusVariant, { bg: string; text: string; border: string; dot: string }> = {
  success: {
    bg: "bg-emerald-500/10",
    text: "text-emerald-600 dark:text-emerald-400",
    border: "border-emerald-500/20",
    dot: "bg-emerald-500",
  },
  warning: {
    bg: "bg-amber-500/10",
    text: "text-amber-600 dark:text-amber-400",
    border: "border-amber-500/20",
    dot: "bg-amber-500",
  },
  error: {
    bg: "bg-red-500/10",
    text: "text-red-600 dark:text-red-400",
    border: "border-red-500/20",
    dot: "bg-red-500",
  },
  info: {
    bg: "bg-blue-500/10",
    text: "text-blue-600 dark:text-blue-400",
    border: "border-blue-500/20",
    dot: "bg-blue-500",
  },
  neutral: {
    bg: "bg-muted",
    text: "text-muted-foreground",
    border: "border-border",
    dot: "bg-muted-foreground/50",
  },
  primary: {
    bg: "bg-primary/10",
    text: "text-primary",
    border: "border-primary/20",
    dot: "bg-primary",
  },
};

export function DomainStatusBadge({ children, variant = "neutral", className, pulse }: DomainStatusBadgeProps) {
  const styles = variantStyles[variant];

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-md border px-2 py-0.5 font-mono text-[11px] font-medium tracking-wide",
        styles.bg,
        styles.text,
        styles.border,
        className
      )}
    >
      <span
        className={cn("h-1.5 w-1.5 rounded-full", styles.dot)}
        style={pulse ? { animation: "status-pulse 2s ease-in-out infinite" } : undefined}
      />
      {children}
    </span>
  );
}

// Helper to map common domain statuses to variants
export function getJobStatusVariant(status: string): StatusVariant {
  switch (status) {
    case "DRAFT": return "neutral";
    case "RELEASED": return "info";
    case "IN_PRODUCTION": return "primary";
    case "ON_HOLD": return "warning";
    case "COMPLETED": return "success";
    case "CANCELLED": return "error";
    default: return "neutral";
  }
}

export function getMachineStatusVariant(status: string): StatusVariant {
  switch (status) {
    case "AVAILABLE": return "success";
    case "IN_USE": return "primary";
    case "MAINTENANCE": return "warning";
    case "BLOCKED": return "error";
    case "DECOMMISSIONED": return "neutral";
    default: return "neutral";
  }
}

export function getConversationStatusVariant(status: string): StatusVariant {
  switch (status) {
    case "OPEN": return "info";
    case "IN_PROGRESS": return "primary";
    case "WAITING": return "warning";
    case "RESOLVED": return "success";
    case "ARCHIVED": return "neutral";
    default: return "neutral";
  }
}

export function getPriorityVariant(priority: number | string): StatusVariant {
  if (typeof priority === "number") {
    if (priority >= 4) return "error";
    if (priority >= 3) return "warning";
    if (priority >= 2) return "info";
    return "neutral";
  }
  switch (priority) {
    case "URGENT": return "error";
    case "HIGH": return "warning";
    case "NORMAL": return "info";
    case "LOW": return "neutral";
    default: return "neutral";
  }
}

export function getSeverityVariant(severity: string): StatusVariant {
  switch (severity) {
    case "CRITICAL": return "error";
    case "HIGH": return "warning";
    case "MEDIUM": return "info";
    case "LOW": return "neutral";
    default: return "neutral";
  }
}

export function getEmployeeStatusVariant(status: string): StatusVariant {
  switch (status) {
    case "ACTIVE": return "success";
    case "INACTIVE": return "neutral";
    case "ON_LEAVE": return "warning";
    default: return "neutral";
  }
}

export function getAbsenceStatusVariant(status: string): StatusVariant {
  switch (status) {
    case "PENDING": return "warning";
    case "APPROVED": return "success";
    case "REJECTED": return "error";
    default: return "neutral";
  }
}
