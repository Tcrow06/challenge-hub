export type ChallengeStatus = 'DRAFT' | 'PUBLISHED' | 'ONGOING' | 'ENDED' | 'ARCHIVED';
export type ChallengeDifficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface ChallengeListItem {
  id: string;
  title: string;
  description: string;
  status: ChallengeStatus;
  difficulty: ChallengeDifficulty;
  cover_url: string | null;
  start_date: string | null;
  end_date: string | null;
  task_count: number;
  participant_count: number;
  created_at: string;
}

export interface ChallengeDetail extends ChallengeListItem {
  max_participants: number | null;
  allow_late_join: boolean;
  task_unlock_mode: 'ALL_AT_ONCE' | 'DAILY_UNLOCK';
  updated_at: string;
  is_joined: boolean | null;
}
