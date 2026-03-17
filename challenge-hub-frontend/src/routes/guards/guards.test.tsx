import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it } from "vitest";
import { useAuthStore } from "../../store/authStore";
import type { AuthUser, UserRole } from "../../types/auth";
import { ROUTE_PATHS } from "../paths";
import { AuthGuard } from "./AuthGuard";
import { RoleGuard } from "./RoleGuard";

const resetAuthState = () => {
  useAuthStore.setState({
    accessToken: null,
    expiresAt: null,
    user: null,
    isAuthenticated: false,
  });
};

const loginAs = (role: UserRole) => {
  const user: AuthUser = {
    id: "15ff26f6-9a84-430f-97d4-2b35d14b8dd1",
    username: "guard_tester",
    email: "guard_tester@example.com",
    role,
    avatar_url: null,
    display_name: null,
  };

  useAuthStore.getState().setAuth({
    accessToken: "access-token",
    expiresIn: 900,
    user,
  });
};

const renderWithRoutes = (path: string) => {
  return render(
    <MemoryRouter
      initialEntries={[path]}
      future={{
        v7_startTransition: true,
        v7_relativeSplatPath: true,
      }}
    >
      <Routes>
        <Route path={ROUTE_PATHS.LOGIN} element={<h1>Login Route</h1>} />
        <Route
          path={ROUTE_PATHS.UNAUTHORIZED}
          element={<h1>Unauthorized Route</h1>}
        />

        <Route
          path="/protected"
          element={
            <AuthGuard>
              <h1>Protected Content</h1>
            </AuthGuard>
          }
        />

        <Route
          path="/creator-only"
          element={
            <RoleGuard allowedRoles={["CREATOR", "ADMIN"]}>
              <h1>Creator Content</h1>
            </RoleGuard>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
};

describe("routing guards", () => {
  beforeEach(() => {
    resetAuthState();
  });

  it("AuthGuard redirects unauthenticated users to login", () => {
    renderWithRoutes("/protected");

    expect(
      screen.getByRole("heading", { name: "Login Route" }),
    ).toBeInTheDocument();
  });

  it("AuthGuard allows authenticated users", () => {
    loginAs("USER");

    renderWithRoutes("/protected");

    expect(
      screen.getByRole("heading", { name: "Protected Content" }),
    ).toBeInTheDocument();
  });

  it("RoleGuard redirects to login when user is missing", () => {
    renderWithRoutes("/creator-only");

    expect(
      screen.getByRole("heading", { name: "Login Route" }),
    ).toBeInTheDocument();
  });

  it("RoleGuard redirects USER role to unauthorized", () => {
    loginAs("USER");

    renderWithRoutes("/creator-only");

    expect(
      screen.getByRole("heading", { name: "Unauthorized Route" }),
    ).toBeInTheDocument();
  });

  it("RoleGuard allows CREATOR role", () => {
    loginAs("CREATOR");

    renderWithRoutes("/creator-only");

    expect(
      screen.getByRole("heading", { name: "Creator Content" }),
    ).toBeInTheDocument();
  });
});
