"use client";

import { useEffect, useRef, useState, useCallback, useMemo } from "react";
import { Button } from "@/components/ui/button";
import { Send, Loader2, Plus, Trash2 } from "lucide-react";
import { MarkdownMessage } from "@/components/chat/markdown-message";
import {
  DelegationCard,
  LEAD_LABELS,
  formatTokens,
  type ChatMessage,
  type LeadStepInfo,
  type TokenUsage,
} from "@/components/chat/delegation-card";
import { useSystemChatSessions, useSystemChatMessages } from "@/hooks/api/use-system-chat";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import type { SystemChatSession } from "@/types/system-agent";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
// Well-known System CEO instance ID from V23 seed
const SYSTEM_CEO_ID = "b0000000-0000-0000-0000-000000000001";

export default function SystemChatPage() {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [streaming, setStreaming] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const { data: sessions, refetch: refetchSessions } = useSystemChatSessions();
  const { data: dbMessages, refetch: refetchMessages } = useSystemChatMessages(sessionId);

  const sessionTokenTotal = useMemo(() => {
    return messages.reduce((sum, m) => {
      if (m.usage) return sum + m.usage.inputTokens + m.usage.outputTokens;
      return sum;
    }, 0);
  }, [messages]);

  // Load messages when session changes
  useEffect(() => {
    if (dbMessages) {
      setMessages(
        dbMessages.map((m) => ({
          role: m.role as "user" | "assistant",
          content: m.content,
        }))
      );
    }
  }, [dbMessages]);

  // Auto-scroll
  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages]);

  const sendMessage = useCallback(async () => {
    const msg = input.trim();
    if (!msg || streaming) return;
    setInput("");
    setStreaming(true);

    setMessages((prev) => [...prev, { role: "user", content: msg }]);
    setMessages((prev) => [...prev, { role: "assistant", content: "" }]);

    try {
      const token = localStorage.getItem("owlsburg_token");
      const response = await fetch(`${API_BASE}/api/system/chat/message`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          message: msg,
          agentInstanceId: SYSTEM_CEO_ID,
          sessionId: sessionId,
        }),
      });

      if (!response.ok) throw new Error(`HTTP ${response.status}`);

      const reader = response.body?.getReader();
      if (!reader) throw new Error("No reader");

      const decoder = new TextDecoder();
      let buffer = "";

      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });

        const lines = buffer.split("\n");
        buffer = lines.pop() || "";

        for (const line of lines) {
          if (!line.startsWith("data:")) continue;
          const dataStr = line.slice(5).trim();
          if (!dataStr) continue;

          try {
            const data = JSON.parse(dataStr);

            if (data.sessionId && !sessionId) {
              setSessionId(data.sessionId);
            }

            if (data.token) {
              setMessages((prev) => {
                const updated = [...prev];
                const last = updated[updated.length - 1];
                if (last?.role === "assistant") {
                  updated[updated.length - 1] = {
                    ...last,
                    content: last.content + data.token,
                  };
                }
                return updated;
              });
            }

            if (data.delegation) {
              const label = LEAD_LABELS[data.delegation.lead] || data.delegation.lead;
              const delegationId = data.delegation.id || undefined;
              setMessages((prev) => [
                ...prev,
                {
                  role: "tool" as const,
                  content: `Delegiere an **${label}**...\n_${data.delegation.task}_`,
                  toolCalls: [{ name: "delegate_to_lead", input: data.delegation.task }],
                  leadSteps: [],
                  delegationId,
                },
              ]);
            }

            if (data.leadStep) {
              const step: LeadStepInfo = {
                type: data.leadStep.type,
                toolName: data.leadStep.toolName,
                content: data.leadStep.content,
                iteration: data.leadStep.iteration,
              };
              const matchId = data.leadStep.id;
              setMessages((prev) => {
                const updated = [...prev];
                for (let i = updated.length - 1; i >= 0; i--) {
                  if (updated[i].role === "tool" && updated[i].delegationId === matchId) {
                    const m = { ...updated[i] };
                    m.leadSteps = [...(m.leadSteps || []), step];
                    updated[i] = m;
                    break;
                  }
                }
                return updated;
              });
            }

            if (data.delegationResult) {
              const matchId = data.delegationResult.id;
              setMessages((prev) => {
                const updated = [...prev];
                for (let i = updated.length - 1; i >= 0; i--) {
                  if (
                    updated[i].role === "tool" &&
                    (matchId ? updated[i].delegationId === matchId :
                      updated[i].toolCalls?.[0]?.name === "delegate_to_lead")
                  ) {
                    const label = LEAD_LABELS[data.delegationResult.lead] || data.delegationResult.lead;
                    const m = { ...updated[i] };
                    const stepCount = m.leadSteps?.length || 0;
                    m.content = `**${label}** hat geantwortet` + (stepCount > 0 ? ` (${stepCount} Schritte)` : "");
                    const toolCalls = [...(m.toolCalls || [])];
                    toolCalls[0] = { ...toolCalls[0], result: data.delegationResult.result };
                    m.toolCalls = toolCalls;
                    updated[i] = m;
                    break;
                  }
                }
                if (updated[updated.length - 1]?.role !== "assistant") {
                  updated.push({ role: "assistant", content: "" });
                }
                return updated;
              });
            }

            if (data.toolCall) {
              setMessages((prev) => [
                ...prev,
                {
                  role: "tool" as const,
                  content: `\u{1F527} **${data.toolCall.name}** wird aufgerufen...`,
                  toolCalls: [{ name: data.toolCall.name, input: data.toolCall.input }],
                },
              ]);
            }

            if (data.toolResult) {
              setMessages((prev) => {
                const updated = [...prev];
                for (let i = updated.length - 1; i >= 0; i--) {
                  if (
                    updated[i].role === "tool" &&
                    updated[i].toolCalls?.[0]?.name === data.toolResult.name
                  ) {
                    const m = { ...updated[i] };
                    const toolCalls = [...(m.toolCalls || [])];
                    toolCalls[0] = { ...toolCalls[0], result: data.toolResult.result };
                    m.toolCalls = toolCalls;
                    m.content = `\u{2705} **${data.toolResult.name}** abgeschlossen`;
                    updated[i] = m;
                    break;
                  }
                }
                if (updated[updated.length - 1]?.role !== "assistant") {
                  updated.push({ role: "assistant", content: "" });
                }
                return updated;
              });
            }

            if (data.usage) {
              setMessages((prev) => {
                const updated = [...prev];
                for (let i = updated.length - 1; i >= 0; i--) {
                  if (updated[i].role === "assistant" && updated[i].content) {
                    updated[i] = { ...updated[i], usage: data.usage as TokenUsage };
                    break;
                  }
                }
                return updated;
              });
            }

            if (data.done) {
              setMessages((prev) => {
                if (prev[prev.length - 1]?.role === "assistant" && !prev[prev.length - 1]?.content) {
                  return prev.slice(0, -1);
                }
                return prev;
              });
              refetchSessions();
            }

            if (data.error) {
              setMessages((prev) => {
                const updated = [...prev];
                const last = updated[updated.length - 1];
                if (last?.role === "assistant") {
                  updated[updated.length - 1] = {
                    ...last,
                    content: `**Fehler:** ${data.error}`,
                  };
                }
                return updated;
              });
            }
          } catch {
            // Skip unparseable lines
          }
        }
      }
    } catch (e) {
      setMessages((prev) => {
        const updated = [...prev];
        const last = updated[updated.length - 1];
        if (last?.role === "assistant") {
          updated[updated.length - 1] = {
            ...last,
            content: `**Fehler:** ${e instanceof Error ? e.message : "Verbindungsfehler"}`,
          };
        }
        return updated;
      });
    } finally {
      setStreaming(false);
    }
  }, [input, streaming, sessionId, refetchSessions]);

  const handleNewSession = () => {
    setSessionId(null);
    setMessages([]);
  };

  const handleSelectSession = (s: SystemChatSession) => {
    setSessionId(s.id);
  };

  const handleDeleteSession = async () => {
    if (!deleteId) return;
    try {
      const token = localStorage.getItem("owlsburg_token");
      await fetch(`${API_BASE}/api/system/chat/sessions/${deleteId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
      if (sessionId === deleteId) {
        setSessionId(null);
        setMessages([]);
      }
      refetchSessions();
    } finally {
      setDeleteId(null);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <div className="flex h-[calc(100vh-7rem)] gap-4">
      {/* Session sidebar */}
      <div className="w-64 shrink-0 rounded-lg border bg-card flex flex-col">
        <div className="flex items-center justify-between border-b p-3">
          <span className="text-sm font-semibold">Sessions</span>
          <Button variant="ghost" size="icon-sm" onClick={handleNewSession}>
            <Plus className="h-4 w-4" />
          </Button>
        </div>
        <div className="flex-1 overflow-y-auto p-2 space-y-0.5">
          {sessions?.map((s) => (
            <div
              key={s.id}
              className={`flex items-center justify-between rounded-md px-2 py-1.5 text-sm cursor-pointer transition-colors ${
                sessionId === s.id ? "bg-accent text-accent-foreground" : "hover:bg-muted"
              }`}
              onClick={() => handleSelectSession(s)}
            >
              <span className="truncate flex-1">{s.title || "Neue Session"}</span>
              <Button
                variant="ghost"
                size="icon-sm"
                className="h-5 w-5 shrink-0 opacity-0 group-hover:opacity-100 hover:opacity-100"
                onClick={(e) => {
                  e.stopPropagation();
                  setDeleteId(s.id);
                }}
              >
                <Trash2 className="h-3 w-3" />
              </Button>
            </div>
          ))}
        </div>
      </div>

      {/* Chat area */}
      <div className="flex flex-1 flex-col rounded-lg border bg-card">
        {/* Header with token counter */}
        <div className="flex items-center justify-between border-b px-4 py-2">
          <span className="text-sm font-semibold">System CEO Chat</span>
          {sessionTokenTotal > 0 && (
            <span className="text-[10px] text-muted-foreground font-mono">
              {formatTokens(sessionTokenTotal)} tok
            </span>
          )}
        </div>

        {/* Messages */}
        <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 space-y-3">
          {messages.length === 0 && (
            <div className="flex h-full items-center justify-center">
              <div className="text-center text-muted-foreground">
                <p className="text-lg font-medium">System CEO Chat</p>
                <p className="text-sm">Stellen Sie Fragen zur Plattform-Verwaltung</p>
              </div>
            </div>
          )}
          {messages.map((msg, i) => (
            <div
              key={i}
              className={
                msg.role === "user"
                  ? "ml-8 rounded-lg rounded-tr-sm bg-primary/10 p-3 text-sm text-foreground"
                  : msg.role === "tool"
                  ? "mx-4 rounded-md border border-border/50 bg-muted/30 px-3 py-2 text-xs text-muted-foreground font-mono"
                  : "mr-8 rounded-lg rounded-tl-sm bg-muted p-3 text-sm text-foreground"
              }
            >
              {msg.role === "assistant" ? (
                <>
                  <MarkdownMessage
                    content={
                      streaming && i === messages.length - 1
                        ? msg.content + " \u258B"
                        : msg.content
                    }
                  />
                  {streaming && i === messages.length - 1 && !msg.content && (
                    <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                  )}
                  {msg.usage && (
                    <div className="mt-1.5 text-[10px] text-muted-foreground/60 font-mono">
                      {"\u2191"}{formatTokens(msg.usage.inputTokens)} {"\u2193"}{formatTokens(msg.usage.outputTokens)}
                    </div>
                  )}
                </>
              ) : msg.role === "tool" ? (
                msg.delegationId != null ? (
                  <DelegationCard msg={msg} isStreaming={streaming} />
                ) : (
                  <MarkdownMessage content={msg.content} />
                )
              ) : (
                <span className="whitespace-pre-wrap">{msg.content}</span>
              )}
            </div>
          ))}
        </div>

        {/* Input */}
        <div className="border-t p-3">
          <div className="flex gap-2">
            <textarea
              ref={textareaRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Nachricht an System-CEO..."
              rows={1}
              className="flex-1 resize-none rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-1 focus:ring-primary"
              disabled={streaming}
            />
            <Button size="icon" onClick={sendMessage} disabled={!input.trim() || streaming}>
              {streaming ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Send className="h-4 w-4" />
              )}
            </Button>
          </div>
        </div>
      </div>

      <ConfirmationDialog
        open={!!deleteId}
        onCancel={() => setDeleteId(null)}
        title="Session löschen"
        description="Möchten Sie diese Chat-Session wirklich löschen?"
        onConfirm={handleDeleteSession}
        confirmLabel="Löschen"
        variant="destructive"
      />
    </div>
  );
}
