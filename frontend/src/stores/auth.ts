import { create } from "zustand";
import type { MeResponse } from "../lib/api";
import { authApi } from "../lib/api-endpoints";

type AuthState = {
  user: MeResponse | null;
  accessToken: string | null;
  refreshToken: string | null;
  setSession: (user: MeResponse, accessToken: string, refreshToken: string) => void;
  setUser: (user: MeResponse | null) => void;
  logout: () => void;
};

function hasStoredSession(): boolean {
  return !!(localStorage.getItem("access_token") && localStorage.getItem("user"));
}

const storedUser = ((): MeResponse | null => {
  try {
    const raw = localStorage.getItem("user");
    return raw ? (JSON.parse(raw) as MeResponse) : null;
  } catch {
    return null;
  }
})();

export const useAuthStore = create<AuthState>((set) => ({
  user: hasStoredSession() ? storedUser : null,
  accessToken: localStorage.getItem("access_token"),
  refreshToken: localStorage.getItem("refresh_token"),
  setSession: (user, accessToken, refreshToken) => {
    localStorage.setItem("access_token", accessToken);
    localStorage.setItem("refresh_token", refreshToken);
    localStorage.setItem("user", JSON.stringify(user));
    set({ user, accessToken, refreshToken });
  },
  setUser: (user) => {
    if (user) {
      localStorage.setItem("user", JSON.stringify(user));
    } else {
      localStorage.removeItem("user");
    }
    set({ user });
  },
  logout: () => {
    const refreshToken = localStorage.getItem("refresh_token");
    if (refreshToken) {
      authApi.logout(refreshToken).catch(() => {});
    }
    localStorage.removeItem("access_token");
    localStorage.removeItem("refresh_token");
    localStorage.removeItem("user");
    set({ user: null, accessToken: null, refreshToken: null });
  },
}));