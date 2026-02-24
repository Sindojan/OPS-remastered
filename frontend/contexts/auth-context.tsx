"use client";

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";
import { apiClient } from "@/lib/api-client";
import type { ApiResponse, LoginResponse } from "@/types/api";

interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  tenantId: string;
}

interface AuthContextType {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem("owlsburg_token");
    const storedUser = localStorage.getItem("owlsburg_user");
    if (token && storedUser) {
      try {
        const payload = JSON.parse(atob(token.split(".")[1]));
        if (payload.exp * 1000 <= Date.now()) {
          // Token expired
          localStorage.removeItem("owlsburg_token");
          localStorage.removeItem("owlsburg_user");
          localStorage.removeItem("owlsburg_refresh_token");
          setIsLoading(false);
          return;
        }
        // Token not expired – verify user still exists via /me
        setUser(JSON.parse(storedUser));
        apiClient.get("/api/users/me").then(() => {
          setIsLoading(false);
        }).catch(() => {
          // User no longer exists in DB (e.g. after DB reset)
          localStorage.removeItem("owlsburg_token");
          localStorage.removeItem("owlsburg_user");
          localStorage.removeItem("owlsburg_refresh_token");
          setUser(null);
          setIsLoading(false);
        });
        return;
      } catch {
        localStorage.removeItem("owlsburg_token");
        localStorage.removeItem("owlsburg_user");
      }
    }
    setIsLoading(false);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiClient.post<ApiResponse<LoginResponse>>(
      "/api/auth/login",
      { email, password }
    );
    const { accessToken, refreshToken, user: userData } = response.data;

    localStorage.setItem("owlsburg_token", accessToken);
    localStorage.setItem("owlsburg_refresh_token", refreshToken);

    const profile: UserProfile = {
      id: userData.id,
      email: userData.email,
      firstName: userData.firstName,
      lastName: userData.lastName,
      role: userData.role,
      tenantId: userData.tenantId || "",
    };
    localStorage.setItem("owlsburg_user", JSON.stringify(profile));
    setUser(profile);
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem("owlsburg_refresh_token");
    try {
      if (refreshToken) {
        await apiClient.post("/api/auth/logout", { refreshToken });
      }
    } catch {
      // Ignore logout errors
    }
    localStorage.removeItem("owlsburg_token");
    localStorage.removeItem("owlsburg_user");
    localStorage.removeItem("owlsburg_refresh_token");
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
