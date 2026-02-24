import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Login page is always accessible
  if (pathname.startsWith("/login")) {
    return NextResponse.next();
  }

  // Note: We can't access localStorage in middleware (server-side)
  // Token validation happens client-side in AuthProvider
  // Middleware just provides a basic pass-through for all routes
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|api).*)"],
};
