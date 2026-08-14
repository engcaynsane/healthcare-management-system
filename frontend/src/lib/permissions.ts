import { useAuthStore } from "../stores/auth";

export function can(permission: string): boolean {
  const user = useAuthStore.getState().user;
  return user?.permissions?.includes(permission) ?? false;
}

export function canAny(permissions: string[]): boolean {
  return permissions.some(can);
}

export function hasRole(role: string): boolean {
  const user = useAuthStore.getState().user;
  return user?.roles?.includes(role) ?? false;
}