const API_BASE = import.meta.env.VITE_API_BASE || "";

export function getApiBase(): string {
  return API_BASE;
}

export function authHeaders(): Record<string, string> {
  const token = localStorage.getItem("access_token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export type HttpError = {
  status: number;
  message: string;
  data?: unknown;
};

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...authHeaders(),
    ...(init.headers as Record<string, string> | undefined),
  };

  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });

  if (response.status === 401) {
    const refreshed = await tryRefresh();
    if (!refreshed) {
      localStorage.removeItem("access_token");
      localStorage.removeItem("refresh_token");
      localStorage.removeItem("user");
      window.dispatchEvent(new Event("auth:logged-out"));
      throw new ApiHttpError(401, "Session expired. Please sign in again.");
    }
    return request<T>(path, init);
  }

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const body = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const message =
      (body && typeof body === "object" && "message" in body
        ? (body as { message: string }).message
        : null) || `Request failed (${response.status})`;
    throw new ApiHttpError(response.status, message, body);
  }
  return unwrap<T>(body);
}

export class ApiHttpError extends Error {
  status: number;
  data?: unknown;
  constructor(status: number, message: string, data?: unknown) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

async function tryRefresh(): Promise<boolean> {
  const refreshToken = localStorage.getItem("refresh_token");
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${API_BASE}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const body = await res.json();
    const data = unwrap<LoginData>(body);
    localStorage.setItem("access_token", data.accessToken);
    localStorage.setItem("refresh_token", data.refreshToken);
    localStorage.setItem("user", JSON.stringify(data.user));
    return true;
  } catch {
    return false;
  }
}

function unwrap<T>(body: unknown): T {
  if (body && typeof body === "object" && "data" in body) {
    return (body as { data: T }).data;
  }
  return body as T;
}

export type LoginData = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: MeResponse;
};

export type MeResponse = {
  id: number;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  branchId: number | null;
  branchName: string | null;
  roles: string[];
  permissions: string[];
};