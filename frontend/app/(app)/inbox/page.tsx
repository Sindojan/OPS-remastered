"use client";

import { useState, useMemo, useCallback, useRef, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Plus,
  Mail,
  Send,
  Search,
  User,
  Bot,
  UserCircle,
  ArrowRightLeft,
  AlertTriangle,
  MessageSquare,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import {
  DomainStatusBadge,
  getConversationStatusVariant,
  getPriorityVariant,
} from "@/components/shared/domain-status-badge";
import { SkeletonCard } from "@/components/shared/skeleton-variants";

import { Button } from "@/components/ui/button";
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
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";

import {
  useConversations,
  useConversation,
  useMessages,
  useInboxMutations,
} from "@/hooks/api/use-inbox";
import type {
  ConversationResponse,
  ConversationStatus,
  ConversationPriority,
  SenderType,
} from "@/types/api";
import { formatRelativeDate, formatDateTime, humanizeStatus } from "@/lib/format";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

// ─── Constants ──────────────────────────────────────────

const CONVERSATION_STATUSES: ConversationStatus[] = [
  "OPEN",
  "IN_PROGRESS",
  "WAITING",
  "RESOLVED",
  "ARCHIVED",
];

const PRIORITIES: ConversationPriority[] = ["LOW", "NORMAL", "HIGH", "URGENT"];

// ─── Schemas ────────────────────────────────────────────

const createConversationSchema = z.object({
  subject: z.string().min(1, "Betreff ist erforderlich"),
  customerId: z.string().optional(),
  priority: z.string().optional(),
  source: z.string().optional(),
});

type CreateConversationFormData = z.infer<typeof createConversationSchema>;

// ─── Component ──────────────────────────────────────────

export default function InboxPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Selected conversation from URL or state
  const urlId = searchParams.get("id");
  const [selectedId, setSelectedId] = useState<string | null>(urlId);

  // Sync URL param to state
  useEffect(() => {
    if (urlId && urlId !== selectedId) {
      setSelectedId(urlId);
    }
  }, [urlId]); // eslint-disable-line react-hooks/exhaustive-deps

  // Data hooks
  const { data: conversations, loading, error, refetch } = useConversations();
  const { data: selectedConversation, refetch: refetchConversation } =
    useConversation(selectedId);
  const { data: messages, loading: messagesLoading, refetch: refetchMessages } =
    useMessages(selectedId);
  const mutations = useInboxMutations();

  // Filters
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [priorityFilter, setPriorityFilter] = useState<string>("ALL");

  // Dialogs
  const [createOpen, setCreateOpen] = useState(false);
  const [statusDialogOpen, setStatusDialogOpen] = useState(false);
  const [priorityDialogOpen, setPriorityDialogOpen] = useState(false);
  const [assignDialogOpen, setAssignDialogOpen] = useState(false);

  // Dialog state
  const [newStatus, setNewStatus] = useState<string>("OPEN");
  const [newPriority, setNewPriority] = useState<string>("NORMAL");
  const [assignTo, setAssignTo] = useState("");

  // Reply state
  const [replyContent, setReplyContent] = useState("");
  const [sending, setSending] = useState(false);

  // Message scroll ref
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Form
  const form = useForm<CreateConversationFormData>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(createConversationSchema) as any,
    defaultValues: {
      subject: "",
      customerId: "",
      priority: "NORMAL",
      source: "MANUAL",
    },
  });

  // ─── Filtered Data ─────────────────────────────────────

  const filteredConversations = useMemo(() => {
    if (!conversations) return [];
    let result = conversations;
    if (statusFilter !== "ALL") {
      result = result.filter((c) => c.status === statusFilter);
    }
    if (priorityFilter !== "ALL") {
      result = result.filter((c) => c.priority === priorityFilter);
    }
    if (searchTerm.trim()) {
      const term = searchTerm.toLowerCase();
      result = result.filter((c) =>
        c.subject.toLowerCase().includes(term)
      );
    }
    // Sort by updatedAt descending
    return [...result].sort(
      (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    );
  }, [conversations, statusFilter, priorityFilter, searchTerm]);

  // Sorted messages
  const sortedMessages = useMemo(() => {
    if (!messages) return [];
    return [...messages].sort(
      (a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
    );
  }, [messages]);

  // Auto-scroll messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [sortedMessages]);

  // ─── Handlers ──────────────────────────────────────────

  const handleSelectConversation = useCallback(
    (id: string) => {
      setSelectedId(id);
      const params = new URLSearchParams(searchParams.toString());
      params.set("id", id);
      router.replace(`/inbox?${params.toString()}`, { scroll: false });
    },
    [router, searchParams]
  );

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const onCreateSubmit = useCallback(async (data: CreateConversationFormData) => {
    try {
      const result = await mutations.createConversation({
        subject: data.subject,
        customerId: data.customerId || undefined,
        priority: (data.priority as ConversationPriority) || "NORMAL",
        source: "MANUAL",
      });
      if (result) {
        toast.success("Konversation erstellt");
        setCreateOpen(false);
        form.reset({
          subject: "",
          customerId: "",
          priority: "NORMAL",
          source: "MANUAL",
        });
        await refetch();
        // Auto-select the newly created conversation
        if (result.id) {
          handleSelectConversation(result.id);
        }
      }
    } catch (err) {
      toast.error("Fehler beim Erstellen der Konversation", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [mutations, form, refetch, handleSelectConversation]);

  const handleChangeStatus = useCallback(async () => {
    if (!selectedId) return;
    try {
      const result = await mutations.updateStatus(selectedId, newStatus);
      if (result) {
        toast.success("Status aktualisiert");
        setStatusDialogOpen(false);
        refetch();
        refetchConversation();
      }
    } catch (err) {
      toast.error("Fehler beim Aktualisieren des Status", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [mutations, selectedId, newStatus, refetch, refetchConversation]);

  const handleChangePriority = useCallback(async () => {
    if (!selectedId) return;
    try {
      const result = await mutations.updatePriority(selectedId, newPriority);
      if (result) {
        toast.success("Priorität aktualisiert");
        setPriorityDialogOpen(false);
        refetch();
        refetchConversation();
      }
    } catch (err) {
      toast.error("Fehler beim Aktualisieren der Priorität", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [mutations, selectedId, newPriority, refetch, refetchConversation]);

  const handleAssign = useCallback(async () => {
    if (!selectedId || !assignTo.trim()) return;
    try {
      const result = await mutations.assign(selectedId, assignTo);
      if (result) {
        toast.success("Konversation zugewiesen");
        setAssignDialogOpen(false);
        setAssignTo("");
        refetch();
        refetchConversation();
      }
    } catch (err) {
      toast.error("Fehler beim Zuweisen der Konversation", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [mutations, selectedId, assignTo, refetch, refetchConversation]);

  const handleSendMessage = useCallback(async () => {
    if (!selectedId || !replyContent.trim()) return;
    setSending(true);
    try {
      const result = await mutations.sendMessage(selectedId, {
        content: replyContent.trim(),
        senderType: "USER",
      });
      if (result) {
        setReplyContent("");
        refetchMessages();
      }
    } catch (err) {
      toast.error("Fehler beim Senden der Nachricht", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setSending(false);
    }
  }, [mutations, selectedId, replyContent, refetchMessages]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        handleSendMessage();
      }
    },
    [handleSendMessage]
  );

  // ─── Sender helpers ────────────────────────────────────

  function getSenderIcon(type: SenderType) {
    switch (type) {
      case "USER":
        return <User className="h-3.5 w-3.5" />;
      case "AGENT":
        return <Bot className="h-3.5 w-3.5" />;
      case "CUSTOMER":
        return <UserCircle className="h-3.5 w-3.5" />;
    }
  }

  function getSenderColor(type: SenderType) {
    switch (type) {
      case "USER":
        return "bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20";
      case "AGENT":
        return "bg-primary/10 text-primary border-primary/20";
      case "CUSTOMER":
        return "bg-muted text-muted-foreground border-border";
    }
  }

  function getPriorityDotColor(priority: ConversationPriority) {
    switch (priority) {
      case "URGENT":
        return "bg-red-500";
      case "HIGH":
        return "bg-amber-500";
      case "NORMAL":
        return "bg-blue-500";
      case "LOW":
        return "bg-muted-foreground/50";
    }
  }

  function getSourceLabel(source: string) {
    switch (source) {
      case "EMAIL":
        return "E-Mail";
      case "MANUAL":
        return "Manuell";
      case "AGENT":
        return "Agent";
      default:
        return source;
    }
  }

  // ─── Error State ───────────────────────────────────────

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <AlertTriangle className="h-8 w-8 text-destructive" />
        <p className="text-sm text-muted-foreground">{error}</p>
        <Button variant="outline" size="sm" onClick={refetch}>
          Erneut versuchen
        </Button>
      </div>
    );
  }

  // ─── Render ────────────────────────────────────────────

  return (
    <div className="flex h-[calc(100vh-6rem)] flex-col">
      <PageHeader
        title="Posteingang"
        description="Konversationen und Support-Threads"
      />

      <div className="flex min-h-0 flex-1 gap-0 rounded-lg border">
        {/* ═══ Left Panel: Conversation List ═══ */}
        <div className="flex w-1/3 min-w-[280px] flex-col border-r">
          {/* Search + Filters + New Button */}
          <div className="space-y-2 border-b p-3">
            <div className="flex items-center gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
                <Input
                  placeholder="Konversationen suchen..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="h-8 pl-8 text-sm"
                />
              </div>
              <Button
                size="sm"
                className="h-8 gap-1 px-2.5"
                onClick={() => setCreateOpen(true)}
              >
                <Plus className="h-3.5 w-3.5" />
                <span className="hidden lg:inline">Neu</span>
              </Button>
            </div>
            <div className="flex items-center gap-2">
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="h-7 flex-1 text-xs">
                  <SelectValue placeholder="Status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Alle Status</SelectItem>
                  {CONVERSATION_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {humanizeStatus(s)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select value={priorityFilter} onValueChange={setPriorityFilter}>
                <SelectTrigger className="h-7 flex-1 text-xs">
                  <SelectValue placeholder="Priorität" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Alle Prioritäten</SelectItem>
                  {PRIORITIES.map((p) => (
                    <SelectItem key={p} value={p}>
                      {humanizeStatus(p)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Conversation Items */}
          <ScrollArea className="flex-1">
            {loading ? (
              <div className="space-y-1 p-2">
                {Array.from({ length: 6 }).map((_, i) => (
                  <div key={i} className="space-y-2 rounded-md border p-3">
                    <Skeleton className="h-3.5 w-3/4" />
                    <div className="flex items-center gap-2">
                      <Skeleton className="h-5 w-16" />
                      <Skeleton className="h-3 w-12" />
                    </div>
                  </div>
                ))}
              </div>
            ) : filteredConversations.length === 0 ? (
              <div className="flex flex-col items-center justify-center gap-3 py-12 text-center">
                <Mail className="h-8 w-8 text-muted-foreground/40" />
                <div>
                  <p className="text-sm font-medium text-muted-foreground">
                    Keine Konversationen gefunden
                  </p>
                  <p className="text-xs text-muted-foreground/60">
                    Erstellen Sie eine neue Konversation
                  </p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setCreateOpen(true)}
                >
                  <Plus className="mr-1.5 h-3.5 w-3.5" />
                  Neue Konversation
                </Button>
              </div>
            ) : (
              <div className="space-y-0.5 p-1.5">
                {filteredConversations.map((conv) => (
                  <button
                    key={conv.id}
                    onClick={() => handleSelectConversation(conv.id)}
                    className={cn(
                      "flex w-full flex-col gap-1.5 rounded-md border px-3 py-2.5 text-left transition-colors hover:bg-muted/50",
                      selectedId === conv.id
                        ? "border-primary/50 bg-primary/5"
                        : "border-transparent"
                    )}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <p className="line-clamp-1 text-sm font-medium leading-tight">
                        {conv.subject}
                      </p>
                      <span
                        className={cn(
                          "mt-0.5 h-2 w-2 shrink-0 rounded-full",
                          getPriorityDotColor(conv.priority)
                        )}
                      />
                    </div>
                    <div className="flex items-center gap-2">
                      <DomainStatusBadge
                        variant={getConversationStatusVariant(conv.status)}
                      >
                        {humanizeStatus(conv.status)}
                      </DomainStatusBadge>
                      <span className="rounded bg-muted px-1.5 py-0.5 font-mono text-[10px] text-muted-foreground">
                        {getSourceLabel(conv.source)}
                      </span>
                      <span className="ml-auto font-mono text-[10px] text-muted-foreground">
                        {formatRelativeDate(conv.updatedAt)}
                      </span>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </ScrollArea>
        </div>

        {/* ═══ Right Panel: Conversation Detail ═══ */}
        <div className="flex flex-1 flex-col">
          {!selectedId || !selectedConversation ? (
            /* Empty State */
            <div className="flex flex-1 flex-col items-center justify-center gap-3">
              <Mail className="h-10 w-10 text-muted-foreground/30" />
              <p className="text-sm text-muted-foreground">
                Konversation auswählen
              </p>
            </div>
          ) : (
            <>
              {/* Detail Header */}
              <div className="flex items-start justify-between gap-4 border-b px-4 py-3">
                <div className="min-w-0 flex-1 space-y-1">
                  <h2 className="truncate text-base font-semibold">
                    {selectedConversation.subject}
                  </h2>
                  <div className="flex flex-wrap items-center gap-2">
                    <DomainStatusBadge
                      variant={getConversationStatusVariant(
                        selectedConversation.status
                      )}
                    >
                      {humanizeStatus(selectedConversation.status)}
                    </DomainStatusBadge>
                    <DomainStatusBadge
                      variant={getPriorityVariant(selectedConversation.priority)}
                    >
                      {selectedConversation.priority}
                    </DomainStatusBadge>
                    {selectedConversation.customerId && (
                      <span className="font-mono text-[11px] text-muted-foreground">
                        Kunde: {selectedConversation.customerId.slice(0, 8)}...
                      </span>
                    )}
                    {selectedConversation.assignedTo && (
                      <span className="font-mono text-[11px] text-muted-foreground">
                        Zugewiesen: {selectedConversation.assignedTo.slice(0, 8)}...
                      </span>
                    )}
                  </div>
                </div>
                <div className="flex shrink-0 items-center gap-1.5">
                  <Button
                    variant="outline"
                    size="sm"
                    className="h-7 gap-1 text-xs"
                    onClick={() => {
                      setNewStatus(selectedConversation.status);
                      setStatusDialogOpen(true);
                    }}
                  >
                    <ArrowRightLeft className="h-3 w-3" />
                    Status
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="h-7 gap-1 text-xs"
                    onClick={() => {
                      setNewPriority(selectedConversation.priority);
                      setPriorityDialogOpen(true);
                    }}
                  >
                    Priorität
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="h-7 gap-1 text-xs"
                    onClick={() => setAssignDialogOpen(true)}
                  >
                    Zuweisen
                  </Button>
                </div>
              </div>

              {/* Message Thread */}
              <ScrollArea className="flex-1 px-4">
                <div className="space-y-3 py-4">
                  {messagesLoading ? (
                    <div className="space-y-3">
                      {Array.from({ length: 3 }).map((_, i) => (
                        <div
                          key={i}
                          className={cn(
                            "flex",
                            i % 2 === 0 ? "justify-start" : "justify-end"
                          )}
                        >
                          <div className="w-2/3 space-y-1.5 rounded-lg border p-3">
                            <Skeleton className="h-3 w-16" />
                            <Skeleton className="h-4 w-full" />
                            <Skeleton className="h-4 w-3/4" />
                            <Skeleton className="h-2.5 w-20" />
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : sortedMessages.length === 0 ? (
                    <div className="flex flex-col items-center justify-center gap-2 py-12">
                      <MessageSquare className="h-8 w-8 text-muted-foreground/30" />
                      <p className="text-sm text-muted-foreground">
                        Noch keine Nachrichten
                      </p>
                      <p className="text-xs text-muted-foreground/60">
                        Senden Sie die erste Nachricht
                      </p>
                    </div>
                  ) : (
                    sortedMessages.map((msg) => {
                      const isUser = msg.senderType === "USER";
                      return (
                        <div
                          key={msg.id}
                          className={cn(
                            "flex",
                            isUser ? "justify-end" : "justify-start"
                          )}
                        >
                          <div
                            className={cn(
                              "max-w-[75%] space-y-1 rounded-lg border px-3 py-2",
                              isUser
                                ? "bg-blue-500/5 border-blue-500/20"
                                : msg.senderType === "AGENT"
                                ? "bg-primary/5 border-primary/20"
                                : "bg-muted/50 border-border"
                            )}
                          >
                            <div className="flex items-center gap-1.5">
                              <span
                                className={cn(
                                  "inline-flex items-center gap-1 rounded-md border px-1.5 py-0.5 text-[10px] font-medium",
                                  getSenderColor(msg.senderType)
                                )}
                              >
                                {getSenderIcon(msg.senderType)}
                                {msg.senderType}
                              </span>
                            </div>
                            <p className="whitespace-pre-wrap text-sm leading-relaxed">
                              {msg.content}
                            </p>
                            <p className="font-mono text-[10px] text-muted-foreground/60">
                              {formatDateTime(msg.sentAt)}
                            </p>
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>
              </ScrollArea>

              {/* Reply Input */}
              <div className="border-t p-3">
                <div className="flex items-end gap-2">
                  <Textarea
                    placeholder="Nachricht eingeben... (Enter zum Senden, Shift+Enter für neue Zeile)"
                    value={replyContent}
                    onChange={(e) => setReplyContent(e.target.value)}
                    onKeyDown={handleKeyDown}
                    className="min-h-[60px] max-h-[120px] resize-none text-sm"
                    rows={2}
                  />
                  <Button
                    size="sm"
                    className="h-9 gap-1.5 px-3"
                    onClick={handleSendMessage}
                    disabled={sending || !replyContent.trim()}
                  >
                    <Send className="h-3.5 w-3.5" />
                    Senden
                  </Button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* ═══ Create Conversation Dialog ═══ */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Mail className="h-4 w-4 text-primary" />
              Neue Konversation
            </DialogTitle>
            <DialogDescription>
              Neue Support-Konversation starten.
            </DialogDescription>
          </DialogHeader>
          <form
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            onSubmit={form.handleSubmit(onCreateSubmit as any)}
            className="space-y-4"
          >
            <div className="space-y-1.5">
              <Label htmlFor="subject">Betreff *</Label>
              <Input
                id="subject"
                placeholder="Betreff eingeben"
                {...form.register("subject")}
              />
              {form.formState.errors.subject && (
                <p className="text-xs text-destructive">
                  {form.formState.errors.subject.message}
                </p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="customerId">Kunden-ID</Label>
              <Input
                id="customerId"
                placeholder="Optionale Kunden-UUID"
                className="font-mono text-xs"
                {...form.register("customerId")}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Priorität</Label>
                <Select
                  value={form.watch("priority") || "NORMAL"}
                  onValueChange={(v) => form.setValue("priority", v)}
                >
                  <SelectTrigger className="text-sm">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {PRIORITIES.map((p) => (
                      <SelectItem key={p} value={p}>
                        {humanizeStatus(p)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Quelle</Label>
                <Select
                  value={form.watch("source") || "MANUAL"}
                  onValueChange={(v) => form.setValue("source", v)}
                >
                  <SelectTrigger className="text-sm">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="MANUAL">Manuell</SelectItem>
                    <SelectItem value="EMAIL">E-Mail</SelectItem>
                    <SelectItem value="AGENT">Agent</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setCreateOpen(false)}
              >
                Abbrechen
              </Button>
              <Button type="submit" size="sm" disabled={mutations.loading}>
                {mutations.loading ? "Erstellen..." : "Konversation erstellen"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* ═══ Change Status Dialog ═══ */}
      <Dialog open={statusDialogOpen} onOpenChange={setStatusDialogOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Status ändern</DialogTitle>
            <DialogDescription>
              Konversationsstatus aktualisieren.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label>Aktueller Status</Label>
              {selectedConversation && (
                <DomainStatusBadge
                  variant={getConversationStatusVariant(
                    selectedConversation.status
                  )}
                >
                  {humanizeStatus(selectedConversation.status)}
                </DomainStatusBadge>
              )}
            </div>
            <div className="space-y-1.5">
              <Label>Neuer Status</Label>
              <Select value={newStatus} onValueChange={setNewStatus}>
                <SelectTrigger className="text-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {CONVERSATION_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {humanizeStatus(s)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setStatusDialogOpen(false)}
            >
              Abbrechen
            </Button>
            <Button
              size="sm"
              onClick={handleChangeStatus}
              disabled={mutations.loading}
            >
              {mutations.loading ? "Aktualisieren..." : "Status aktualisieren"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ═══ Change Priority Dialog ═══ */}
      <Dialog open={priorityDialogOpen} onOpenChange={setPriorityDialogOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Priorität ändern</DialogTitle>
            <DialogDescription>
              Konversationspriorität aktualisieren.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label>Aktuelle Priorität</Label>
              {selectedConversation && (
                <DomainStatusBadge
                  variant={getPriorityVariant(selectedConversation.priority)}
                >
                  {selectedConversation.priority}
                </DomainStatusBadge>
              )}
            </div>
            <div className="space-y-1.5">
              <Label>Neue Priorität</Label>
              <Select value={newPriority} onValueChange={setNewPriority}>
                <SelectTrigger className="text-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PRIORITIES.map((p) => (
                    <SelectItem key={p} value={p}>
                      {humanizeStatus(p)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPriorityDialogOpen(false)}
            >
              Abbrechen
            </Button>
            <Button
              size="sm"
              onClick={handleChangePriority}
              disabled={mutations.loading}
            >
              {mutations.loading ? "Aktualisieren..." : "Priorität aktualisieren"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ═══ Assign Dialog ═══ */}
      <Dialog open={assignDialogOpen} onOpenChange={setAssignDialogOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Konversation zuweisen</DialogTitle>
            <DialogDescription>
              Diese Konversation einem Benutzer zuweisen.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label>Zuweisen an (Benutzer-ID)</Label>
            <Input
              placeholder="Benutzer-UUID"
              className="font-mono text-xs"
              value={assignTo}
              onChange={(e) => setAssignTo(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setAssignDialogOpen(false)}
            >
              Abbrechen
            </Button>
            <Button
              size="sm"
              onClick={handleAssign}
              disabled={mutations.loading || !assignTo.trim()}
            >
              {mutations.loading ? "Zuweisen..." : "Zuweisen"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
