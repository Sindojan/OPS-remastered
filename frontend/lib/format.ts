import { t } from "@/lib/i18n";

export function formatDate(isoString: string | null | undefined): string {
  if (!isoString) return "–";
  return new Date(isoString).toLocaleDateString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function formatDateTime(isoString: string | null | undefined): string {
  if (!isoString) return "–";
  return new Date(isoString).toLocaleString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatTime(isoString: string | null | undefined): string {
  if (!isoString) return "–";
  return new Date(isoString).toLocaleTimeString("de-DE", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatRelativeDate(isoString: string | null | undefined): string {
  if (!isoString) return "–";
  const date = new Date(isoString);
  const now = new Date();
  const diffMs = date.getTime() - now.getTime();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return "Heute";
  if (diffDays === 1) return "Morgen";
  if (diffDays === -1) return "Gestern";
  if (diffDays > 0 && diffDays <= 7) return `in ${diffDays} T.`;
  if (diffDays < 0 && diffDays >= -7) return `vor ${Math.abs(diffDays)} T.`;
  return formatDate(isoString);
}

export function daysUntil(isoString: string | null | undefined): number | null {
  if (!isoString) return null;
  const date = new Date(isoString);
  const now = new Date();
  return Math.ceil((date.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
}

export function formatNumber(value: number | null | undefined): string {
  if (value == null) return "–";
  return value.toLocaleString("de-DE");
}

export function formatCurrency(value: number | null | undefined, currency = "EUR"): string {
  if (value == null) return "–";
  return value.toLocaleString("de-DE", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
  });
}

export function humanizeStatus(status: string): string {
  const translated = t(status);
  if (translated !== status) return translated;
  return status
    .replace(/_/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
}
