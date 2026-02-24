"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { useAuth } from "@/contexts/auth-context";
import { useTenantConfig, useTenantConfigMutations } from "@/hooks/api/use-settings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "sonner";
import { Building2, Save, Loader2, Upload, Trash2, ImageIcon } from "lucide-react";
import type { TenantConfigUpdateRequest } from "@/types/api";

export function CompanyTab() {
  const { user } = useAuth();
  const { data: tenant, loading, refetch } = useTenantConfig();
  const mutations = useTenantConfigMutations();

  const [form, setForm] = useState<TenantConfigUpdateRequest>({});
  const [logoPreview, setLogoPreview] = useState<string | null>(null);
  const [isDirty, setIsDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [uploadingLogo, setUploadingLogo] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (tenant) {
      setForm({
        name: tenant.name || "",
        contactEmail: tenant.contactEmail || "",
        contactPhone: tenant.contactPhone || "",
        website: tenant.website || "",
        vatId: tenant.vatId || "",
        address: tenant.address || "",
        postalCode: tenant.postalCode || "",
        city: tenant.city || "",
        country: tenant.country || "Deutschland",
      });
      setLogoPreview(tenant.logoUrl);
      setIsDirty(false);
    }
  }, [tenant]);

  const updateField = useCallback((key: keyof TenantConfigUpdateRequest, value: string) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    setIsDirty(true);
  }, []);

  const handleSave = useCallback(async () => {
    setSaving(true);
    try {
      await mutations.patch(form);
      toast.success("Firmendaten gespeichert");
      setIsDirty(false);
      refetch();
    } catch (err) {
      toast.error("Fehler beim Speichern", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setSaving(false);
    }
  }, [form, mutations, refetch]);

  const handleLogoUpload = useCallback(async (file: File) => {
    if (file.size > 2 * 1024 * 1024) {
      toast.error("Logo zu groß", { description: "Maximale Dateigröße: 2 MB" });
      return;
    }
    if (!["image/png", "image/jpeg", "image/svg+xml"].includes(file.type)) {
      toast.error("Ungültiges Format", { description: "Nur PNG, JPG und SVG werden unterstützt." });
      return;
    }
    setUploadingLogo(true);
    try {
      const res = await mutations.uploadLogo(file);
      setLogoPreview(res.data.logoUrl);
      toast.success("Logo hochgeladen");
      refetch();
    } catch (err) {
      toast.error("Fehler beim Hochladen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    } finally {
      setUploadingLogo(false);
    }
  }, [mutations, refetch]);

  const handleLogoDelete = useCallback(async () => {
    try {
      await mutations.deleteLogo();
      setLogoPreview(null);
      toast.success("Logo entfernt");
      refetch();
    } catch (err) {
      toast.error("Fehler beim Entfernen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [mutations, refetch]);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file) handleLogoUpload(file);
  }, [handleLogoUpload]);

  if (user?.role !== "ADMIN" && user?.role !== "SYSTEM_ADMIN") {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <Building2 className="h-8 w-8 text-muted-foreground/40" />
        <p className="mt-2 text-sm text-muted-foreground">Nur Administratoren können Firmendaten bearbeiten.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Logo */}
      <Card className="border-border/50">
        <CardHeader className="pb-4">
          <CardTitle className="text-base">Firmenlogo</CardTitle>
        </CardHeader>
        <CardContent>
          <div
            className="flex flex-col items-center justify-center gap-3 rounded-lg border-2 border-dashed border-border/50 p-6 transition-colors hover:border-primary/30"
            onDragOver={(e) => e.preventDefault()}
            onDrop={handleDrop}
          >
            {logoPreview ? (
              <div className="flex flex-col items-center gap-3">
                <img src={logoPreview} alt="Firmenlogo" className="h-20 max-w-[200px] object-contain" />
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    className="gap-1.5"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploadingLogo}
                  >
                    {uploadingLogo ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Upload className="h-3.5 w-3.5" />}
                    Ersetzen
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="gap-1.5 text-destructive hover:text-destructive"
                    onClick={handleLogoDelete}
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                    Logo entfernen
                  </Button>
                </div>
              </div>
            ) : (
              <>
                <ImageIcon className="h-10 w-10 text-muted-foreground/30" />
                <p className="text-sm text-muted-foreground">Logo hierher ziehen oder klicken</p>
                <p className="text-xs text-muted-foreground">PNG, JPG oder SVG, max. 2 MB</p>
                <Button
                  variant="outline"
                  size="sm"
                  className="gap-1.5"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploadingLogo}
                >
                  {uploadingLogo ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Upload className="h-3.5 w-3.5" />}
                  Logo hochladen
                </Button>
              </>
            )}
            <input
              ref={fileInputRef}
              type="file"
              accept="image/png,image/jpeg,image/svg+xml"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) handleLogoUpload(file);
              }}
            />
          </div>
        </CardContent>
      </Card>

      {/* Company Info */}
      <Card className="border-border/50">
        <CardHeader className="pb-4">
          <CardTitle className="text-base">Firmendaten</CardTitle>
        </CardHeader>
        <CardContent className="space-y-5">
          <div className="space-y-1.5">
            <Label className="text-xs">Firmenname *</Label>
            <Input
              value={form.name || ""}
              onChange={(e) => updateField("name", e.target.value)}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label className="text-xs">Kontakt-E-Mail</Label>
              <Input
                type="email"
                value={form.contactEmail || ""}
                onChange={(e) => updateField("contactEmail", e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Telefon</Label>
              <Input
                value={form.contactPhone || ""}
                onChange={(e) => updateField("contactPhone", e.target.value)}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label className="text-xs">Website</Label>
              <Input
                value={form.website || ""}
                onChange={(e) => updateField("website", e.target.value)}
                placeholder="https://..."
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">USt-IdNr.</Label>
              <Input
                value={form.vatId || ""}
                onChange={(e) => updateField("vatId", e.target.value)}
                placeholder="DE123456789"
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Address */}
      <Card className="border-border/50">
        <CardHeader className="pb-4">
          <CardTitle className="text-base">Adresse</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-1.5">
            <Label className="text-xs">Straße</Label>
            <Input
              value={form.address || ""}
              onChange={(e) => updateField("address", e.target.value)}
            />
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <Label className="text-xs">PLZ</Label>
              <Input
                value={form.postalCode || ""}
                onChange={(e) => updateField("postalCode", e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Stadt</Label>
              <Input
                value={form.city || ""}
                onChange={(e) => updateField("city", e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs">Land</Label>
              <Input
                value={form.country || "Deutschland"}
                onChange={(e) => updateField("country", e.target.value)}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Save Button */}
      {isDirty && (
        <div className="flex items-center gap-3">
          <Button onClick={handleSave} disabled={saving || !form.name} className="gap-2">
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            {saving ? "Speichern..." : "Änderungen speichern"}
          </Button>
        </div>
      )}
    </div>
  );
}
