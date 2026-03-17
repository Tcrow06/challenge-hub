import { beforeEach, describe, expect, it, vi } from "vitest";
import client from "../../../api/client";
import type { ApiResponse } from "../../../types/api";
import type { AuthData } from "../../../types/auth";
import { login, logout, register } from "./authApi";

vi.mock("../../../api/client", () => ({
  default: {
    post: vi.fn(),
  },
}));

const mockedPost = vi.mocked(client.post);

const buildApiResponse = <T>(data: T | null): ApiResponse<T> => ({
  success: true,
  message: "ok",
  data,
  metadata: null,
  timestamp: "2026-01-01T00:00:00Z",
});

const authDataFixture: AuthData = {
  access_token: "access-token",
  token_type: "Bearer",
  expires_in: 900,
  user: {
    id: "7f9cc5eb-a722-47cd-8e3c-3c764b988a5a",
    username: "tester",
    email: "tester@example.com",
    role: "USER",
    avatar_url: null,
    display_name: null,
  },
};

describe("authApi", () => {
  beforeEach(() => {
    mockedPost.mockReset();
  });

  it("login calls /auth/login and returns auth payload", async () => {
    mockedPost.mockResolvedValue({
      data: buildApiResponse<AuthData>(authDataFixture),
    });

    const result = await login({
      email: "tester@example.com",
      password: "StrongPass1",
    });

    expect(mockedPost).toHaveBeenCalledWith("/auth/login", {
      email: "tester@example.com",
      password: "StrongPass1",
    });
    expect(result).toEqual(authDataFixture);
  });

  it("register throws when payload is missing", async () => {
    mockedPost.mockResolvedValue({
      data: buildApiResponse<AuthData>(null),
    });

    await expect(
      register({
        username: "tester",
        email: "tester@example.com",
        password: "StrongPass1",
      }),
    ).rejects.toThrow("Missing register payload");
  });

  it("logout posts to /auth/logout", async () => {
    mockedPost.mockResolvedValue({ data: buildApiResponse<null>(null) });

    await logout();

    expect(mockedPost).toHaveBeenCalledWith("/auth/logout");
  });
});
