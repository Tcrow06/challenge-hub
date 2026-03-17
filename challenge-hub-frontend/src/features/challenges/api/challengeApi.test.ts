import { beforeEach, describe, expect, it, vi } from "vitest";
import client from "../../../api/client";
import type { ApiResponse } from "../../../types/api";
import type {
    ChallengeDetail,
    ChallengeListItem,
} from "../../../types/challenge";
import { fetchChallengeDetail, fetchChallenges } from "./challengeApi";

vi.mock("../../../api/client", () => ({
  default: {
    get: vi.fn(),
  },
}));

const mockedGet = vi.mocked(client.get);

const buildApiResponse = <T>(data: T | null): ApiResponse<T> => ({
  success: true,
  message: "ok",
  data,
  metadata: null,
  timestamp: "2026-01-01T00:00:00Z",
});

const challengeItemFixture: ChallengeListItem = {
  id: "c0a63456-f64d-41b8-8b3b-95e4d6237519",
  title: "30 Days of TypeScript",
  description: "Practice TypeScript every day",
  status: "ONGOING",
  difficulty: "MEDIUM",
  cover_url: null,
  start_date: "2026-01-01T00:00:00Z",
  end_date: "2026-01-31T00:00:00Z",
  task_count: 30,
  participant_count: 120,
  created_at: "2025-12-31T00:00:00Z",
};

const challengeDetailFixture: ChallengeDetail = {
  ...challengeItemFixture,
  max_participants: 500,
  allow_late_join: true,
  task_unlock_mode: "DAILY_UNLOCK",
  updated_at: "2026-01-02T00:00:00Z",
  is_joined: false,
};

describe("challengeApi", () => {
  beforeEach(() => {
    mockedGet.mockReset();
  });

  it("fetchChallenges requests /challenges with params", async () => {
    mockedGet.mockResolvedValue({
      data: buildApiResponse<ChallengeListItem[]>([challengeItemFixture]),
    });

    const result = await fetchChallenges({
      page: 1,
      size: 10,
      status: "ONGOING",
    });

    expect(mockedGet).toHaveBeenCalledWith("/challenges", {
      params: {
        page: 1,
        size: 10,
        status: "ONGOING",
      },
    });
    expect(result).toEqual([challengeItemFixture]);
  });

  it("fetchChallenges returns empty array when payload is null", async () => {
    mockedGet.mockResolvedValue({
      data: buildApiResponse<ChallengeListItem[]>(null),
    });

    const result = await fetchChallenges();

    expect(result).toEqual([]);
  });

  it("fetchChallengeDetail throws when payload is missing", async () => {
    mockedGet.mockResolvedValue({
      data: buildApiResponse<ChallengeDetail>(null),
    });

    await expect(fetchChallengeDetail("challenge-id")).rejects.toThrow(
      "Challenge not found",
    );
  });

  it("fetchChallengeDetail returns challenge detail payload", async () => {
    mockedGet.mockResolvedValue({
      data: buildApiResponse<ChallengeDetail>(challengeDetailFixture),
    });

    const result = await fetchChallengeDetail(challengeDetailFixture.id);

    expect(mockedGet).toHaveBeenCalledWith(
      `/challenges/${challengeDetailFixture.id}`,
    );
    expect(result).toEqual(challengeDetailFixture);
  });
});
