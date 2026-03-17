import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuthStore } from "../../store/authStore";
import { ROUTE_PATHS } from "../paths";

interface AuthGuardProps {
  children: ReactNode;
  redirectTo?: string;
}

export const AuthGuard = ({
  children,
  redirectTo = ROUTE_PATHS.LOGIN,
}: AuthGuardProps) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    return (
      <Navigate to={redirectTo} replace state={{ from: location.pathname }} />
    );
  }

  return <>{children}</>;
};
