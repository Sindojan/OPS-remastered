"use client";

import { Button } from "@/components/ui/button";
import { Bot, X, Sparkles } from "lucide-react";

interface AgentPanelProps {
  open: boolean;
  onClose: () => void;
}

export function AgentPanel({ open, onClose }: AgentPanelProps) {
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
          <span className="text-[13px] font-semibold">CEO Agent</span>
        </div>
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={onClose}
        >
          <X className="h-3.5 w-3.5" />
        </Button>
      </div>

      {/* Empty state */}
      <div className="flex flex-1 flex-col items-center justify-center gap-3 p-6">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/5 ring-1 ring-primary/10">
          <Sparkles className="h-5 w-5 text-primary/50" />
        </div>
        <div className="text-center">
          <p className="text-sm font-medium text-foreground/70">
            Agent Panel
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            Context-aware AI assistance
          </p>
        </div>
      </div>
    </aside>
  );
}
