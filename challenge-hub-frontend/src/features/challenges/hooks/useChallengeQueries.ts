import { useQuery } from '@tanstack/react-query';
import { fetchChallengeDetail, fetchChallenges, type ChallengeListParams } from '../api/challengeApi';

export const useChallengesQuery = (params: ChallengeListParams = {}) => {
  return useQuery({
    queryKey: ['challenges', 'list', params],
    queryFn: () => fetchChallenges(params),
  });
};

export const useChallengeDetailQuery = (id: string) => {
  return useQuery({
    queryKey: ['challenges', 'detail', id],
    queryFn: () => fetchChallengeDetail(id),
    enabled: Boolean(id),
  });
};
