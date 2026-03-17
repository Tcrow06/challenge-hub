import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it } from "vitest";
import App from "./App";
import { useAuthStore } from "./store/authStore";
import type { AuthUser, UserRole } from "./types/auth";

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
    id: "7f9cc5eb-a722-47cd-8e3c-3c764b988a5a",
    username: "tester",
    email: "tester@example.com",
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

const renderAt = (path: string) => {
  return render(
    <MemoryRouter
      initialEntries={[path]}
      future={{
        v7_startTransition: true,
        v7_relativeSplatPath: true,
      }}
    >
      <App />
    </MemoryRouter>,
  );
};

describe("App component", () => {
  beforeEach(() => {
    resetAuthState();
  });

  it("redirects unauthenticated users to login from root", () => {
    renderAt("/");

    expect(screen.getByRole("heading", { name: "Login" })).toBeInTheDocument();
  });

  it("renders challenges page for authenticated user", () => {
    loginAs("USER");

    renderAt("/challenges");

    expect(
      screen.getByRole("heading", { name: "Challenges" }),
    ).toBeInTheDocument();
  });
});
