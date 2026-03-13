"use client";

import { useState } from "react";
import { ChevronRight, ChevronDown } from "lucide-react";
import { MarkdownMessage } from "@/components/chat/markdown-message";

export interface ToolCallInfo {
  name: string;
  input: string;
  result?: string;
}

export interface LeadStepInfo {
  type: "reasoning" | "tool_call" | "tool_result";
  toolName?: string;
  content: string;
  iteration: number;
}

export interface TokenUsage {
  inputTokens: number;
  outputTokens: number;
}

export interface ChatMessage {
  role: "user" | "assistant" | "tool";
  content: string;
  toolCalls?: ToolCallInfo[];
  leadSteps?: LeadStepInfo[];
  delegationId?: string;
  usage?: TokenUsage;
}

export const LEAD_LABELS: Record<string, string> = {
  produktions_lead: "Produktions-Lead",
  maschinen_lead: "Maschinen-Lead",
  lager_lead: "Lager-Lead",
  personal_lead: "Personal-Lead",
  support_lead: "Support-Lead",
};

export function formatTokens(n: number): string {
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, "") + "K";
  return String(n);
}

export function truncate(text: string, maxLen: number): string {
  if (text.length <= maxLen) return text;
  return text.slice(0, maxLen) + "...";
}

export function DelegationCard({ msg, isStreaming }: { msg: ChatMessage; isStreaming: boolean }) {
  const [expanded, setExpanded] = useState(false);
  const hasResult = !!msg.toolCalls?.[0]?.result;
  const steps = msg.leadSteps || [];
  const hasSteps = steps.length > 0;

  return (
    <div>
      <div
        className={`flex items-center gap-1.5 ${hasSteps ? "cursor-pointer select-none" : ""}`}
        onClick={() => hasSteps && setExpanded((v) => !v)}
      >
        {hasSteps ? (
          expanded ? (
            <ChevronDown className="h-3 w-3 shrink-0" />
          ) : (
            <ChevronRight className="h-3 w-3 shrink-0" />
          )
        ) : null}
        <MarkdownMessage content={
          (!hasResult && isStreaming ? "\u{23F3} " : hasResult ? "\u{2705} " : "\u{1F4E4} ") + msg.content
        } />
      </div>
      {expanded && hasSteps && (
        <div className="mt-2 ml-4 space-y-1.5 border-l-2 border-border/50 pl-3">
          {steps.map((step, j) => (
            <div key={j} className="text-[11px] leading-relaxed">
              {step.type === "reasoning" ? (
                <span className="italic text-muted-foreground">{step.content}</span>
              ) : step.type === "tool_call" ? (
                <div>
                  <span className="font-semibold text-primary/80">{step.toolName}</span>
                  <pre className="mt-0.5 overflow-x-auto rounded bg-muted/50 px-1.5 py-0.5 text-[10px]">
                    {truncate(step.content, 200)}
                  </pre>
                </div>
              ) : step.type === "tool_result" ? (
                <div>
                  <span className="text-muted-foreground/70">{step.toolName} Ergebnis:</span>
                  <pre className="mt-0.5 overflow-x-auto rounded bg-muted/50 px-1.5 py-0.5 text-[10px]">
                    {truncate(step.content, 300)}
                  </pre>
                </div>
              ) : null}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
