"use client";

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";
import { apiClient } from "@/lib/api-client";
import type { ApiResponse, LoginResponse, MeResponse } from "@/types/api";

interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  tenantId: string;
  employeeId: string | null;
  enabledModules: string[];
}

interface AuthContextType {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshModules: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchMe = useCallback(async (): Promise<UserProfile | null> => {
    try {
      const res = await apiClient.get<ApiResponse<MeResponse>>("/api/users/me");
      const me = res.data;
      return {
        id: me.id,
        email: me.email,
        firstName: me.firstName,
        lastName: me.lastName,
        role: me.role,
        tenantId: me.tenantId,
        employeeId: me.employeeId || null,
        enabledModules: me.enabledModules || [],
      };
    } catch {
      return null;
    }
  }, []);

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
        fetchMe().then((profile) => {
          if (profile) {
            setUser(profile);
            localStorage.setItem("owlsburg_user", JSON.stringify(profile));
          } else {
            // User no longer exists in DB (e.g. after DB reset)
            localStorage.removeItem("owlsburg_token");
            localStorage.removeItem("owlsburg_user");
            localStorage.removeItem("owlsburg_refresh_token");
            setUser(null);
          }
          setIsLoading(false);
        });
        return;
      } catch {
        localStorage.removeItem("owlsburg_token");
        localStorage.removeItem("owlsburg_user");
      }
    }
    setIsLoading(false);
  }, [fetchMe]);

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiClient.post<ApiResponse<LoginResponse>>(
      "/api/auth/login",
      { email, password }
    );
    const { accessToken, refreshToken, user: userData } = response.data;

    localStorage.setItem("owlsburg_token", accessToken);
    localStorage.setItem("owlsburg_refresh_token", refreshToken);

    // Fetch full profile including enabledModules
    const profile: UserProfile = {
      id: userData.id,
      email: userData.email,
      firstName: userData.firstName,
      lastName: userData.lastName,
      role: userData.role,
      tenantId: userData.tenantId || "",
      employeeId: null,
      enabledModules: [],
    };
    setUser(profile);
    localStorage.setItem("owlsburg_user", JSON.stringify(profile));

    // Fetch modules from /me
    const fullProfile = await fetchMe();
    if (fullProfile) {
      setUser(fullProfile);
      localStorage.setItem("owlsburg_user", JSON.stringify(fullProfile));
    }
  }, [fetchMe]);

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

  const refreshModules = useCallback(async () => {
    const profile = await fetchMe();
    if (profile) {
      setUser(profile);
      localStorage.setItem("owlsburg_user", JSON.stringify(profile));
    }
  }, [fetchMe]);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, logout, refreshModules }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
