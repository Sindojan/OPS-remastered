"use client";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DomainStatusBadge } from "@/components/shared/domain-status-badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import {
  Database,
  Plug,
  Users,
  Package,
  ShoppingCart,
  Truck,
  Warehouse,
  Factory,
  UserCheck,
  Settings,
} from "lucide-react";
import { useOdooConfig } from "@/hooks/api/use-odoo";
import Link from "next/link";

const ODOO_DATA_TYPES = [
  { label: "Partner / Kunden", icon: Users, model: "res.partner", description: "Kunden, Lieferanten und Kontakte" },
  { label: "Produkte", icon: Package, model: "product.product", description: "Artikelstamm mit Preisen und Beständen" },
  { label: "Verkaufsaufträge", icon: ShoppingCart, model: "sale.order", description: "Angebote und Auftragsbestätigungen" },
  { label: "Einkaufsbestellungen", icon: Truck, model: "purchase.order", description: "Bestellungen an Lieferanten" },
  { label: "Lagerbestände", icon: Warehouse, model: "stock.quant", description: "Aktuelle Bestände pro Lagerort" },
  { label: "Fertigung", icon: Factory, model: "mrp.production", description: "Fertigungsaufträge (MRP-Modul)" },
  { label: "Mitarbeiter", icon: UserCheck, model: "hr.employee", description: "Personalstammdaten (HR-Modul)" },
];

export default function OdooPage() {
  const { data: config, loading } = useOdooConfig();

  const isConnected = config?.connectionStatus === "CONNECTED";
  const isConfigured = config?.connectionStatus !== "UNCONFIGURED" && config?.hasApiKey;

  const statusVariant = isConnected ? "success" : config?.connectionStatus === "ERROR" ? "error" : config?.connectionStatus === "PENDING" ? "warning" : "neutral";
  const statusLabel = isConnected ? "Verbunden" : config?.connectionStatus === "ERROR" ? "Fehler" : config?.connectionStatus === "PENDING" ? "Noch nicht getestet" : "Nicht konfiguriert";

  return (
    <div className="space-y-6">
      <PageHeader
        title="Odoo-Integration"
        description="Verbindung zu Odoo 19+ ERP für Datenabfragen durch Agents"
        breadcrumb={["Betrieb", "Odoo"]}
        actions={
          <DomainStatusBadge variant={statusVariant}>
            {statusLabel}
          </DomainStatusBadge>
        }
      />

      {loading ? (
        <div className="space-y-4">
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>
      ) : !isConfigured ? (
        <Card className="border-border/50">
          <CardContent className="flex flex-col items-center gap-4 py-12">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-muted">
              <Database className="h-6 w-6 text-muted-foreground" />
            </div>
            <div className="text-center">
              <h3 className="text-sm font-medium">Odoo noch nicht konfiguriert</h3>
              <p className="mt-1 text-sm text-muted-foreground">
                Verbindungsdaten in den Einstellungen hinterlegen, um Odoo-Daten abfragen zu können.
              </p>
            </div>
            <Button asChild variant="outline" className="gap-2">
              <Link href="/settings">
                <Settings className="h-4 w-4" />
                Zu den Einstellungen
              </Link>
            </Button>
          </CardContent>
        </Card>
      ) : (
        <>
          {/* Connection Info */}
          <Card className="border-border/50">
            <CardHeader className="pb-4">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base flex items-center gap-2">
                  <Plug className="h-4 w-4" />
                  Verbindung
                </CardTitle>
                <DomainStatusBadge variant={statusVariant}>
                  {statusLabel}
                </DomainStatusBadge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4 sm:grid-cols-3">
                <div>
                  <p className="text-xs text-muted-foreground">URL</p>
                  <p className="font-mono text-sm">{config?.baseUrl}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Datenbank</p>
                  <p className="font-mono text-sm">{config?.databaseName}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Version</p>
                  <p className="font-mono text-sm">{config?.odooVersion}</p>
                </div>
              </div>
              {config?.lastConnectedAt && (
                <p className="mt-3 text-xs text-muted-foreground">
                  Letzte Verbindung: {new Date(config.lastConnectedAt).toLocaleString("de-DE")}
                </p>
              )}
            </CardContent>
          </Card>

          {/* Available Data Types */}
          <div>
            <h2 className="mb-3 text-sm font-medium text-muted-foreground">
              Verfügbare Datentypen
            </h2>
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {ODOO_DATA_TYPES.map((dt) => {
                const Icon = dt.icon;
                return (
                  <Card key={dt.model} className="border-border/50">
                    <CardContent className="flex items-start gap-3 p-4">
                      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-primary/10">
                        <Icon className="h-4 w-4 text-primary" />
                      </div>
                      <div>
                        <p className="text-sm font-medium">{dt.label}</p>
                        <p className="text-xs text-muted-foreground">{dt.description}</p>
                        <p className="mt-1 font-mono text-[10px] text-muted-foreground/60">{dt.model}</p>
                      </div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
