"use client";

import { useEffect, useRef, useState, useMemo } from "react";
import { AgentNodeCard } from "./agent-node-card";
import type { AgentInstanceActivity, ActiveLink, AgentActivityEvent } from "@/types/api";

interface HierarchyCanvasProps {
  instances: AgentInstanceActivity[];
  activeLinks: ActiveLink[];
  realtimeEvents: Map<string, AgentActivityEvent>;
  selectedAgentId: string | null;
  onSelectAgent: (id: string) => void;
}

interface Line {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  active: boolean;
}

export function HierarchyCanvas({ instances, activeLinks, realtimeEvents, selectedAgentId, onSelectAgent }: HierarchyCanvasProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const cardRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const [lines, setLines] = useState<Line[]>([]);
  const [svgSize, setSvgSize] = useState({ width: 0, height: 0 });

  const activeLinkKeys = useMemo(
    () => activeLinks.map((l) => `${l.senderInstanceId}->${l.targetInstanceId}`),
    [activeLinks]
  );
  const activeLinkSet = useMemo(() => new Set(activeLinkKeys), [activeLinkKeys]);
  const activeLinkSetReverse = useMemo(
    () => new Set(activeLinks.map((l) => `${l.targetInstanceId}->${l.senderInstanceId}`)),
    [activeLinks]
  );

  // Build set of realtime-active links from DELEGATION_START events
  const realtimeLinkSet = useMemo(() => {
    const set = new Set<string>();
    for (const [, event] of realtimeEvents) {
      if (event.type === "DELEGATION_START" && event.targetInstanceId) {
        set.add(`${event.agentInstanceId}->${event.targetInstanceId}`);
        set.add(`${event.targetInstanceId}->${event.agentInstanceId}`);
      }
    }
    return set;
  }, [realtimeEvents]);

  const ceo = instances.find((i) => i.templateRole === "ceo");
  const leads = instances.filter(
    (i) => i.type === "PERSISTENT" && i.templateRole !== "ceo"
  );
  const subAgents = instances.filter((i) => i.type === "EPHEMERAL");

  const nameMap = useMemo(() => {
    const map: Record<string, string> = {};
    for (const inst of instances) {
      map[inst.id] = inst.name;
    }
    return map;
  }, [instances]);

  function computeLines() {
    const container = containerRef.current;
    if (!container) return;
    const containerRect = container.getBoundingClientRect();

    setSvgSize({ width: containerRect.width, height: containerRect.height });

    const newLines: Line[] = [];

    for (const inst of instances) {
      if (!inst.parentInstanceId) continue;
      const parentEl = cardRefs.current[inst.parentInstanceId];
      const childEl = cardRefs.current[inst.id];
      if (!parentEl || !childEl) continue;

      const parentRect = parentEl.getBoundingClientRect();
      const childRect = childEl.getBoundingClientRect();

      const x1 = parentRect.left + parentRect.width / 2 - containerRect.left;
      const y1 = parentRect.bottom - containerRect.top;
      const x2 = childRect.left + childRect.width / 2 - containerRect.left;
      const y2 = childRect.top - containerRect.top;

      const key = `${inst.parentInstanceId}->${inst.id}`;
      const isActive = activeLinkSet.has(key) || activeLinkSetReverse.has(key)
        || realtimeLinkSet.has(key);

      newLines.push({ x1, y1, x2, y2, active: isActive });
    }

    setLines(newLines);
  }

  // Recompute on data changes — double rAF to ensure layout is fully settled
  useEffect(() => {
    const id = requestAnimationFrame(() => {
      requestAnimationFrame(computeLines);
    });
    return () => cancelAnimationFrame(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [instances, activeLinkSet, activeLinkSetReverse, realtimeLinkSet]);

  // Recompute on resize
  useEffect(() => {
    const observer = new ResizeObserver(() => computeLines());
    if (containerRef.current) {
      observer.observe(containerRef.current);
    }
    return () => observer.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function isAgentActive(agentId: string): boolean {
    return activeLinkKeys.some((k) => k.includes(agentId)) || realtimeEvents.has(agentId);
  }

  return (
    <div ref={containerRef} className="relative flex flex-col items-center gap-16 py-10">
      {/* SVG connection lines — rendered first so cards paint on top */}
      {lines.length > 0 && (
        <svg
          className="pointer-events-none absolute inset-0 z-0"
          width={svgSize.width}
          height={svgSize.height}
          style={{ overflow: "visible" }}
        >
          {lines.map((line, i) => (
            <line
              key={i}
              x1={line.x1}
              y1={line.y1}
              x2={line.x2}
              y2={line.y2}
              className={line.active ? "stroke-primary" : "stroke-muted-foreground"}
              strokeWidth={line.active ? 4 : 2.5}
              strokeOpacity={line.active ? 1 : 0.25}
              strokeDasharray={line.active ? "10 6" : undefined}
              strokeLinecap="round"
              style={line.active ? { animation: "line-flow 0.6s linear infinite" } : undefined}
            />
          ))}
        </svg>
      )}

      {/* Level 1: CEO */}
      {ceo && (
        <div className="relative z-10 flex justify-center">
          <div ref={(el) => { cardRefs.current[ceo.id] = el; }}>
            <AgentNodeCard
              agent={ceo}
              isActive={isAgentActive(ceo.id)}
              isSelected={selectedAgentId === ceo.id}
              onSelect={onSelectAgent}
              realtimeEvent={realtimeEvents.get(ceo.id)}
            />
          </div>
        </div>
      )}

      {/* Level 2: Leads */}
      {leads.length > 0 && (
        <div className="relative z-10 flex flex-wrap items-start justify-center gap-6">
          {leads.map((lead) => (
            <div key={lead.id} ref={(el) => { cardRefs.current[lead.id] = el; }}>
              <AgentNodeCard
                agent={lead}
                parentName={lead.parentInstanceId ? nameMap[lead.parentInstanceId] : undefined}
                isActive={isAgentActive(lead.id)}
                isSelected={selectedAgentId === lead.id}
                onSelect={onSelectAgent}
                realtimeEvent={realtimeEvents.get(lead.id)}
              />
            </div>
          ))}
        </div>
      )}

      {/* Level 3: Sub-Agents (ephemeral) */}
      {subAgents.length > 0 && (
        <div className="relative z-10 flex flex-wrap items-start justify-center gap-5">
          {subAgents.map((sub) => (
            <div key={sub.id} ref={(el) => { cardRefs.current[sub.id] = el; }}>
              <AgentNodeCard
                agent={sub}
                parentName={sub.parentInstanceId ? nameMap[sub.parentInstanceId] : undefined}
                isActive={isAgentActive(sub.id)}
                isSelected={selectedAgentId === sub.id}
                onSelect={onSelectAgent}
                realtimeEvent={realtimeEvents.get(sub.id)}
              />
            </div>
          ))}
        </div>
      )}

      {instances.length === 0 && (
        <div className="py-12 text-center text-sm text-muted-foreground">
          Keine Agent-Instanzen gefunden.
        </div>
      )}
    </div>
  );
}
