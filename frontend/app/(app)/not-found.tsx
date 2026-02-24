import Link from "next/link";
import { ArrowLeft } from "lucide-react";

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-6">
      <div className="text-center">
        <p className="font-mono text-sm font-medium text-primary tracking-wider">404</p>
        <h1 className="mt-2 text-2xl font-bold tracking-tight">Page Not Found</h1>
        <p className="mt-2 text-sm text-muted-foreground max-w-xs">
          The resource you requested could not be located in the system.
        </p>
      </div>
      <Link
        href="/agents"
        className="inline-flex items-center gap-2 rounded-md bg-primary/10 px-4 py-2 text-sm font-medium text-primary transition-colors hover:bg-primary/15"
      >
        <ArrowLeft className="h-3.5 w-3.5" />
        Return to Console
      </Link>
    </div>
  );
}
