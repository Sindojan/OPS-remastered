"use client";

import { useEffect, useRef, useState, useCallback, useMemo } from "react";
import { Button } from "@/components/ui/button";
import { Bot, X, Send, Loader2, Plus, List, Trash2, History } from "lucide-react";
import { usePrimaryAgent } from "@/hooks/use-primary-agent";
import { useApi } from "@/hooks/api/use-api";
import { MarkdownMessage } from "@/components/chat/markdown-message";
import {
  DelegationCard,
  LEAD_LABELS,
  formatTokens,
  truncate,
  type ChatMessage,
  type LeadStepInfo,
  type TokenUsage,
} from "@/components/chat/delegation-card";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import type { ChatSessionResponse, LlmConfig, AgentRunDetail } from "@/types/api";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

/** Override config for system-agent usage (different API paths, fixed agent). */
export interface AgentPanelConfig {
  /** Agent display name */
  agentName: string;
  /** Agent instance ID */
  agentId: string;
  /** Model display string */
  model: string;
  /** API path for chat sessions, e.g. "/api/system/chat/sessions" */
  sessionsPath: string;
  /** API path for sending a message (POST), e.g. "/api/system/chat/message" */
  messagePath: string;
  /** API path for last-run, e.g. "/api/system/agents/{id}/last-run" */
  lastRunPath: string;
  /** Greeting message */
  greeting: string;
  /** Placeholder text */
  placeholder: string;
}

interface AgentPanelProps {
  open: boolean;
  onClose: () => void;
  /** If provided, skips usePrimaryAgent/useApi and uses these fixed values instead. */
  config?: AgentPanelConfig;
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function AgentPanel({ open, onClose, config }: AgentPanelProps) {
  const { agent: primaryAgent } = usePrimaryAgent();
  const { data: llmConfig } = useApi<LlmConfig>(config ? null : "/api/settings/llm");

  // Resolve agent info: use config override or primary agent
  const agentName = config?.agentName ?? primaryAgent?.name ?? "Agent";
  const agentId = config?.agentId ?? primaryAgent?.id;
  const agentModel = config?.model ?? llmConfig?.defaultModel;
  const sessionsPath = config?.sessionsPath ?? "/api/chat/sessions";
  const messagePath = config?.messagePath ?? "/api/chat/message";
  const lastRunPath = config ? config.lastRunPath : (agentId ? `/api/agent-instances/${agentId}/last-run` : null);
  const greetingText = config?.greeting ?? (agentName ? `Guten Tag! Ich bin Ihr ${agentName}. Wie kann ich Ihnen helfen?` : "Guten Tag! Wie kann ich Ihnen helfen?");
  const placeholderText = config?.placeholder ?? "Nachricht eingeben...";

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
  const [showSessionList, setShowSessionList] = useState(false);
  const [sessions, setSessions] = useState<ChatSessionResponse[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ChatSessionResponse | null>(
    null
  );
  const [showLastRun, setShowLastRun] = useState(false);
  const [lastRun, setLastRun] = useState<AgentRunDetail | null>(null);
  const [lastRunLoading, setLastRunLoading] = useState(false);

  const scrollRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const sessionTokenTotal = useMemo(() => {
    return messages.reduce((sum, m) => {
      if (m.usage) return sum + m.usage.inputTokens + m.usage.outputTokens;
      return sum;
    }, 0);
  }, [messages]);

  const greetingMessage = useCallback((): ChatMessage => {
    return {
      role: "assistant",
      content: greetingText,
    };
  }, [greetingText]);

  const fetchSessions = useCallback(async () => {
    const token = localStorage.getItem("owlsburg_token");
    if (!token) return;
    setSessionsLoading(true);
    try {
      const res = await fetch(`${API_BASE}${sessionsPath}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const json = await res.json();
      if (json.success) {
        setSessions(json.data);
      }
    } catch {
      // silently fail
    } finally {
      setSessionsLoading(false);
    }
  }, [sessionsPath]);

  const loadSession = useCallback(
    async (sessionId: string) => {
      const token = localStorage.getItem("owlsburg_token");
      if (!token) return;
      try {
        const res = await fetch(
          `${API_BASE}${sessionsPath}/${sessionId}/messages`,
          { headers: { Authorization: `Bearer ${token}` } }
        );
        const json = await res.json();
        if (json.success) {
          const loaded: ChatMessage[] = json.data.map(
            (m: { role: "user" | "assistant"; content: string }) => ({
              role: m.role,
              content: m.content,
            })
          );
          setMessages(loaded.length > 0 ? loaded : [greetingMessage()]);
          setCurrentSessionId(sessionId);
          setShowSessionList(false);
        }
      } catch {
        // silently fail
      }
    },
    [greetingMessage]
  );

  // Load sessions and auto-load most recent on panel open
  useEffect(() => {
    if (open && agentId) {
      fetchSessions().then(() => {
        // We fetch sessions above, but we need to auto-load in a then-chain
        // because setSessions is async. Use a separate effect instead.
      });
    }
    if (!open) {
      setShowSessionList(false);
    }
  }, [open, agentId, fetchSessions]);

  // Auto-load most recent session once sessions are fetched, but only on panel open
  const hasAutoLoaded = useRef(false);
  useEffect(() => {
    if (!open) {
      hasAutoLoaded.current = false;
      return;
    }
    if (hasAutoLoaded.current) return;
    if (sessionsLoading) return;

    hasAutoLoaded.current = true;
    if (sessions.length > 0) {
      loadSession(sessions[0].id);
    } else {
      setMessages([greetingMessage()]);
      setCurrentSessionId(null);
    }
  }, [open, sessions, sessionsLoading, loadSession, greetingMessage]);

  // Auto-scroll on new messages
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  // Auto-resize textarea
  const resizeTextarea = () => {
    const ta = textareaRef.current;
    if (ta) {
      ta.style.height = "auto";
      ta.style.height = `${Math.min(ta.scrollHeight, 120)}px`;
    }
  };

  const handleNewSession = () => {
    setCurrentSessionId(null);
    setMessages([greetingMessage()]);
    setShowSessionList(false);
    setInput("");
  };

  const handleDeleteSession = async () => {
    if (!deleteTarget) return;
    const token = localStorage.getItem("owlsburg_token");
    if (!token) return;
    try {
      await fetch(
        `${API_BASE}${sessionsPath}/${deleteTarget.id}`,
        {
          method: "DELETE",
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      // If we deleted the active session, start fresh
      if (currentSessionId === deleteTarget.id) {
        handleNewSession();
      }
      await fetchSessions();
    } catch {
      // silently fail
    } finally {
      setDeleteTarget(null);
    }
  };

  const fetchLastRun = useCallback(async () => {
    if (!lastRunPath) return;
    const token = localStorage.getItem("owlsburg_token");
    if (!token) return;
    setLastRunLoading(true);
    try {
      const res = await fetch(`${API_BASE}${lastRunPath}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const json = await res.json();
      if (json.success && json.data) {
        setLastRun(json.data);
        setShowLastRun(true);
      } else {
        setLastRun(null);
        setShowLastRun(true);
      }
    } catch {
      setLastRun(null);
    } finally {
      setLastRunLoading(false);
    }
  }, [lastRunPath]);

  const sendMessage = async () => {
    if (!input.trim() || isStreaming) return;

    const userMsg = input.trim();

    setMessages((prev) => [...prev, { role: "user", content: userMsg }]);
    setInput("");
    setIsStreaming(true);

    // Reset textarea height
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
    }

    // Add empty assistant message for streaming
    setMessages((prev) => [...prev, { role: "assistant", content: "" }]);

    try {
      const token = localStorage.getItem("owlsburg_token");
      const response = await fetch(`${API_BASE}${messagePath}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          message: userMsg,
          agentInstanceId: agentId,
          sessionId: currentSessionId,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const reader = response.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() || "";

        for (const line of lines) {
          if (!line.startsWith("data:")) continue;
          const jsonStr = line.substring(5).trim();
          if (!jsonStr) continue;

          try {
            const data = JSON.parse(jsonStr);

            if (data.sessionId) {
              setCurrentSessionId(data.sessionId);
            }

            if (data.token) {
              setMessages((prev) => {
                const updated = [...prev];
                const last = { ...updated[updated.length - 1] };
                last.content += data.token;
                updated[updated.length - 1] = last;
                return updated;
              });
            }

            if (data.delegation) {
              // Show delegation event with ID for matching
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
              // Accumulate leadStep into the matching delegation message
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
                  if (
                    updated[i].role === "tool" &&
                    updated[i].delegationId === matchId
                  ) {
                    const msg = { ...updated[i] };
                    msg.leadSteps = [...(msg.leadSteps || []), step];
                    updated[i] = msg;
                    break;
                  }
                }
                return updated;
              });
            }

            if (data.delegationResult) {
              // Update the delegation message with result, match by ID
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
                    const msg = { ...updated[i] };
                    const stepCount = msg.leadSteps?.length || 0;
                    msg.content = `**${label}** hat geantwortet` + (stepCount > 0 ? ` (${stepCount} Schritte)` : "");
                    const toolCalls = [...(msg.toolCalls || [])];
                    toolCalls[0] = { ...toolCalls[0], result: data.delegationResult.result };
                    msg.toolCalls = toolCalls;
                    updated[i] = msg;
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
              // Add tool call info as a separate message
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
              // Update the last tool message with result
              setMessages((prev) => {
                const updated = [...prev];
                // Find the last tool message for this tool
                for (let i = updated.length - 1; i >= 0; i--) {
                  if (
                    updated[i].role === "tool" &&
                    updated[i].toolCalls?.[0]?.name === data.toolResult.name
                  ) {
                    const msg = { ...updated[i] };
                    const toolCalls = [...(msg.toolCalls || [])];
                    toolCalls[0] = { ...toolCalls[0], result: data.toolResult.result };
                    msg.toolCalls = toolCalls;
                    msg.content = `\u{2705} **${data.toolResult.name}** abgeschlossen`;
                    updated[i] = msg;
                    break;
                  }
                }
                // Add empty assistant message for next response
                if (updated[updated.length - 1]?.role !== "assistant") {
                  updated.push({ role: "assistant", content: "" });
                }
                return updated;
              });
            }

            if (data.usage) {
              // Attach usage to the last assistant message
              setMessages((prev) => {
                const updated = [...prev];
                for (let i = updated.length - 1; i >= 0; i--) {
                  if (updated[i].role === "assistant" && updated[i].content) {
                    updated[i] = { ...updated[i], usage: data.usage };
                    break;
                  }
                }
                return updated;
              });
            }

            if (data.error) {
              setMessages((prev) => {
                const updated = [...prev];
                updated[updated.length - 1] = {
                  role: "assistant",
                  content: `Fehler: ${data.error}`,
                };
                return updated;
              });
            }

            if (data.done) {
              // Remove empty trailing assistant messages
              setMessages((prev) => {
                if (prev[prev.length - 1]?.role === "assistant" && !prev[prev.length - 1]?.content) {
                  return prev.slice(0, -1);
                }
                return prev;
              });
              // Refresh session list to update titles
              fetchSessions();
            }
          } catch {
            // Ignore parse errors for incomplete JSON
          }
        }
      }
    } catch {
      setMessages((prev) => {
        const updated = [...prev];
        if (
          updated[updated.length - 1]?.role === "assistant" &&
          !updated[updated.length - 1]?.content
        ) {
          updated[updated.length - 1] = {
            role: "assistant",
            content:
              "Verbindungsfehler. Bitte prüfen Sie die LLM-Konfiguration.",
          };
        }
        return updated;
      });
    } finally {
      setIsStreaming(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  if (!open) return null;

  return (
    <aside className="flex h-full w-[380px] shrink-0 flex-col border-l border-border bg-background">
      {/* Header with gradient accent */}
      <div className="relative flex h-12 items-center justify-between border-b border-border px-4">
        <div className="absolute inset-x-0 top-0 h-[2px] bg-gradient-to-r from-primary/0 via-primary/60 to-primary/0" />
        <div className="flex items-center gap-2">
          <div className="flex h-6 w-6 items-center justify-center rounded-md bg-primary/10">
            <Bot className="h-3.5 w-3.5 text-primary" />
          </div>
          <div className="flex flex-col">
            <span className="text-[13px] font-semibold leading-tight">
              {agentName}
            </span>
            <div className="flex items-center gap-2">
              {agentModel && (
                <span className="text-[10px] text-muted-foreground leading-tight font-mono">
                  {agentModel}
                </span>
              )}
              {sessionTokenTotal > 0 && (
                <span className="text-[10px] text-muted-foreground/60 font-mono">
                  {formatTokens(sessionTokenTotal)} tok
                </span>
              )}
            </div>
          </div>
        </div>
        <div className="flex items-center gap-0.5">
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={fetchLastRun}
            title="Letzter Run"
            disabled={lastRunLoading}
            className={showLastRun ? "bg-muted" : ""}
          >
            {lastRunLoading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <History className="h-3.5 w-3.5" />}
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={handleNewSession}
            title="Neue Konversation"
          >
            <Plus className="h-3.5 w-3.5" />
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => setShowSessionList((v) => !v)}
            title="Konversationen"
            className={showSessionList ? "bg-muted" : ""}
          >
            <List className="h-3.5 w-3.5" />
          </Button>
          <Button variant="ghost" size="icon-sm" onClick={onClose}>
            <X className="h-3.5 w-3.5" />
          </Button>
        </div>
      </div>

      {/* Session list dropdown */}
      {showSessionList && (
        <div className="max-h-[240px] overflow-y-auto border-b border-border">
          <div className="px-3 py-2 text-xs font-medium text-muted-foreground">
            Letzte Konversationen
          </div>
          {sessionsLoading ? (
            <div className="flex items-center justify-center py-4">
              <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
            </div>
          ) : sessions.length === 0 ? (
            <div className="px-3 py-4 text-center text-xs text-muted-foreground">
              Keine bisherigen Konversationen
            </div>
          ) : (
            sessions.map((session) => (
              <div
                key={session.id}
                className={`group flex cursor-pointer items-start gap-2 border-b border-border p-3 last:border-0 hover:bg-muted/50 ${
                  currentSessionId === session.id
                    ? "border-l-2 border-l-primary bg-primary/5"
                    : ""
                }`}
                onClick={() => loadSession(session.id)}
              >
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-medium">
                    {session.title || "Neue Konversation"}
                  </div>
                  <div className="text-xs text-muted-foreground">
                    {formatDate(session.updatedAt)}
                  </div>
                </div>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  className="shrink-0 opacity-0 hover:text-destructive group-hover:opacity-100"
                  onClick={(e) => {
                    e.stopPropagation();
                    setDeleteTarget(session);
                  }}
                >
                  <Trash2 className="h-3 w-3" />
                </Button>
              </div>
            ))
          )}
        </div>
      )}

      {/* Last Run panel */}
      {showLastRun && (
        <div className="max-h-[280px] overflow-y-auto border-b border-border">
          <div className="flex items-center justify-between px-3 py-2">
            <span className="text-xs font-medium text-muted-foreground">Letzter Run</span>
            <Button variant="ghost" size="icon-sm" onClick={() => setShowLastRun(false)}>
              <X className="h-3 w-3" />
            </Button>
          </div>
          {!lastRun ? (
            <div className="px-3 py-4 text-center text-xs text-muted-foreground">
              Kein letzter Run vorhanden
            </div>
          ) : (
            <div className="px-3 pb-3 space-y-2">
              <div className="flex items-center gap-2 text-xs">
                <span className={`inline-block h-1.5 w-1.5 rounded-full ${
                  lastRun.status === "SUCCESS" ? "bg-success" : lastRun.status === "FAILED" ? "bg-destructive" : "bg-warning"
                }`} />
                <span className="font-mono">{lastRun.status}</span>
                <span className="text-muted-foreground">{formatTokens(lastRun.tokensUsed)} tok</span>
                {lastRun.completedAt && (
                  <span className="text-muted-foreground">{formatDate(lastRun.completedAt)}</span>
                )}
              </div>
              {lastRun.errorMessage && (
                <div className="text-xs text-destructive bg-destructive/10 rounded px-2 py-1">
                  {lastRun.errorMessage}
                </div>
              )}
              {lastRun.steps && lastRun.steps.length > 0 && (
                <div className="space-y-1">
                  <span className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider">
                    Schritte ({lastRun.steps.length})
                  </span>
                  {lastRun.steps.map((step) => (
                    <div key={step.id} className="rounded border border-border/50 bg-muted/30 px-2 py-1.5 text-[11px] font-mono">
                      <div className="flex items-center gap-2">
                        <span className="text-primary/80 font-semibold">{step.type}</span>
                        {step.toolName && <span className="text-muted-foreground">{step.toolName}</span>}
                        {step.durationMs != null && (
                          <span className="text-muted-foreground/60">{step.durationMs}ms</span>
                        )}
                        {step.tokensUsed > 0 && (
                          <span className="text-muted-foreground/60">{formatTokens(step.tokensUsed)} tok</span>
                        )}
                      </div>
                      {step.output && (
                        <pre className="mt-0.5 overflow-x-auto text-[10px] text-muted-foreground whitespace-pre-wrap">
                          {truncate(step.output, 200)}
                        </pre>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Messages area */}
      <div ref={scrollRef} className="flex-1 space-y-3 overflow-y-auto p-4">
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
                    isStreaming && i === messages.length - 1
                      ? msg.content + " \u258B"
                      : msg.content
                  }
                />
                {msg.usage && (
                  <div className="mt-1.5 text-[10px] text-muted-foreground/60 font-mono">
                    {"\u2191"}{formatTokens(msg.usage.inputTokens)} {"\u2193"}{formatTokens(msg.usage.outputTokens)}
                  </div>
                )}
              </>
            ) : msg.role === "tool" ? (
              msg.delegationId != null ? (
                <DelegationCard msg={msg} isStreaming={isStreaming} />
              ) : (
                <MarkdownMessage content={msg.content} />
              )
            ) : (
              <span className="whitespace-pre-wrap">{msg.content}</span>
            )}
          </div>
        ))}
      </div>

      {/* Input area */}
      <div className="flex items-end gap-2 border-t border-border p-3">
        <textarea
          ref={textareaRef}
          value={input}
          onChange={(e) => {
            setInput(e.target.value);
            resizeTextarea();
          }}
          onKeyDown={handleKeyDown}
          placeholder={placeholderText}
          rows={1}
          className="flex-1 resize-none rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
          style={{ minHeight: "40px", maxHeight: "120px" }}
          disabled={isStreaming}
        />
        <Button
          size="icon"
          onClick={sendMessage}
          disabled={!input.trim() || isStreaming}
          className="shrink-0"
        >
          {isStreaming ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Send className="h-4 w-4" />
          )}
        </Button>
      </div>

      {/* Delete confirmation dialog */}
      <ConfirmationDialog
        open={!!deleteTarget}
        title="Konversation löschen"
        description={`Möchten Sie die Konversation "${deleteTarget?.title || "Neue Konversation"}" wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.`}
        variant="destructive"
        confirmLabel="Löschen"
        cancelLabel="Abbrechen"
        onConfirm={handleDeleteSession}
        onCancel={() => setDeleteTarget(null)}
      />
    </aside>
  );
}
