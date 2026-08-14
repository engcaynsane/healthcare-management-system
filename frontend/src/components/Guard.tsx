import { useAuthStore } from "../stores/auth";

export function hasPermission(permission: string): boolean {
  return useAuthStore.getState().user?.permissions?.includes(permission) ?? false;
}

export function RequirePermission({
  permission,
  children,
  fallback = null,
}: {
  permission: string;
  children: React.ReactNode;
  fallback?: React.ReactNode;
}) {
  if (hasPermission(permission)) return <>{children}</>;
  return <>{fallback}</>;
}