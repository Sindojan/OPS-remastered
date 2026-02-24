"use client";

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/contexts/auth-context";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { AlertCircle, Eye, EyeOff, Zap, Loader2 } from "lucide-react";

function getDefaultRoute(role: string): string {
  if (role === "SYSTEM_ADMIN") return "/system/companies";
  if (role === "WORKER") return "/my-day";
  return "/production";
}

export default function LoginPage() {
  const { isAuthenticated, isLoading, user, login } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirect = searchParams.get("redirect");

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!isLoading && isAuthenticated && user) {
      router.replace(redirect || getDefaultRoute(user.role));
    }
  }, [isAuthenticated, isLoading, user, router, redirect]);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="font-mono text-sm text-muted-foreground">Loading...</div>
      </div>
    );
  }

  if (isAuthenticated) {
    return null;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
    } catch {
      setError("E-Mail oder Passwort falsch");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen">
      {/* Branding Panel */}
      <div className="relative hidden lg:flex lg:w-[45%] flex-col justify-between overflow-hidden bg-sidebar p-10">
        {/* Grid pattern background */}
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.06]"
          style={{
            backgroundImage:
              "linear-gradient(to right, currentColor 1px, transparent 1px), linear-gradient(to bottom, currentColor 1px, transparent 1px)",
            backgroundSize: "40px 40px",
          }}
        />

        {/* Geometric accent lines */}
        <div className="pointer-events-none absolute right-0 top-0 h-full w-px bg-gradient-to-b from-transparent via-primary/30 to-transparent" />
        <div className="pointer-events-none absolute bottom-32 left-10 right-10 h-px bg-gradient-to-r from-primary/20 via-primary/40 to-transparent" />

        {/* Top section: Logo */}
        <div className="relative z-10 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg border border-primary/20 bg-primary/10">
            <Zap className="h-5 w-5 text-primary" />
          </div>
          <div>
            <span className="text-lg font-bold tracking-tight text-sidebar-foreground">
              Owlsburg
            </span>
            <span className="ml-1.5 font-mono text-xs font-medium tracking-widest text-primary">
              OPS
            </span>
          </div>
        </div>

        {/* Center section: Claim */}
        <div className="relative z-10 space-y-4">
          <h1 className="text-3xl font-bold leading-tight tracking-tight text-sidebar-foreground">
            Operations Platform
            <br />
            <span className="text-primary">for Manufacturing</span>
          </h1>
          <p className="max-w-sm text-sm leading-relaxed text-sidebar-foreground/60">
            Agent-basierte Steuerung Ihrer Produktionsprozesse.
            Deterministisch. Transparent. Effizient.
          </p>
        </div>

        {/* Bottom section: Version info */}
        <div className="relative z-10 flex items-center gap-4">
          <div className="flex items-center gap-2">
            <div className="h-1.5 w-1.5 rounded-full bg-primary animate-pulse" />
            <span className="font-mono text-[11px] text-sidebar-foreground/40">
              System Online
            </span>
          </div>
          <span className="font-mono text-[11px] text-sidebar-foreground/30">
            v1.0.0
          </span>
        </div>
      </div>

      {/* Login Form Panel */}
      <div className="flex flex-1 items-center justify-center bg-background p-6">
        <div className="w-full max-w-sm space-y-8">
          {/* Mobile logo */}
          <div className="flex items-center gap-3 lg:hidden">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg border border-primary/20 bg-primary/10">
              <Zap className="h-4.5 w-4.5 text-primary" />
            </div>
            <div>
              <span className="text-lg font-bold tracking-tight">Owlsburg</span>
              <span className="ml-1.5 font-mono text-xs font-medium tracking-widest text-primary">
                OPS
              </span>
            </div>
          </div>

          {/* Header */}
          <div className="space-y-1.5">
            <h2 className="text-xl font-bold tracking-tight">Anmelden</h2>
            <p className="text-sm text-muted-foreground">
              Melden Sie sich an, um auf das System zuzugreifen.
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="flex items-center gap-2.5 rounded-md border border-destructive/20 bg-destructive/5 px-3.5 py-2.5 text-sm text-destructive">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="email">E-Mail</Label>
              <Input
                id="email"
                type="email"
                placeholder="operator@owlsburg.de"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                autoFocus
                className="h-10"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Passwort</Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete="current-password"
                  className="h-10 pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-0 top-0 flex h-10 w-10 items-center justify-center text-muted-foreground transition-colors hover:text-foreground"
                  tabIndex={-1}
                >
                  {showPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <Checkbox id="remember" />
              <Label htmlFor="remember" className="text-sm font-normal text-muted-foreground cursor-pointer">
                Angemeldet bleiben
              </Label>
            </div>

            <Button
              type="submit"
              className="h-10 w-full"
              disabled={submitting}
            >
              {submitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Authentifizierung...
                </>
              ) : (
                "Anmelden"
              )}
            </Button>
          </form>

          {/* Footer */}
          <p className="text-center font-mono text-[11px] text-muted-foreground/50">
            Owlsburg OPS - Industrial Operations Platform
          </p>
        </div>
      </div>
    </div>
  );
}
