import type { ReactNode } from "react";
import LoginPage from "../features/auth/pages/LoginPage";
import UnauthorizedPage from "../features/auth/pages/UnauthorizedPage";
import ChallengesPage from "../features/challenges/pages/ChallengesPage";
import CreatorWorkspacePage from "../features/challenges/pages/CreatorWorkspacePage";
import type { UserRole } from "../types/auth";
import { ROUTE_PATHS } from "./paths";

export interface RouteConfigItem {
  path: string;
  element: ReactNode;
  requiresAuth?: boolean;
  allowedRoles?: UserRole[];
}

export const routeConfig: RouteConfigItem[] = [
  {
    path: ROUTE_PATHS.LOGIN,
    element: <LoginPage />,
  },
  {
    path: ROUTE_PATHS.UNAUTHORIZED,
    element: <UnauthorizedPage />,
  },
  {
    path: ROUTE_PATHS.CHALLENGES,
    element: <ChallengesPage />,
    requiresAuth: true,
  },
  {
    path: ROUTE_PATHS.CREATOR_WORKSPACE,
    element: <CreatorWorkspacePage />,
    requiresAuth: true,
    allowedRoles: ["CREATOR", "ADMIN"],
  },
];
