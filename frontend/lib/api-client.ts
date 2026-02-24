const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

class ApiClient {
  private getToken(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem('owlsburg_token');
  }

  private async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const token = this.getToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...((options.headers as Record<string, string>) || {}),
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
    });

    if (response.status === 401) {
      localStorage.removeItem('owlsburg_token');
      localStorage.removeItem('owlsburg_user');
      localStorage.removeItem('owlsburg_refresh_token');
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
      throw new Error('Unauthorized');
    }

    if (!response.ok) {
      let msg = `HTTP ${response.status}`;
      try {
        const errorBody = await response.json();
        msg = errorBody?.message || JSON.stringify(errorBody);
      } catch {
        const text = await response.text();
        if (text) msg = text;
      }
      throw new Error(msg);
    }

    const json = await response.json();

    // Handle ApiResponse wrapper with success: false
    if (json && typeof json === "object" && "success" in json && !json.success) {
      throw new Error(json.message || "Operation failed");
    }

    return json;
  }

  async get<T>(path: string): Promise<T> {
    return this.request<T>(path);
  }

  async put<T>(path: string, body: unknown): Promise<T> {
    return this.request<T>(path, { method: 'PUT', body: JSON.stringify(body) });
  }

  async post<T>(path: string, body: unknown): Promise<T> {
    return this.request<T>(path, { method: 'POST', body: JSON.stringify(body) });
  }

  async patch<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  async delete<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: 'DELETE' });
  }
}

export const apiClient = new ApiClient();
