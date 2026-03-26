"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import type { AgentActivitySnapshot, AgentActivityEvent } from "@/types/api";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const RECONNECT_NORMAL_MS = 3000;
const RECONNECT_MAX_MS = 30000;
const MAX_LOG_PER_AGENT = 50;

export function useSystemAgentActivity(): {
  snapshot: AgentActivitySnapshot | null;
  realtimeEvents: Map<string, AgentActivityEvent>;
  activityLog: Map<string, AgentActivityEvent[]>;
  connected: boolean;
} {
  const [snapshot, setSnapshot] = useState<AgentActivitySnapshot | null>(null);
  const [realtimeEvents, setRealtimeEvents] = useState<Map<string, AgentActivityEvent>>(new Map());
  const [activityLog, setActivityLog] = useState<Map<string, AgentActivityEvent[]>>(new Map());
  const [connected, setConnected] = useState(false);
  const abortRef = useRef(false);
  const retryCountRef = useRef(0);

  const connect = useCallback(async () => {
    if (abortRef.current) return;

    const token = localStorage.getItem("owlsburg_token");
    if (!token) {
      setTimeout(() => {
        if (!abortRef.current) connect();
      }, 2000);
      return;
    }

    let hadSuccessfulConnection = false;

    try {
      const response = await fetch(
        `${API_BASE}/api/system/agent-activity/stream`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      if (!response.ok || !response.body) {
        setConnected(false);
        const delay = Math.min(1000 * Math.pow(2, retryCountRef.current), RECONNECT_MAX_MS);
        retryCountRef.current++;
        setTimeout(() => {
          if (!abortRef.current) connect();
        }, delay);
        return;
      }

      setConnected(true);
      retryCountRef.current = 0;
      hadSuccessfulConnection = true;

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      let eventType = "";
      let dataLines: string[] = [];

      while (!abortRef.current) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() || "";

        for (const rawLine of lines) {
          const line = rawLine.trimEnd();

          if (line === "") {
            if (dataLines.length > 0) {
              const jsonStr = dataLines.join("\n").trim();
              if (jsonStr) {
                dispatchSSEEvent(eventType || "message", jsonStr);
              }
            }
            eventType = "";
            dataLines = [];
          } else if (line.startsWith("event:")) {
            eventType = line.slice(6).trim();
          } else if (line.startsWith("data:")) {
            dataLines.push(line.slice(5).trimStart());
          } else if (line.startsWith(":")) {
            // heartbeat
          }
        }
      }

      const remaining = decoder.decode();
      if (remaining) {
        buffer += remaining;
      }
    } catch {
      // Connection error
    } finally {
      if (!abortRef.current) {
        setConnected(false);
        const delay = hadSuccessfulConnection
          ? RECONNECT_NORMAL_MS
          : Math.min(1000 * Math.pow(2, retryCountRef.current), RECONNECT_MAX_MS);
        if (!hadSuccessfulConnection) {
          retryCountRef.current++;
        }
        setTimeout(() => {
          if (!abortRef.current) connect();
        }, delay);
      }
    }

    function dispatchSSEEvent(type: string, jsonStr: string) {
      if (abortRef.current) return;

      try {
        if (type === "snapshot" || type === "message") {
          const parsed = JSON.parse(jsonStr) as AgentActivitySnapshot;
          setSnapshot(parsed);
        } else if (type === "activity") {
          const event = JSON.parse(jsonStr) as AgentActivityEvent;
          setRealtimeEvents((prev) => {
            const next = new Map(prev);
            if (event.type === "IDLE") {
              next.delete(event.agentInstanceId);
            } else {
              next.set(event.agentInstanceId, event);
            }
            if (event.type === "DELEGATION_START" && event.targetInstanceId) {
              next.set(event.targetInstanceId, {
                ...event,
                type: "THINKING",
                agentInstanceId: event.targetInstanceId,
                targetInstanceId: null,
              });
            } else if (event.type === "DELEGATION_END" && event.targetInstanceId) {
              next.delete(event.targetInstanceId);
            }
            return next;
          });
          setActivityLog((prev) => {
            const next = new Map(prev);
            const agentId = event.agentInstanceId;
            const existing = next.get(agentId) || [];
            const updated = [...existing, event];
            next.set(agentId, updated.length > MAX_LOG_PER_AGENT
              ? updated.slice(-MAX_LOG_PER_AGENT)
              : updated);
            if (event.targetInstanceId && (event.type === "DELEGATION_START" || event.type === "DELEGATION_END")) {
              const targetExisting = next.get(event.targetInstanceId) || [];
              const targetUpdated = [...targetExisting, event];
              next.set(event.targetInstanceId, targetUpdated.length > MAX_LOG_PER_AGENT
                ? targetUpdated.slice(-MAX_LOG_PER_AGENT)
                : targetUpdated);
            }
            return next;
          });
        }
      } catch {
        // Ignore JSON parse errors
      }
    }
  }, []);

  useEffect(() => {
    abortRef.current = false;
    retryCountRef.current = 0;
    connect();
    return () => {
      abortRef.current = true;
    };
  }, [connect]);

  return { snapshot, realtimeEvents, activityLog, connected };
}
