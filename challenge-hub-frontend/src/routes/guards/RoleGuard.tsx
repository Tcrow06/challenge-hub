import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuthStore } from "../../store/authStore";
import type { UserRole } from "../../types/auth";
import { ROUTE_PATHS } from "../paths";

interface RoleGuardProps {
  children: ReactNode;
  allowedRoles: UserRole[];
  redirectTo?: string;
}

const ROLE_INHERITANCE: Record<UserRole, UserRole[]> = {
  USER: ["USER"],
  CREATOR: ["CREATOR", "USER"],
  MODERATOR: ["MODERATOR", "USER"],
  ADMIN: ["ADMIN", "MODERATOR", "CREATOR", "USER"],
};

const hasAnyAllowedRole = (currentRole: UserRole, allowedRoles: UserRole[]) => {
  const effectiveRoles = new Set(ROLE_INHERITANCE[currentRole]);
  return allowedRoles.some((role) => effectiveRoles.has(role));
};

export const RoleGuard = ({
  children,
  allowedRoles,
  redirectTo = ROUTE_PATHS.UNAUTHORIZED,
}: RoleGuardProps) => {
  const user = useAuthStore((state) => state.user);

  if (!user) {
    return <Navigate to={ROUTE_PATHS.LOGIN} replace />;
  }

  if (!hasAnyAllowedRole(user.role, allowedRoles)) {
    return <Navigate to={redirectTo} replace />;
  }

  return <>{children}</>;
};
