// ─── German Translations for Owlsburg OPS ─────────────────────

// Status labels (DB enum values → German display)
export const statusLabels: Record<string, string> = {
  // General
  ACTIVE: "Aktiv",
  INACTIVE: "Inaktiv",
  DRAFT: "Entwurf",
  RELEASED: "Freigegeben",
  IN_PRODUCTION: "In Produktion",
  COMPLETED: "Abgeschlossen",
  ON_HOLD: "Pausiert",
  CANCELLED: "Storniert",
  // Machines
  AVAILABLE: "Verfügbar",
  IN_USE: "In Betrieb",
  MAINTENANCE: "Wartung",
  BLOCKED: "Blockiert",
  DECOMMISSIONED: "Außer Betrieb",
  // Conversations
  OPEN: "Offen",
  IN_PROGRESS: "In Bearbeitung",
  WAITING: "Wartend",
  RESOLVED: "Gelöst",
  ARCHIVED: "Archiviert",
  // Absences
  PENDING: "Ausstehend",
  APPROVED: "Genehmigt",
  REJECTED: "Abgelehnt",
  // Maintenance Records
  PLANNED: "Geplant",
  DONE: "Erledigt",
  SKIPPED: "Übersprungen",
  // Company
  SUSPENDED: "Gesperrt",
  DELETED: "Gelöscht",
  // Time Entry Types
  CLOCK_IN: "Einstempeln",
  CLOCK_OUT: "Ausstempeln",
  JOB_START: "Auftrag gestartet",
  JOB_END: "Auftrag beendet",
  // Absence Types
  VACATION: "Urlaub",
  SICK: "Krankheit",
  OTHER: "Sonstiges",
  // Part Types
  PRODUCT: "Produkt",
  COMPONENT: "Bauteil",
  RAW_MATERIAL: "Rohstoff",
  // BOM Version Status
  // DRAFT, ACTIVE, ARCHIVED already covered above
  // Stock Movement Types
  INBOUND: "Zugang",
  OUTBOUND: "Abgang",
  TRANSFER: "Transfer",
  CORRECTION: "Korrektur",
  // Address Types
  BILLING: "Rechnung",
  SHIPPING: "Lieferung",
  BOTH: "Beides",
  // Severity
  LOW: "Niedrig",
  MEDIUM: "Mittel",
  HIGH: "Hoch",
  CRITICAL: "Kritisch",
  // Priority (string)
  NORMAL: "Normal",
  URGENT: "Dringend",
  // Conversation Source
  EMAIL: "E-Mail",
  MANUAL: "Manuell",
  AGENT: "Agent",
  // Sender Type
  USER: "Benutzer",
  CUSTOMER: "Kunde",
  // Maintenance Type
  TIME_BASED: "Zeitbasiert",
  HOURS_BASED: "Stundenbasiert",
  // Quality
  PASS: "Bestanden",
  FAIL: "Nicht bestanden",
  PARTIAL: "Teilweise",
};

// Roles
export const roleLabels: Record<string, string> = {
  SYSTEM_ADMIN: "Systemadministrator",
  ADMIN: "Administrator",
  MANAGER: "Manager",
  TEAM_LEAD: "Teamleiter",
  WORKER: "Mitarbeiter",
  AGENT_SYSTEM: "Agent-System",
};

// Company plans
export const planLabels: Record<string, string> = {
  BASIC: "Basis",
  PROFESSIONAL: "Professional",
  ENTERPRISE: "Enterprise",
};

// Translate a status/enum value to German
export function t(key: string): string {
  return statusLabels[key] ?? roleLabels[key] ?? planLabels[key] ?? key;
}
