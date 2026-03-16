import client from '../../../api/client';
import type { ApiResponse } from '../../../types/api';
import type { ChallengeDetail, ChallengeListItem } from '../../../types/challenge';

export interface ChallengeListParams {
  page?: number;
  size?: number;
  status?: string;
  difficulty?: string;
}

export const fetchChallenges = async (params: ChallengeListParams = {}): Promise<ChallengeListItem[]> => {
  const response = await client.get<ApiResponse<ChallengeListItem[]>>('/challenges', { params });
  return response.data.data ?? [];
};

export const fetchChallengeDetail = async (id: string): Promise<ChallengeDetail> => {
  const response = await client.get<ApiResponse<ChallengeDetail>>(`/challenges/${id}`);
  if (!response.data.data) {
    throw new Error('Challenge not found');
  }
  return response.data.data;
};
