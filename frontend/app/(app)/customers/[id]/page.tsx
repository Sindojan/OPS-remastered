"use client";

import { useState, useMemo, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  Building2,
  ArrowLeft,
  AlertTriangle,
  Users,
  MapPin,
  Tag,
  Calendar,
  Plus,
  Pencil,
  Trash2,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { DataTable, type ColumnDef } from "@/components/shared/data-table";
import {
  DomainStatusBadge,
  getCustomerStatusVariant,
} from "@/components/shared/domain-status-badge";
import { SkeletonCard, SkeletonTable } from "@/components/shared/skeleton-variants";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
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
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";

import { useCustomer, useCustomerMutations } from "@/hooks/api/use-customers";
import type {
  ContactResponse,
  AddressResponse,
  PriceGroupResponse,
  AddressType,
} from "@/types/api";
import { toast } from "sonner";
import { formatDate, humanizeStatus, formatCurrency } from "@/lib/format";

// ─── Component ──────────────────────────────────────────

export default function CustomerDetailPage() {
  const params = useParams();
  const router = useRouter();
  const customerId = params.id as string;

  const { data: customer, loading, error, refetch } = useCustomer(customerId);
  const mutations = useCustomerMutations();

  // ─── Edit Mode State ──────────────────────────────────

  const [editMode, setEditMode] = useState(false);
  const [editCompanyName, setEditCompanyName] = useState("");
  const [editShortName, setEditShortName] = useState("");
  const [editTaxId, setEditTaxId] = useState("");
  const [editCustomerNumber, setEditCustomerNumber] = useState("");

  // ─── Contact Dialog State ─────────────────────────────

  const [contactDialogOpen, setContactDialogOpen] = useState(false);
  const [editingContact, setEditingContact] = useState<ContactResponse | null>(null);
  const [contactFirstName, setContactFirstName] = useState("");
  const [contactLastName, setContactLastName] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [contactPosition, setContactPosition] = useState("");
  const [contactIsPrimary, setContactIsPrimary] = useState(false);

  // ─── Address Dialog State ─────────────────────────────

  const [addressDialogOpen, setAddressDialogOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<AddressResponse | null>(null);
  const [addressType, setAddressType] = useState<string>("BILLING");
  const [addressStreet, setAddressStreet] = useState("");
  const [addressZip, setAddressZip] = useState("");
  const [addressCity, setAddressCity] = useState("");
  const [addressCountry, setAddressCountry] = useState("");

  // ─── Edit Handlers ────────────────────────────────────

  const startEdit = useCallback(() => {
    if (!customer) return;
    setEditCompanyName(customer.companyName);
    setEditShortName(customer.shortName ?? "");
    setEditTaxId(customer.taxId ?? "");
    setEditCustomerNumber(customer.customerNumber ?? "");
    setEditMode(true);
  }, [customer]);

  const handleSaveEdit = useCallback(async () => {
    if (!customer) return;
    try {
      const result = await mutations.updateCustomer(customer.id, {
        companyName: editCompanyName || customer.companyName,
        shortName: editShortName || undefined,
        taxId: editTaxId || undefined,
        customerNumber: editCustomerNumber || undefined,
      });
      if (result) {
        toast.success("Kunde aktualisiert");
        setEditMode(false);
        refetch();
      }
    } catch (err) {
      toast.error("Kunde konnte nicht aktualisiert werden", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [mutations, customer, editCompanyName, editShortName, editTaxId, editCustomerNumber, refetch]);

  const handleToggleStatus = useCallback(async () => {
    if (!customer) return;
    const newStatus = customer.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    try {
      const result = await mutations.updateCustomer(customer.id, {
        companyName: customer.companyName,
      });
      if (result) {
        toast.success(`Kunde ${newStatus === "ACTIVE" ? "aktiviert" : "deaktiviert"}`);
        refetch();
      }
    } catch (err) {
      toast.error("Statusänderung fehlgeschlagen", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [mutations, customer, refetch]);

  // ─── Contact Handlers ─────────────────────────────────

  const openContactDialog = useCallback((contact?: ContactResponse) => {
    if (contact) {
      setEditingContact(contact);
      setContactFirstName(contact.firstName);
      setContactLastName(contact.lastName);
      setContactEmail(contact.email ?? "");
      setContactPhone(contact.phone ?? "");
      setContactPosition(contact.position ?? "");
      setContactIsPrimary(contact.isPrimary);
    } else {
      setEditingContact(null);
      setContactFirstName("");
      setContactLastName("");
      setContactEmail("");
      setContactPhone("");
      setContactPosition("");
      setContactIsPrimary(false);
    }
    setContactDialogOpen(true);
  }, []);

  const handleSaveContact = useCallback(async () => {
    if (!customer) return;
    try {
      const data = {
        firstName: contactFirstName,
        lastName: contactLastName,
        email: contactEmail || undefined,
        phone: contactPhone || undefined,
        position: contactPosition || undefined,
        isPrimary: contactIsPrimary,
      };

      if (editingContact) {
        await mutations.updateContact(customer.id, editingContact.id, data);
        toast.success("Kontakt aktualisiert");
      } else {
        await mutations.addContact(customer.id, data);
        toast.success("Kontakt hinzugefügt");
      }
      setContactDialogOpen(false);
      refetch();
    } catch (err) {
      toast.error("Kontakt konnte nicht gespeichert werden", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [
    mutations, customer, editingContact,
    contactFirstName, contactLastName, contactEmail,
    contactPhone, contactPosition, contactIsPrimary, refetch,
  ]);

  const handleDeleteContact = useCallback(async (contact: ContactResponse) => {
    if (!customer) return;
    try {
      await mutations.deleteContact(customer.id, contact.id);
      toast.success("Kontakt gelöscht");
      refetch();
    } catch (err) {
      toast.error("Kontakt konnte nicht gelöscht werden", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [mutations, customer, refetch]);

  // ─── Address Handlers ─────────────────────────────────

  const openAddressDialog = useCallback((address?: AddressResponse) => {
    if (address) {
      setEditingAddress(address);
      setAddressType(address.type);
      setAddressStreet(address.street);
      setAddressZip(address.zip);
      setAddressCity(address.city);
      setAddressCountry(address.country ?? "");
    } else {
      setEditingAddress(null);
      setAddressType("BILLING");
      setAddressStreet("");
      setAddressZip("");
      setAddressCity("");
      setAddressCountry("");
    }
    setAddressDialogOpen(true);
  }, []);

  const handleSaveAddress = useCallback(async () => {
    if (!customer) return;
    try {
      const data = {
        type: addressType,
        street: addressStreet,
        zip: addressZip,
        city: addressCity,
        country: addressCountry || undefined,
      };

      if (editingAddress) {
        await mutations.updateAddress(customer.id, editingAddress.id, data);
        toast.success("Adresse aktualisiert");
      } else {
        await mutations.addAddress(customer.id, data);
        toast.success("Adresse hinzugefügt");
      }
      setAddressDialogOpen(false);
      refetch();
    } catch (err) {
      toast.error("Adresse konnte nicht gespeichert werden", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [
    mutations, customer, editingAddress,
    addressType, addressStreet, addressZip,
    addressCity, addressCountry, refetch,
  ]);

  const handleDeleteAddress = useCallback(async (address: AddressResponse) => {
    if (!customer) return;
    try {
      await mutations.deleteAddress(customer.id, address.id);
      toast.success("Adresse gelöscht");
      refetch();
    } catch (err) {
      toast.error("Adresse konnte nicht gelöscht werden", {
        description: err instanceof Error ? err.message : "Unbekannter Fehler",
      });
    }
  }, [mutations, customer, refetch]);

  // ─── Table Columns ────────────────────────────────────

  const contactColumns: ColumnDef<ContactResponse>[] = useMemo(
    () => [
      {
        id: "name",
        header: "Name",
        cell: (row) => (
          <span className="font-medium">
            {row.firstName} {row.lastName}
          </span>
        ),
        sortable: true,
      },
      {
        id: "email",
        header: "E-Mail",
        accessorKey: "email",
        cell: (row) => (
          <span className="text-muted-foreground">{row.email ?? "–"}</span>
        ),
      },
      {
        id: "phone",
        header: "Telefon",
        accessorKey: "phone",
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {row.phone ?? "–"}
          </span>
        ),
      },
      {
        id: "position",
        header: "Position",
        accessorKey: "position",
        cell: (row) => (
          <span className="text-muted-foreground">{row.position ?? "–"}</span>
        ),
      },
      {
        id: "isPrimary",
        header: "Primär",
        cell: (row) =>
          row.isPrimary ? (
            <DomainStatusBadge variant="primary">Primär</DomainStatusBadge>
          ) : null,
      },
    ],
    []
  );

  const addressColumns: ColumnDef<AddressResponse>[] = useMemo(
    () => [
      {
        id: "type",
        header: "Typ",
        accessorKey: "type",
        cell: (row) => (
          <DomainStatusBadge
            variant={
              row.type === "BILLING"
                ? "info"
                : row.type === "SHIPPING"
                  ? "primary"
                  : "success"
            }
          >
            {humanizeStatus(row.type)}
          </DomainStatusBadge>
        ),
      },
      {
        id: "street",
        header: "Straße",
        accessorKey: "street",
      },
      {
        id: "zip",
        header: "ZIP",
        accessorKey: "zip",
        cell: (row) => (
          <span className="font-mono text-xs">{row.zip}</span>
        ),
      },
      {
        id: "city",
        header: "Stadt",
        accessorKey: "city",
      },
      {
        id: "country",
        header: "Land",
        accessorKey: "country",
        cell: (row) => (
          <span className="text-muted-foreground">{row.country ?? "–"}</span>
        ),
      },
    ],
    []
  );

  const priceGroupColumns: ColumnDef<PriceGroupResponse>[] = useMemo(
    () => [
      {
        id: "name",
        header: "Name",
        accessorKey: "name",
        sortable: true,
      },
      {
        id: "discountPercent",
        header: "Rabatt %",
        accessorKey: "discountPercent",
        cell: (row) => (
          <span className="font-mono text-xs font-semibold">
            {row.discountPercent}%
          </span>
        ),
      },
      {
        id: "validFrom",
        header: "Gültig ab",
        accessorKey: "validFrom",
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {formatDate(row.validFrom)}
          </span>
        ),
      },
      {
        id: "validUntil",
        header: "Gültig bis",
        accessorKey: "validUntil",
        cell: (row) => (
          <span className="font-mono text-xs text-muted-foreground">
            {formatDate(row.validUntil)}
          </span>
        ),
      },
    ],
    []
  );

  // ─── Error / Loading ──────────────────────────────────

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

  if (loading || !customer) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-3">
          <SkeletonCard className="w-full" />
        </div>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
        <SkeletonTable rows={5} columns={5} />
      </div>
    );
  }

  // ─── Render ────────────────────────────────────────────

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title={customer.companyName}
        breadcrumb={["Kunden", customer.companyName]}
        actions={
          <div className="flex items-center gap-2">
            <DomainStatusBadge variant={getCustomerStatusVariant(customer.status)}>
              {humanizeStatus(customer.status)}
            </DomainStatusBadge>
            <Button
              variant="outline"
              size="sm"
              onClick={() => router.push("/customers")}
            >
              <ArrowLeft className="mr-1.5 h-3.5 w-3.5" />
              Zurück
            </Button>
          </div>
        }
      />

      {/* Tabs */}
      <Tabs defaultValue="overview" className="space-y-4">
        <TabsList>
          <TabsTrigger value="overview" className="gap-1.5">
            <Building2 className="h-3.5 w-3.5" />
            Übersicht
          </TabsTrigger>
          <TabsTrigger value="contacts" className="gap-1.5">
            <Users className="h-3.5 w-3.5" />
            Ansprechpartner
            {customer.contacts.length > 0 && (
              <span className="ml-1 inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-muted px-1 text-[10px] font-bold text-muted-foreground">
                {customer.contacts.length}
              </span>
            )}
          </TabsTrigger>
          <TabsTrigger value="addresses" className="gap-1.5">
            <MapPin className="h-3.5 w-3.5" />
            Adressen
          </TabsTrigger>
          <TabsTrigger value="pricegroups" className="gap-1.5">
            <Tag className="h-3.5 w-3.5" />
            Preisgruppen
          </TabsTrigger>
          <TabsTrigger value="orders" className="gap-1.5">
            <Calendar className="h-3.5 w-3.5" />
            Auftragshistorie
          </TabsTrigger>
        </TabsList>

        {/* ═══ Overview Tab ═══ */}
        <TabsContent value="overview" className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Kundendetails
            </h3>
            <div className="flex gap-2">
              {!editMode ? (
                <>
                  <Button variant="outline" size="sm" onClick={startEdit}>
                    Bearbeiten
                  </Button>
                  <Button variant="outline" size="sm" onClick={handleToggleStatus}>
                    {customer.status === "ACTIVE" ? "Deaktivieren" : "Aktivieren"}
                  </Button>
                </>
              ) : (
                <>
                  <Button variant="outline" size="sm" onClick={() => setEditMode(false)}>
                    Abbrechen
                  </Button>
                  <Button size="sm" onClick={handleSaveEdit} disabled={mutations.loading}>
                    {mutations.loading ? "Wird gespeichert..." : "Änderungen speichern"}
                  </Button>
                </>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Card className="p-4">
              <div className="flex items-center gap-2 text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                <Tag className="h-3.5 w-3.5" />
                Kundennummer
              </div>
              {editMode ? (
                <Input
                  className="mt-2 font-mono text-sm"
                  value={editCustomerNumber}
                  onChange={(e) => setEditCustomerNumber(e.target.value)}
                />
              ) : (
                <p className="mt-1.5 font-mono text-sm font-semibold">
                  {customer.customerNumber ?? "–"}
                </p>
              )}
            </Card>

            <Card className="p-4">
              <div className="flex items-center gap-2 text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                <Building2 className="h-3.5 w-3.5" />
                Firmenname
              </div>
              {editMode ? (
                <Input
                  className="mt-2 text-sm"
                  value={editCompanyName}
                  onChange={(e) => setEditCompanyName(e.target.value)}
                />
              ) : (
                <p className="mt-1.5 text-sm font-semibold">
                  {customer.companyName}
                </p>
              )}
            </Card>

            <Card className="p-4">
              <div className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                Kurzname
              </div>
              {editMode ? (
                <Input
                  className="mt-2 text-sm"
                  value={editShortName}
                  onChange={(e) => setEditShortName(e.target.value)}
                />
              ) : (
                <p className="mt-1.5 text-sm font-semibold">
                  {customer.shortName ?? "–"}
                </p>
              )}
            </Card>

            <Card className="p-4">
              <div className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                Steuer-ID
              </div>
              {editMode ? (
                <Input
                  className="mt-2 font-mono text-xs"
                  value={editTaxId}
                  onChange={(e) => setEditTaxId(e.target.value)}
                />
              ) : (
                <p className="mt-1.5 font-mono text-xs font-semibold">
                  {customer.taxId ?? "–"}
                </p>
              )}
            </Card>

            <Card className="p-4">
              <div className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                Status
              </div>
              <div className="mt-1.5">
                <DomainStatusBadge variant={getCustomerStatusVariant(customer.status)}>
                  {humanizeStatus(customer.status)}
                </DomainStatusBadge>
              </div>
            </Card>

            <Card className="p-4">
              <div className="flex items-center gap-2 text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                <Calendar className="h-3.5 w-3.5" />
                Erstellt am
              </div>
              <p className="mt-1.5 font-mono text-sm font-semibold">
                {formatDate(customer.createdAt)}
              </p>
            </Card>
          </div>
        </TabsContent>

        {/* ═══ Contacts Tab ═══ */}
        <TabsContent value="contacts" className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Ansprechpartner
            </h3>
            <Button
              variant="outline"
              size="sm"
              className="gap-1.5"
              onClick={() => openContactDialog()}
            >
              <Plus className="h-3.5 w-3.5" />
              Kontakt hinzufügen
            </Button>
          </div>

          <DataTable<ContactResponse>
            data={customer.contacts}
            columns={contactColumns}
            pageSize={10}
            rowActions={[
              {
                label: "Bearbeiten",
                icon: <Pencil className="h-3.5 w-3.5" />,
                onClick: (row) => openContactDialog(row),
              },
              {
                label: "Löschen",
                icon: <Trash2 className="h-3.5 w-3.5" />,
                onClick: (row) => handleDeleteContact(row),
                variant: "destructive",
              },
            ]}
            emptyState={{
              icon: <Users className="h-8 w-8 text-muted-foreground/40" />,
              title: "Keine Ansprechpartner",
              description: "Fügen Sie Ansprechpartner für diesen Kunden hinzu.",
            }}
          />
        </TabsContent>

        {/* ═══ Addresses Tab ═══ */}
        <TabsContent value="addresses" className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Adressen
            </h3>
            <Button
              variant="outline"
              size="sm"
              className="gap-1.5"
              onClick={() => openAddressDialog()}
            >
              <Plus className="h-3.5 w-3.5" />
              Adresse hinzufügen
            </Button>
          </div>

          <DataTable<AddressResponse>
            data={customer.addresses}
            columns={addressColumns}
            pageSize={10}
            rowActions={[
              {
                label: "Bearbeiten",
                icon: <Pencil className="h-3.5 w-3.5" />,
                onClick: (row) => openAddressDialog(row),
              },
              {
                label: "Löschen",
                icon: <Trash2 className="h-3.5 w-3.5" />,
                onClick: (row) => handleDeleteAddress(row),
                variant: "destructive",
              },
            ]}
            emptyState={{
              icon: <MapPin className="h-8 w-8 text-muted-foreground/40" />,
              title: "Keine Adressen",
              description: "Fügen Sie Adressen für diesen Kunden hinzu.",
            }}
          />
        </TabsContent>

        {/* ═══ Price Groups Tab ═══ */}
        <TabsContent value="pricegroups" className="space-y-4">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
            Preisgruppen
          </h3>

          <DataTable<PriceGroupResponse>
            data={customer.priceGroups}
            columns={priceGroupColumns}
            pageSize={10}
            emptyState={{
              icon: <Tag className="h-8 w-8 text-muted-foreground/40" />,
              title: "Keine Preisgruppen",
              description: "Diesem Kunden sind noch keine Preisgruppen zugeordnet.",
            }}
          />
        </TabsContent>

        {/* ═══ Order History Tab ═══ */}
        <TabsContent value="orders">
          <Card className="flex flex-col items-center justify-center gap-4 p-12 text-center">
            <Calendar className="h-10 w-10 text-muted-foreground/30" />
            <div>
              <p className="text-sm font-semibold text-foreground/70">
                Auftragshistorie folgt in Kürze
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                Verfolgen Sie Kundenaufträge, Lieferungen und Rechnungen.
              </p>
            </div>
          </Card>
        </TabsContent>
      </Tabs>

      {/* ═══ Contact Dialog ═══ */}
      <Dialog open={contactDialogOpen} onOpenChange={setContactDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Users className="h-4 w-4 text-primary" />
              {editingContact ? "Kontakt bearbeiten" : "Kontakt hinzufügen"}
            </DialogTitle>
            <DialogDescription>
              {editingContact
                ? `Kontakt ${editingContact.firstName} ${editingContact.lastName} bearbeiten`
                : "Neuen Ansprechpartner für diesen Kunden hinzufügen."}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Vorname *</Label>
                <Input
                  value={contactFirstName}
                  onChange={(e) => setContactFirstName(e.target.value)}
                  placeholder="John"
                />
              </div>
              <div className="space-y-1.5">
                <Label>Nachname *</Label>
                <Input
                  value={contactLastName}
                  onChange={(e) => setContactLastName(e.target.value)}
                  placeholder="Doe"
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>E-Mail</Label>
                <Input
                  type="email"
                  value={contactEmail}
                  onChange={(e) => setContactEmail(e.target.value)}
                  placeholder="john@example.com"
                />
              </div>
              <div className="space-y-1.5">
                <Label>Telefon</Label>
                <Input
                  value={contactPhone}
                  onChange={(e) => setContactPhone(e.target.value)}
                  placeholder="+49 123 456789"
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Position</Label>
                <Input
                  value={contactPosition}
                  onChange={(e) => setContactPosition(e.target.value)}
                  placeholder="CEO, Buyer..."
                />
              </div>
              <div className="flex items-end gap-2 pb-0.5">
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={contactIsPrimary}
                    onChange={(e) => setContactIsPrimary(e.target.checked)}
                    className="h-4 w-4 rounded border-input"
                  />
                  Primärkontakt
                </label>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setContactDialogOpen(false)}
            >
              Abbrechen
            </Button>
            <Button
              size="sm"
              onClick={handleSaveContact}
              disabled={mutations.loading || !contactFirstName.trim() || !contactLastName.trim()}
            >
              {mutations.loading ? "Wird gespeichert..." : editingContact ? "Kontakt aktualisieren" : "Kontakt hinzufügen"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ═══ Address Dialog ═══ */}
      <Dialog open={addressDialogOpen} onOpenChange={setAddressDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <MapPin className="h-4 w-4 text-primary" />
              {editingAddress ? "Adresse bearbeiten" : "Adresse hinzufügen"}
            </DialogTitle>
            <DialogDescription>
              {editingAddress
                ? "Adressdetails aktualisieren."
                : "Neue Adresse für diesen Kunden hinzufügen."}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Typ</Label>
              <Select value={addressType} onValueChange={setAddressType}>
                <SelectTrigger className="text-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="BILLING">Rechnung</SelectItem>
                  <SelectItem value="SHIPPING">Lieferung</SelectItem>
                  <SelectItem value="BOTH">Beides</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>Straße *</Label>
              <Input
                value={addressStreet}
                onChange={(e) => setAddressStreet(e.target.value)}
                placeholder="Musterstraße 1"
              />
            </div>
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <Label>PLZ *</Label>
                <Input
                  value={addressZip}
                  onChange={(e) => setAddressZip(e.target.value)}
                  placeholder="12345"
                  className="font-mono"
                />
              </div>
              <div className="col-span-2 space-y-1.5">
                <Label>Stadt *</Label>
                <Input
                  value={addressCity}
                  onChange={(e) => setAddressCity(e.target.value)}
                  placeholder="Berlin"
                />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label>Land</Label>
              <Input
                value={addressCountry}
                onChange={(e) => setAddressCountry(e.target.value)}
                placeholder="Deutschland"
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setAddressDialogOpen(false)}
            >
              Abbrechen
            </Button>
            <Button
              size="sm"
              onClick={handleSaveAddress}
              disabled={
                mutations.loading ||
                !addressStreet.trim() ||
                !addressZip.trim() ||
                !addressCity.trim()
              }
            >
              {mutations.loading ? "Wird gespeichert..." : editingAddress ? "Adresse aktualisieren" : "Adresse hinzufügen"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
