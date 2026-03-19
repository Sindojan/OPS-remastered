"use client";

import { useState, useMemo, useCallback, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  AlertTriangle,
  Clock,
  Calendar,
  Award,
  Briefcase,
  Plus,
  Check,
  X,
  Pencil,
  Save,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import {
  DomainStatusBadge,
  getEmployeeStatusVariant,
  getAbsenceStatusVariant,
} from "@/components/shared/domain-status-badge";
import { SkeletonCard, SkeletonTable } from "@/components/shared/skeleton-variants";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

import {
  useEmployee,
  useTimeEntries,
  useAbsences,
  useQualifications,
  usePeopleMutations,
} from "@/hooks/api/use-people";
import type {
  TimeEntryResponse,
  AbsenceResponse,
  QualificationResponse,
  AbsenceType,
} from "@/types/api";
import { formatDate, formatDateTime, formatTime, humanizeStatus } from "@/lib/format";
import { toast } from "sonner";

// ─── Schemas ────────────────────────────────────────────

const absenceSchema = z
  .object({
    type: z.enum(["VACATION", "SICK", "OTHER"]),
    fromDate: z.string().min(1, "Start date is required"),
    toDate: z.string().min(1, "End date is required"),
    notes: z.string().optional(),
  })
  .refine((data) => data.toDate >= data.fromDate, {
    message: "End date must be on or after start date",
    path: ["toDate"],
  });

type AbsenceFormData = z.infer<typeof absenceSchema>;

const qualificationSchema = z.object({
  qualification: z.string().min(1, "Qualification name is required"),
  certifiedAt: z.string().optional(),
  expiresAt: z.string().optional(),
});

type QualificationFormData = z.infer<typeof qualificationSchema>;

const manualEntrySchema = z.object({
  type: z.enum(["CLOCK_IN", "CLOCK_OUT", "JOB_START", "JOB_END"]),
  timestamp: z.string().min(1, "Timestamp is required"),
  jobId: z.string().optional(),
});

type ManualEntryFormData = z.infer<typeof manualEntrySchema>;

// ─── Time Entry Type Variant Helper ─────────────────────

function getTimeEntryTypeVariant(type: string) {
  switch (type) {
    case "CLOCK_IN":
      return "success" as const;
    case "CLOCK_OUT":
      return "neutral" as const;
    case "JOB_START":
      return "info" as const;
    case "JOB_END":
      return "warning" as const;
    default:
      return "neutral" as const;
  }
}

// ─── Component ──────────────────────────────────────────

export default function EmployeeDetailPage() {
  const router = useRouter();
  const params = useParams();
  const employeeId = params.id as string;

  const { data: employee, loading, error, refetch } = useEmployee(employeeId);
  const { data: timeEntries, loading: timeLoading, refetch: refetchTime } = useTimeEntries(employeeId);
  const { data: absences, loading: absencesLoading, refetch: refetchAbsences } = useAbsences(employeeId);
  const { data: qualifications, loading: qualsLoading, refetch: refetchQuals } = useQualifications(employeeId);
  const mutations = usePeopleMutations();

  // Dialog states
  const [absenceDialogOpen, setAbsenceDialogOpen] = useState(false);
  const [qualDialogOpen, setQualDialogOpen] = useState(false);
  const [manualEntryDialogOpen, setManualEntryDialogOpen] = useState(false);
  const [editMode, setEditMode] = useState(false);

  // Inline edit state
  const [editFirstName, setEditFirstName] = useState("");
  const [editLastName, setEditLastName] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editPhone, setEditPhone] = useState("");
  const [editRole, setEditRole] = useState("");

  useEffect(() => {
    if (employee) {
      setEditFirstName(employee.firstName);
      setEditLastName(employee.lastName);
      setEditEmail(employee.email || "");
      setEditPhone(employee.phone || "");
      setEditRole(employee.role || "");
    }
  }, [employee]);

  // Forms
  const absenceForm = useForm<AbsenceFormData>({
    resolver: zodResolver(absenceSchema),
    defaultValues: { type: "VACATION", fromDate: "", toDate: "", notes: "" },
  });

  const qualForm = useForm<QualificationFormData>({
    resolver: zodResolver(qualificationSchema),
    defaultValues: { qualification: "", certifiedAt: "", expiresAt: "" },
  });

  const manualEntryForm = useForm<ManualEntryFormData>({
    resolver: zodResolver(manualEntrySchema),
    defaultValues: { type: "CLOCK_IN", timestamp: "", jobId: "" },
  });

  // ─── Time Entry Columns ────────────────────────────────

  const timeColumns: ColumnDef<TimeEntryResponse>[] = useMemo(
    () => [
      {
        id: "timestamp",
        header: "Date",
        accessorKey: "timestamp",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {formatDate(row.timestamp)}
          </span>
        ),
      },
      {
        id: "type",
        header: "Type",
        accessorKey: "type",
        sortable: true,
        cell: (row) => (
          <DomainStatusBadge variant={getTimeEntryTypeVariant(row.type)}>
            {humanizeStatus(row.type)}
          </DomainStatusBadge>
        ),
      },
      {
        id: "time",
        header: "Time",
        sortable: false,
        accessorFn: (row) => (
          <span className="font-mono text-xs font-semibold text-foreground">
            {formatTime(row.timestamp)}
          </span>
        ),
      },
      {
        id: "jobId",
        header: "Job Reference",
        accessorKey: "jobId",
        sortable: false,
        cell: (row) =>
          row.jobId ? (
            <span className="font-mono text-xs text-muted-foreground">
              {row.jobId.slice(0, 8)}...
            </span>
          ) : (
            <span className="text-muted-foreground">–</span>
          ),
      },
    ],
    []
  );

  // ─── Absence Columns ───────────────────────────────────

  const absenceColumns: ColumnDef<AbsenceResponse>[] = useMemo(
    () => [
      {
        id: "type",
        header: "Type",
        accessorKey: "type",
        sortable: true,
        cell: (row) => (
          <span className="font-medium text-foreground">
            {humanizeStatus(row.type)}
          </span>
        ),
      },
      {
        id: "fromDate",
        header: "From",
        accessorKey: "fromDate",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {formatDate(row.fromDate)}
          </span>
        ),
      },
      {
        id: "toDate",
        header: "To",
        accessorKey: "toDate",
        sortable: true,
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {formatDate(row.toDate)}
          </span>
        ),
      },
      {
        id: "status",
        header: "Status",
        accessorKey: "status",
        sortable: true,
        cell: (row) => (
          <DomainStatusBadge variant={getAbsenceStatusVariant(row.status)}>
            {humanizeStatus(row.status)}
          </DomainStatusBadge>
        ),
      },
      {
        id: "notes",
        header: "Notes",
        accessorKey: "notes",
        sortable: false,
        cell: (row) => (
          <span className="max-w-[200px] truncate text-xs text-muted-foreground">
            {row.notes || "–"}
          </span>
        ),
      },
    ],
    []
  );

  // ─── Pending Absences ──────────────────────────────────

  const pendingAbsences = useMemo(
    () => (absences || []).filter((a) => a.status === "PENDING"),
    [absences]
  );

  // ─── Handlers ──────────────────────────────────────────

  const handleSaveEdit = useCallback(async () => {
    if (!employee) return;
    try {
      const result = await mutations.updateEmployee(employee.id, {
        employeeNumber: employee.employeeNumber,
        firstName: editFirstName,
        lastName: editLastName,
        email: editEmail || undefined,
        phone: editPhone || undefined,
        role: editRole || undefined,
      });
      if (result) {
        toast.success("Employee updated successfully");
        setEditMode(false);
        refetch();
      }
    } catch (err) {
      toast.error("Failed to update employee", { description: err instanceof Error ? err.message : "Unknown error" });
    }
  }, [employee, editFirstName, editLastName, editEmail, editPhone, editRole, mutations, refetch]);

  const handleAbsenceSubmit = useCallback(
    async (data: AbsenceFormData) => {
      if (!employeeId) return;
      try {
        const result = await mutations.createAbsence({
          employeeId,
          type: data.type,
          fromDate: data.fromDate,
          toDate: data.toDate,
          notes: data.notes || undefined,
        });
        if (result) {
          toast.success("Absence request submitted");
          setAbsenceDialogOpen(false);
          absenceForm.reset();
          refetchAbsences();
        }
      } catch (err) {
        toast.error("Failed to submit absence", { description: err instanceof Error ? err.message : "Unknown error" });
      }
    },
    [employeeId, mutations, absenceForm, refetchAbsences]
  );

  const handleQualificationSubmit = useCallback(
    async (data: QualificationFormData) => {
      if (!employeeId) return;
      try {
        const result = await mutations.addQualification(employeeId, {
          qualification: data.qualification,
          certifiedAt: data.certifiedAt || undefined,
          expiresAt: data.expiresAt || undefined,
        });
        if (result) {
          toast.success("Qualification added successfully");
          setQualDialogOpen(false);
          qualForm.reset();
          refetchQuals();
        }
      } catch (err) {
        toast.error("Failed to add qualification", { description: err instanceof Error ? err.message : "Unknown error" });
      }
    },
    [employeeId, mutations, qualForm, refetchQuals]
  );

  const handleManualEntry = useCallback(
    async (data: ManualEntryFormData) => {
      if (!employeeId) return;
      try {
        // Use the appropriate mutation based on type
        let result = null;
        switch (data.type) {
          case "CLOCK_IN":
            result = await mutations.clockIn(employeeId);
            break;
          case "CLOCK_OUT":
            result = await mutations.clockOut(employeeId);
            break;
          case "JOB_START":
            if (data.jobId) {
              result = await mutations.jobStart(employeeId, data.jobId);
            }
            break;
          case "JOB_END":
            if (data.jobId) {
              result = await mutations.jobEnd(employeeId, data.jobId);
            }
            break;
        }
        if (result !== undefined) {
          toast.success("Time entry created successfully");
          setManualEntryDialogOpen(false);
          manualEntryForm.reset();
          refetchTime();
        }
      } catch (err) {
        toast.error("Failed to create time entry", { description: err instanceof Error ? err.message : "Unknown error" });
      }
    },
    [employeeId, mutations, manualEntryForm, refetchTime]
  );

  const handleApproveAbsence = useCallback(
    async (id: string) => {
      try {
        await mutations.approveAbsence(id);
        toast.success("Absence approved");
        refetchAbsences();
      } catch (err) {
        toast.error("Failed to approve absence", { description: err instanceof Error ? err.message : "Unknown error" });
      }
    },
    [mutations, refetchAbsences]
  );

  const handleRejectAbsence = useCallback(
    async (id: string) => {
      try {
        await mutations.rejectAbsence(id);
        toast.success("Absence rejected");
        refetchAbsences();
      } catch (err) {
        toast.error("Failed to reject absence", { description: err instanceof Error ? err.message : "Unknown error" });
      }
    },
    [mutations, refetchAbsences]
  );

  // ─── Loading State ─────────────────────────────────────

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="space-y-2">
          <div className="h-3 w-32 rounded bg-muted" />
          <div className="h-6 w-48 rounded bg-muted" />
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
        <SkeletonTable rows={5} columns={4} />
      </div>
    );
  }

  // ─── Error State ───────────────────────────────────────

  if (error || !employee) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <AlertTriangle className="h-8 w-8 text-destructive" />
        <p className="text-sm text-muted-foreground">
          {error || "Employee not found"}
        </p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => router.push("/employees")}>
            Back to People
          </Button>
          <Button variant="outline" size="sm" onClick={refetch}>
            Retry
          </Button>
        </div>
      </div>
    );
  }

  // ─── Render ────────────────────────────────────────────

  return (
    <div className="space-y-6">
      <PageHeader
        title={`${employee.firstName} ${employee.lastName}`}
        breadcrumb={["People", `${employee.firstName} ${employee.lastName}`]}
        actions={
          <DomainStatusBadge
            variant={getEmployeeStatusVariant(employee.status)}
            pulse={employee.status === "ON_LEAVE"}
          >
            {humanizeStatus(employee.status)}
          </DomainStatusBadge>
        }
      />

      <Tabs defaultValue="overview">
        <TabsList variant="line">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="time">Time Tracking</TabsTrigger>
          <TabsTrigger value="absences">Absences</TabsTrigger>
          <TabsTrigger value="jobs">Job Bookings</TabsTrigger>
        </TabsList>

        {/* ═══ Overview Tab ═══ */}
        <TabsContent value="overview" className="space-y-6">
          {/* Key Data */}
          <Card>
            <CardHeader className="flex-row items-center justify-between">
              <CardTitle className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Employee Information
              </CardTitle>
              {editMode ? (
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    className="gap-1.5"
                    onClick={() => {
                      setEditMode(false);
                      setEditFirstName(employee.firstName);
                      setEditLastName(employee.lastName);
                      setEditEmail(employee.email || "");
                      setEditPhone(employee.phone || "");
                      setEditRole(employee.role || "");
                    }}
                  >
                    <X className="h-3 w-3" />
                    Cancel
                  </Button>
                  <Button
                    size="sm"
                    className="gap-1.5"
                    onClick={handleSaveEdit}
                    disabled={mutations.loading}
                  >
                    <Save className="h-3 w-3" />
                    {mutations.loading ? "Saving..." : "Save"}
                  </Button>
                </div>
              ) : (
                <Button
                  variant="outline"
                  size="sm"
                  className="gap-1.5"
                  onClick={() => setEditMode(true)}
                >
                  <Pencil className="h-3 w-3" />
                  Edit
                </Button>
              )}
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-x-8 gap-y-4 sm:grid-cols-3 lg:grid-cols-4">
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                    Employee #
                  </p>
                  <p className="mt-0.5 font-mono text-sm font-semibold text-foreground">
                    {employee.employeeNumber}
                  </p>
                </div>
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                    First Name
                  </p>
                  {editMode ? (
                    <Input
                      value={editFirstName}
                      onChange={(e) => setEditFirstName(e.target.value)}
                      className="mt-0.5 h-8 text-sm"
                    />
                  ) : (
                    <p className="mt-0.5 text-sm text-foreground">
                      {employee.firstName}
                    </p>
                  )}
                </div>
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                    Last Name
                  </p>
                  {editMode ? (
                    <Input
                      value={editLastName}
                      onChange={(e) => setEditLastName(e.target.value)}
                      className="mt-0.5 h-8 text-sm"
                    />
                  ) : (
                    <p className="mt-0.5 text-sm text-foreground">
                      {employee.lastName}
                    </p>
                  )}
                </div>
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                    Email
                  </p>
                  {editMode ? (
                    <Input
                      value={editEmail}
                      onChange={(e) => setEditEmail(e.target.value)}
                      className="mt-0.5 h-8 text-sm"
                    />
                  ) : (
                    <p className="mt-0.5 text-sm text-muted-foreground">
                      {employee.email || "–"}
                    </p>
                  )}
                </div>
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                    Phone
                  </p>
                  {editMode ? (
                    <Input
                      value={editPhone}
                      onChange={(e) => setEditPhone(e.target.value)}
                      className="mt-0.5 h-8 text-sm"
                    />
                  ) : (
                    <p className="mt-0.5 text-sm text-muted-foreground">
                      {employee.phone || "–"}
                    </p>
                  )}
                </div>
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                    Role
                  </p>
                  {editMode ? (
                    <Input
                      value={editRole}
                      onChange={(e) => setEditRole(e.target.value)}
                      className="mt-0.5 h-8 text-sm"
                    />
                  ) : (
                    <p className="mt-0.5 text-sm text-foreground">
                      {employee.role || "–"}
                    </p>
                  )}
                </div>
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                    Hire Date
                  </p>
                  <p className="mt-0.5 font-mono text-sm text-muted-foreground">
                    {formatDate(employee.hireDate)}
                  </p>
                </div>
                <div>
                  <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                    Station
                  </p>
                  <p className="mt-0.5 font-mono text-sm text-muted-foreground">
                    {employee.stationId
                      ? `${employee.stationId.slice(0, 8)}...`
                      : "–"}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Qualifications */}
          <Card>
            <CardHeader className="flex-row items-center justify-between">
              <CardTitle className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Qualifications
              </CardTitle>
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5"
                onClick={() => setQualDialogOpen(true)}
              >
                <Plus className="h-3 w-3" />
                Add Qualification
              </Button>
            </CardHeader>
            <CardContent>
              {qualsLoading ? (
                <div className="flex gap-2">
                  {Array.from({ length: 3 }).map((_, i) => (
                    <div
                      key={i}
                      className="h-8 w-32 animate-pulse rounded-md bg-muted"
                    />
                  ))}
                </div>
              ) : qualifications && qualifications.length > 0 ? (
                <div className="flex flex-wrap gap-2">
                  {qualifications.map((q) => (
                    <div
                      key={q.id}
                      className="flex items-center gap-2 rounded-lg border border-primary/20 bg-primary/5 px-3 py-1.5"
                    >
                      <Award className="h-3.5 w-3.5 text-primary" />
                      <div>
                        <p className="text-sm font-medium text-foreground">
                          {q.qualification}
                        </p>
                        <p className="font-mono text-[10px] text-muted-foreground">
                          {q.certifiedAt
                            ? `Certified ${formatDate(q.certifiedAt)}`
                            : "No certification date"}
                          {q.expiresAt
                            ? ` · Expires ${formatDate(q.expiresAt)}`
                            : ""}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">
                  No qualifications recorded yet.
                </p>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* ═══ Time Tracking Tab ═══ */}
        <TabsContent value="time" className="space-y-6">
          {/* Today Status Card */}
          <Card>
            <CardContent className="flex items-center gap-4 pt-6">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                <Clock className="h-6 w-6 text-primary" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-medium text-foreground">
                  Today&apos;s Status
                </p>
                <p className="font-mono text-xs text-muted-foreground">
                  Check time entries below for clock-in/out history
                </p>
              </div>
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5"
                onClick={() => setManualEntryDialogOpen(true)}
              >
                <Plus className="h-3 w-3" />
                Manual Entry
              </Button>
            </CardContent>
          </Card>

          {/* Time Entries Table */}
          <DataTable<TimeEntryResponse>
            data={timeEntries || []}
            columns={timeColumns}
            searchPlaceholder="Search entries..."
            loading={timeLoading}
            pageSize={15}
            emptyState={{
              icon: <Clock className="h-8 w-8 text-muted-foreground/40" />,
              title: "No time entries",
              description: "Time entries will appear here when the employee clocks in.",
            }}
          />
        </TabsContent>

        {/* ═══ Absences Tab ═══ */}
        <TabsContent value="absences" className="space-y-6">
          {/* Pending Requests */}
          {pendingAbsences.length > 0 && (
            <Card className="border-amber-500/20 bg-amber-500/5">
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-sm font-semibold text-amber-600 dark:text-amber-400">
                  <Calendar className="h-4 w-4" />
                  Pending Approval ({pendingAbsences.length})
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {pendingAbsences.map((absence) => (
                    <div
                      key={absence.id}
                      className="flex items-center justify-between rounded-lg border border-amber-500/20 bg-background px-4 py-3"
                    >
                      <div className="space-y-0.5">
                        <p className="text-sm font-medium text-foreground">
                          {humanizeStatus(absence.type)}
                        </p>
                        <p className="font-mono text-xs text-muted-foreground">
                          {formatDate(absence.fromDate)} – {formatDate(absence.toDate)}
                        </p>
                        {absence.notes && (
                          <p className="text-xs text-muted-foreground">
                            {absence.notes}
                          </p>
                        )}
                      </div>
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          className="gap-1.5 border-emerald-500/30 text-emerald-600 hover:bg-emerald-500/10 dark:text-emerald-400"
                          onClick={() => handleApproveAbsence(absence.id)}
                          disabled={mutations.loading}
                        >
                          <Check className="h-3 w-3" />
                          Approve
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          className="gap-1.5 border-red-500/30 text-red-600 hover:bg-red-500/10 dark:text-red-400"
                          onClick={() => handleRejectAbsence(absence.id)}
                          disabled={mutations.loading}
                        >
                          <X className="h-3 w-3" />
                          Reject
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Absences Table */}
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              All Absences
            </h3>
            <Button
              size="sm"
              className="gap-1.5"
              onClick={() => setAbsenceDialogOpen(true)}
            >
              <Plus className="h-3 w-3" />
              Request Absence
            </Button>
          </div>

          <DataTable<AbsenceResponse>
            data={absences || []}
            columns={absenceColumns}
            loading={absencesLoading}
            pageSize={10}
            emptyState={{
              icon: <Calendar className="h-8 w-8 text-muted-foreground/40" />,
              title: "No absences recorded",
              description:
                "Absence requests and history will appear here.",
            }}
          />
        </TabsContent>

        {/* ═══ Job Bookings Tab ═══ */}
        <TabsContent value="jobs">
          <div className="flex flex-col items-center justify-center gap-4 py-20">
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-muted">
              <Briefcase className="h-8 w-8 text-muted-foreground/40" />
            </div>
            <p className="text-sm font-medium text-foreground/70">
              Job booking aggregation coming soon
            </p>
            <p className="max-w-sm text-center text-xs text-muted-foreground">
              This section will show aggregated job booking data, work hours per
              job, and productivity metrics for this employee.
            </p>
          </div>
        </TabsContent>
      </Tabs>

      {/* ═══ Request Absence Dialog ═══ */}
      <Dialog open={absenceDialogOpen} onOpenChange={setAbsenceDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Calendar className="h-4 w-4 text-primary" />
              Request Absence
            </DialogTitle>
            <DialogDescription>
              Submit an absence request for {employee.firstName}{" "}
              {employee.lastName}.
            </DialogDescription>
          </DialogHeader>
          <form
            onSubmit={absenceForm.handleSubmit(handleAbsenceSubmit as any)}
            className="space-y-4"
          >
            <div className="space-y-1.5">
              <Label>Type *</Label>
              <Select
                value={absenceForm.watch("type")}
                onValueChange={(v) =>
                  absenceForm.setValue("type", v as AbsenceType)
                }
              >
                <SelectTrigger className="text-sm">
                  <SelectValue placeholder="Typ wählen..." />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="VACATION">Vacation</SelectItem>
                  <SelectItem value="SICK">Sick</SelectItem>
                  <SelectItem value="OTHER">Other</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="fromDate">From *</Label>
                <Input
                  id="fromDate"
                  type="date"
                  {...absenceForm.register("fromDate")}
                />
                {absenceForm.formState.errors.fromDate && (
                  <p className="text-xs text-destructive">
                    {absenceForm.formState.errors.fromDate.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="toDate">To *</Label>
                <Input
                  id="toDate"
                  type="date"
                  {...absenceForm.register("toDate")}
                />
                {absenceForm.formState.errors.toDate && (
                  <p className="text-xs text-destructive">
                    {absenceForm.formState.errors.toDate.message}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="notes">Notes</Label>
              <Input
                id="notes"
                placeholder="Optional notes..."
                {...absenceForm.register("notes")}
              />
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setAbsenceDialogOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit" size="sm" disabled={mutations.loading}>
                {mutations.loading ? "Submitting..." : "Submit Request"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* ═══ Add Qualification Dialog ═══ */}
      <Dialog open={qualDialogOpen} onOpenChange={setQualDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Award className="h-4 w-4 text-primary" />
              Add Qualification
            </DialogTitle>
            <DialogDescription>
              Record a qualification for {employee.firstName}{" "}
              {employee.lastName}.
            </DialogDescription>
          </DialogHeader>
          <form
            onSubmit={qualForm.handleSubmit(handleQualificationSubmit as any)}
            className="space-y-4"
          >
            <div className="space-y-1.5">
              <Label htmlFor="qualification">Qualification *</Label>
              <Input
                id="qualification"
                placeholder="Welding Certificate, Forklift License..."
                {...qualForm.register("qualification")}
              />
              {qualForm.formState.errors.qualification && (
                <p className="text-xs text-destructive">
                  {qualForm.formState.errors.qualification.message}
                </p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="certifiedAt">Certified At</Label>
                <Input
                  id="certifiedAt"
                  type="date"
                  {...qualForm.register("certifiedAt")}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="expiresAt">Expires At</Label>
                <Input
                  id="expiresAt"
                  type="date"
                  {...qualForm.register("expiresAt")}
                />
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setQualDialogOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit" size="sm" disabled={mutations.loading}>
                {mutations.loading ? "Adding..." : "Add Qualification"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* ═══ Manual Time Entry Dialog ═══ */}
      <Dialog
        open={manualEntryDialogOpen}
        onOpenChange={setManualEntryDialogOpen}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Clock className="h-4 w-4 text-primary" />
              Manual Time Entry
            </DialogTitle>
            <DialogDescription>
              Create a manual time entry for {employee.firstName}{" "}
              {employee.lastName}.
            </DialogDescription>
          </DialogHeader>
          <form
            onSubmit={manualEntryForm.handleSubmit(handleManualEntry as any)}
            className="space-y-4"
          >
            <div className="space-y-1.5">
              <Label>Type *</Label>
              <Select
                value={manualEntryForm.watch("type")}
                onValueChange={(v) =>
                  manualEntryForm.setValue(
                    "type",
                    v as "CLOCK_IN" | "CLOCK_OUT" | "JOB_START" | "JOB_END"
                  )
                }
              >
                <SelectTrigger className="text-sm">
                  <SelectValue placeholder="Typ wählen..." />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="CLOCK_IN">Clock In</SelectItem>
                  <SelectItem value="CLOCK_OUT">Clock Out</SelectItem>
                  <SelectItem value="JOB_START">Job Start</SelectItem>
                  <SelectItem value="JOB_END">Job End</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="timestamp">Timestamp *</Label>
              <Input
                id="timestamp"
                type="datetime-local"
                {...manualEntryForm.register("timestamp")}
              />
              {manualEntryForm.formState.errors.timestamp && (
                <p className="text-xs text-destructive">
                  {manualEntryForm.formState.errors.timestamp.message}
                </p>
              )}
            </div>

            {(manualEntryForm.watch("type") === "JOB_START" ||
              manualEntryForm.watch("type") === "JOB_END") && (
              <div className="space-y-1.5">
                <Label htmlFor="jobId">Job ID</Label>
                <Input
                  id="jobId"
                  placeholder="Job UUID"
                  className="font-mono text-xs"
                  {...manualEntryForm.register("jobId")}
                />
              </div>
            )}

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setManualEntryDialogOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit" size="sm" disabled={mutations.loading}>
                {mutations.loading ? "Creating..." : "Create Entry"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
