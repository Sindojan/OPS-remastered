"use client";

import { useState, useMemo, useCallback } from "react";
import { useRouter, useParams } from "next/navigation";
import { useJob, useJobMutations, useQualityChecks, useStations } from "@/hooks/api/use-jobs";
import type {
  JobResponse,
  JobStatus,
  UpdateJobRequest,
  QualityCheckResponse,
  QualityResult,
  CreateQualityCheckRequest,
} from "@/types/api";
import {
  formatDate,
  formatDateTime,
  daysUntil,
  formatNumber,
  humanizeStatus,
} from "@/lib/format";
import { PageHeader } from "@/components/shared/page-header";
import { SkeletonCard, SkeletonTable } from "@/components/shared/skeleton-variants";
import {
  DomainStatusBadge,
  getJobStatusVariant,
  getPriorityVariant,
  getSeverityVariant,
} from "@/components/shared/domain-status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  ArrowLeft,
  Pencil,
  Save,
  X,
  ArrowRight,
  Clock,
  FileText,
  ShieldCheck,
  ClipboardList,
  AlertCircle,
  RefreshCw,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Plus,
  Package,
  Calendar,
  Factory,
  User,
  Hash,
} from "lucide-react";
import { toast } from "sonner";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

// ─── Edit Schema ────────────────────────────────────────
const editJobSchema = z.object({
  title: z.string().min(1, "Titel ist erforderlich"),
  priority: z.coerce.number().min(1).max(5),
  deadline: z.string().optional(),
  notes: z.string().optional(),
});

type EditJobFormData = z.infer<typeof editJobSchema>;

// ─── Quality Check Schema ───────────────────────────────
const qualityCheckSchema = z.object({
  checkType: z.string().min(1, "Prüfart ist erforderlich"),
  result: z.enum(["PASS", "FAIL", "PARTIAL"]),
  defectCount: z.coerce.number().min(0),
  notes: z.string().optional(),
});

type QualityCheckFormData = z.infer<typeof qualityCheckSchema>;

const PRIORITIES = [1, 2, 3, 4, 5];

function getQualityResultIcon(result: QualityResult) {
  switch (result) {
    case "PASS":
      return <CheckCircle2 className="h-4 w-4 text-emerald-500" />;
    case "FAIL":
      return <XCircle className="h-4 w-4 text-red-500" />;
    case "PARTIAL":
      return <AlertTriangle className="h-4 w-4 text-amber-500" />;
  }
}

function getQualityResultVariant(result: QualityResult) {
  switch (result) {
    case "PASS":
      return "success" as const;
    case "FAIL":
      return "error" as const;
    case "PARTIAL":
      return "warning" as const;
  }
}

export default function JobDetailPage() {
  const router = useRouter();
  const params = useParams();
  const jobId = params.id as string;

  const { data: job, loading, error, refetch } = useJob(jobId);
  const { data: qualityChecks, loading: qcLoading, refetch: refetchQC } = useQualityChecks(jobId);
  const { data: stations } = useStations();
  const { updateJob, createQualityCheck, loading: mutating } = useJobMutations();

  const [editing, setEditing] = useState(false);
  const [addingQC, setAddingQC] = useState(false);

  // Edit form
  const editForm = useForm<EditJobFormData>({
    resolver: zodResolver(editJobSchema) as any,
  });

  // Quality check form
  const qcForm = useForm<QualityCheckFormData>({
    resolver: zodResolver(qualityCheckSchema) as any,
    defaultValues: {
      checkType: "",
      result: "PASS",
      defectCount: 0,
      notes: "",
    },
  });

  // Station lookup
  const stationName = useMemo(() => {
    if (!job?.assignedStationId || !stations) return null;
    return stations.find((s) => s.id === job.assignedStationId)?.name ?? "Unknown";
  }, [job, stations]);

  // Start editing
  const startEdit = useCallback(() => {
    if (!job) return;
    editForm.reset({
      title: job.title,
      priority: job.priority,
      deadline: job.deadline?.split("T")[0] ?? "",
      notes: job.notes ?? "",
    });
    setEditing(true);
  }, [job, editForm]);

  // Save edit
  const handleSave = useCallback(
    async (data: EditJobFormData) => {
      if (!job) return;
      try {
        const req: UpdateJobRequest = {
          title: data.title,
          priority: data.priority,
          ...(data.deadline ? { deadline: data.deadline } : {}),
          ...(data.notes ? { notes: data.notes } : {}),
        };
        await updateJob(job.id, req);
        toast.success("Auftrag erfolgreich aktualisiert");
        setEditing(false);
        refetch();
      } catch (err) {
        toast.error("Auftrag konnte nicht aktualisiert werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
      }
    },
    [job, updateJob, refetch]
  );

  // Add quality check
  const handleAddQC = useCallback(
    async (data: QualityCheckFormData) => {
      if (!job) return;
      try {
        const req: CreateQualityCheckRequest = {
          jobId: job.id,
          checkType: data.checkType,
          result: data.result,
          defectCount: data.defectCount,
          ...(data.notes ? { notes: data.notes } : {}),
        };
        await createQualityCheck(req);
        toast.success("Qualitätsprüfung hinzugefügt");
        setAddingQC(false);
        qcForm.reset({ checkType: "", result: "PASS", defectCount: 0, notes: "" });
        refetchQC();
      } catch (err) {
        toast.error("Qualitätsprüfung konnte nicht hinzugefügt werden", { description: err instanceof Error ? err.message : "Unbekannter Fehler" });
      }
    },
    [job, createQualityCheck, qcForm, refetchQC]
  );

  // Deadline info
  const deadlineDays = job ? daysUntil(job.deadline) : null;

  // ─── Loading State ────────────────────────────────────
  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-3">
          <SkeletonCard className="h-8 w-8" />
          <SkeletonCard className="h-6 w-48" />
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
        <SkeletonTable rows={5} columns={4} />
      </div>
    );
  }

  // ─── Error State ──────────────────────────────────────
  if (error || !job) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <AlertCircle className="h-10 w-10 text-destructive/60" />
        <p className="text-sm text-muted-foreground">{error ?? "Auftrag nicht gefunden"}</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => router.push("/production")} className="gap-2">
            <ArrowLeft className="h-3.5 w-3.5" />
            Zurück
          </Button>
          <Button variant="outline" size="sm" onClick={refetch} className="gap-2">
            <RefreshCw className="h-3.5 w-3.5" />
            Erneut versuchen
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* ─── Header ──────────────────────────────────────── */}
      <PageHeader
        title={`${job.jobNumber} - ${job.title}`}
        breadcrumb={["Produktion", job.jobNumber]}
        actions={
          <div className="flex items-center gap-2">
            <DomainStatusBadge variant={getJobStatusVariant(job.status)} pulse={job.status === "IN_PRODUCTION"}>
              {humanizeStatus(job.status)}
            </DomainStatusBadge>
            <DomainStatusBadge variant={getPriorityVariant(job.priority)}>
              P{job.priority}
            </DomainStatusBadge>
            <Button variant="outline" size="sm" onClick={() => router.push("/production")} className="gap-1.5">
              <ArrowLeft className="h-3.5 w-3.5" />
              Zurück
            </Button>
            {!editing && (
              <Button variant="outline" size="sm" onClick={startEdit} className="gap-1.5">
                <Pencil className="h-3.5 w-3.5" />
                Bearbeiten
              </Button>
            )}
          </div>
        }
      />

      {/* ─── Tabs ────────────────────────────────────────── */}
      <Tabs defaultValue="overview">
        <TabsList variant="line">
          <TabsTrigger value="overview" className="gap-1.5">
            <ClipboardList className="h-3.5 w-3.5" />
            Übersicht
          </TabsTrigger>
          <TabsTrigger value="history" className="gap-1.5">
            <Clock className="h-3.5 w-3.5" />
            Statusverlauf
          </TabsTrigger>
          <TabsTrigger value="quality" className="gap-1.5">
            <ShieldCheck className="h-3.5 w-3.5" />
            Qualitätsprüfungen
          </TabsTrigger>
          <TabsTrigger value="documents" className="gap-1.5">
            <FileText className="h-3.5 w-3.5" />
            Dokumente
          </TabsTrigger>
        </TabsList>

        {/* ─── Overview Tab ──────────────────────────────── */}
        <TabsContent value="overview" className="mt-4">
          {editing ? (
            <form onSubmit={editForm.handleSubmit(handleSave as any)} className="space-y-4">
              <Card className="p-4 space-y-4">
                <div className="space-y-1.5">
                  <Label htmlFor="edit-title">Titel</Label>
                  <Input id="edit-title" {...editForm.register("title")} />
                  {editForm.formState.errors.title && (
                    <p className="text-xs text-destructive">{editForm.formState.errors.title.message}</p>
                  )}
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <Label>Priorität</Label>
                    <Select
                      value={String(editForm.watch("priority"))}
                      onValueChange={(v) => editForm.setValue("priority", Number(v))}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {PRIORITIES.map((p) => (
                          <SelectItem key={p} value={String(p)}>
                            P{p}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="edit-deadline">Fälligkeitsdatum</Label>
                    <Input id="edit-deadline" type="date" {...editForm.register("deadline")} className="font-mono text-xs" />
                  </div>
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="edit-notes">Notizen</Label>
                  <Textarea id="edit-notes" rows={3} {...editForm.register("notes")} />
                </div>
                <div className="flex gap-2">
                  <Button type="submit" size="sm" disabled={mutating} className="gap-1.5">
                    <Save className="h-3.5 w-3.5" />
                    Speichern
                  </Button>
                  <Button type="button" variant="outline" size="sm" onClick={() => setEditing(false)} className="gap-1.5">
                    <X className="h-3.5 w-3.5" />
                    Abbrechen
                  </Button>
                </div>
              </Card>
            </form>
          ) : (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
              <InfoCard icon={<User className="h-4 w-4" />} label="Kunde" value={job.customerId ?? "-"} mono />
              <InfoCard icon={<Hash className="h-4 w-4" />} label="Menge" value={formatNumber(job.quantity)} mono />
              <InfoCard
                icon={<Calendar className="h-4 w-4" />}
                label="Fälligkeitsdatum"
                value={formatDate(job.deadline)}
                mono
                accent={
                  deadlineDays !== null
                    ? deadlineDays < 0
                      ? "destructive"
                      : deadlineDays < 3
                        ? "warning"
                        : undefined
                    : undefined
                }
                suffix={
                  deadlineDays !== null
                    ? deadlineDays < 0
                      ? `${Math.abs(deadlineDays)}T überfällig`
                      : `${deadlineDays}T verbleibend`
                    : undefined
                }
              />
              <InfoCard icon={<Factory className="h-4 w-4" />} label="Station" value={stationName ?? "Nicht zugewiesen"} />
              <InfoCard icon={<Clock className="h-4 w-4" />} label="Erstellt" value={formatDateTime(job.createdAt)} mono />
              <InfoCard
                icon={<Clock className="h-4 w-4" />}
                label="Gestartet"
                value={job.startedAt ? formatDateTime(job.startedAt) : "Noch nicht gestartet"}
                mono
              />
              {job.completedAt && (
                <InfoCard
                  icon={<CheckCircle2 className="h-4 w-4" />}
                  label="Abgeschlossen"
                  value={formatDateTime(job.completedAt)}
                  mono
                />
              )}
              {job.notes && (
                <Card className="col-span-full p-4">
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">Notizen</p>
                  <p className="mt-1.5 text-sm text-foreground/80 whitespace-pre-wrap">{job.notes}</p>
                </Card>
              )}
            </div>
          )}
        </TabsContent>

        {/* ─── Status History Tab ────────────────────────── */}
        <TabsContent value="history" className="mt-4">
          {job.statusHistory.length === 0 ? (
            <Card className="flex flex-col items-center justify-center gap-2 py-12">
              <Clock className="h-8 w-8 text-muted-foreground/40" />
              <p className="text-sm text-muted-foreground">Noch keine Statusänderungen aufgezeichnet.</p>
            </Card>
          ) : (
            <div className="relative space-y-0">
              {/* Timeline line */}
              <div className="absolute left-[19px] top-2 bottom-2 w-px bg-border" />
              {[...job.statusHistory].reverse().map((entry, i) => (
                <div key={entry.id} className="relative flex gap-4 pb-6 last:pb-0">
                  {/* Timeline dot */}
                  <div className="relative z-10 flex h-10 w-10 shrink-0 items-center justify-center rounded-full border bg-background">
                    <div
                      className={`h-2.5 w-2.5 rounded-full ${
                        i === 0 ? "bg-primary" : "bg-muted-foreground/30"
                      }`}
                    />
                  </div>
                  {/* Content */}
                  <Card className="flex-1 p-3">
                    <div className="flex items-center gap-2 flex-wrap">
                      {entry.fromStatus && (
                        <>
                          <DomainStatusBadge variant={getJobStatusVariant(entry.fromStatus)}>
                            {humanizeStatus(entry.fromStatus)}
                          </DomainStatusBadge>
                          <ArrowRight className="h-3.5 w-3.5 text-muted-foreground" />
                        </>
                      )}
                      <DomainStatusBadge variant={getJobStatusVariant(entry.toStatus)}>
                        {humanizeStatus(entry.toStatus)}
                      </DomainStatusBadge>
                    </div>
                    <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground">
                      <span className="font-mono">{formatDateTime(entry.changedAt)}</span>
                      {entry.changedBy && (
                        <span className="flex items-center gap-1">
                          <User className="h-3 w-3" />
                          {entry.changedBy}
                        </span>
                      )}
                    </div>
                    {entry.reason && (
                      <p className="mt-1.5 text-xs text-foreground/70">{entry.reason}</p>
                    )}
                  </Card>
                </div>
              ))}
            </div>
          )}
        </TabsContent>

        {/* ─── Quality Checks Tab ────────────────────────── */}
        <TabsContent value="quality" className="mt-4 space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              {qualityChecks ? `${qualityChecks.length} Prüfung(en) aufgezeichnet` : "Wird geladen..."}
            </p>
            <Button size="sm" variant="outline" onClick={() => setAddingQC(true)} className="gap-1.5">
              <Plus className="h-3.5 w-3.5" />
              Prüfung hinzufügen
            </Button>
          </div>

          {/* Add QC form */}
          {addingQC && (
            <Card className="p-4">
              <form onSubmit={qcForm.handleSubmit(handleAddQC as any)} className="space-y-4">
                <p className="text-sm font-semibold">Neue Qualitätsprüfung</p>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <Label htmlFor="checkType">Prüfart</Label>
                    <Input id="checkType" placeholder="z.B. Sichtprüfung" {...qcForm.register("checkType")} />
                    {qcForm.formState.errors.checkType && (
                      <p className="text-xs text-destructive">{qcForm.formState.errors.checkType.message}</p>
                    )}
                  </div>
                  <div className="space-y-1.5">
                    <Label>Ergebnis</Label>
                    <Select
                      value={qcForm.watch("result")}
                      onValueChange={(v) => qcForm.setValue("result", v as QualityResult)}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="PASS">Bestanden</SelectItem>
                        <SelectItem value="FAIL">Nicht bestanden</SelectItem>
                        <SelectItem value="PARTIAL">Teilweise</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <Label htmlFor="defectCount">Fehleranzahl</Label>
                    <Input id="defectCount" type="number" min={0} {...qcForm.register("defectCount")} className="font-mono" />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="qcNotes">Notizen</Label>
                    <Input id="qcNotes" placeholder="Optional" {...qcForm.register("notes")} />
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button type="submit" size="sm" disabled={mutating} className="gap-1.5">
                    <Plus className="h-3.5 w-3.5" />
                    Absenden
                  </Button>
                  <Button type="button" variant="outline" size="sm" onClick={() => setAddingQC(false)}>
                    Abbrechen
                  </Button>
                </div>
              </form>
            </Card>
          )}

          {/* QC List */}
          {qcLoading ? (
            <SkeletonTable rows={3} columns={4} />
          ) : qualityChecks && qualityChecks.length > 0 ? (
            <div className="space-y-3">
              {qualityChecks.map((qc) => (
                <Card key={qc.id} className="p-4">
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex items-center gap-3">
                      {getQualityResultIcon(qc.result)}
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium">{qc.checkType}</span>
                          <DomainStatusBadge variant={getQualityResultVariant(qc.result)}>
                            {qc.result}
                          </DomainStatusBadge>
                        </div>
                        <p className="mt-0.5 text-xs text-muted-foreground">
                          {formatDateTime(qc.checkedAt)}
                          {qc.checkedBy && ` by ${qc.checkedBy}`}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <span className="font-mono text-sm font-semibold">
                        {qc.defectCount}
                      </span>
                      <p className="text-[10px] text-muted-foreground">Fehler</p>
                    </div>
                  </div>
                  {qc.notes && (
                    <p className="mt-2 text-xs text-foreground/70">{qc.notes}</p>
                  )}
                  {qc.defects && qc.defects.length > 0 && (
                    <div className="mt-3 space-y-1.5">
                      <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">Mängel</p>
                      {qc.defects.map((d) => (
                        <div key={d.id} className="flex items-center gap-2 rounded-md border px-3 py-1.5">
                          <DomainStatusBadge variant={getSeverityVariant(d.severity)}>
                            {d.severity}
                          </DomainStatusBadge>
                          <span className="text-xs font-medium">{d.defectType}</span>
                          {d.description && (
                            <span className="text-xs text-muted-foreground">- {d.description}</span>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </Card>
              ))}
            </div>
          ) : (
            <Card className="flex flex-col items-center justify-center gap-2 py-12">
              <ShieldCheck className="h-8 w-8 text-muted-foreground/40" />
              <p className="text-sm text-muted-foreground">Keine Qualitätsprüfungen aufgezeichnet.</p>
            </Card>
          )}
        </TabsContent>

        {/* ─── Documents Tab ─────────────────────────────── */}
        <TabsContent value="documents" className="mt-4">
          <Card className="flex flex-col items-center justify-center gap-3 py-16">
            <FileText className="h-10 w-10 text-muted-foreground/30" />
            <p className="text-sm font-medium text-muted-foreground">Demnächst verfügbar</p>
            <p className="text-xs text-muted-foreground/60">
              Dokumentenverwaltung wird in einem zukünftigen Update verfügbar sein.
            </p>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

// ─── Info Card Component ────────────────────────────────
function InfoCard({
  icon,
  label,
  value,
  mono = false,
  accent,
  suffix,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  mono?: boolean;
  accent?: "destructive" | "warning";
  suffix?: string;
}) {
  return (
    <Card className="p-4">
      <div className="flex items-center gap-2 text-muted-foreground">
        {icon}
        <p className="text-[11px] font-medium uppercase tracking-wider">{label}</p>
      </div>
      <div className="mt-2 flex items-baseline gap-2">
        <span
          className={`text-sm font-semibold ${mono ? "font-mono" : ""} ${
            accent === "destructive"
              ? "text-red-500"
              : accent === "warning"
                ? "text-amber-500"
                : "text-foreground"
          }`}
        >
          {value}
        </span>
        {suffix && (
          <span
            className={`text-[10px] ${
              accent === "destructive"
                ? "text-red-500/70"
                : accent === "warning"
                  ? "text-amber-500/70"
                  : "text-muted-foreground"
            }`}
          >
            {suffix}
          </span>
        )}
      </div>
    </Card>
  );
}
