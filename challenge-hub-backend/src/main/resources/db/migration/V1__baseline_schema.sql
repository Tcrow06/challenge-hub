CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    avatar_url TEXT,
    display_name VARCHAR(100),
    bio VARCHAR(500),
    streak_count INT NOT NULL DEFAULT 0,
    streak_last_date DATE,
    login_failed_count INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS challenges (
    id UUID PRIMARY KEY,
    creator_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    cover_url TEXT,
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE,
    difficulty VARCHAR(20),
    max_participants INT,
    allow_late_join BOOLEAN NOT NULL DEFAULT TRUE,
    task_unlock_mode VARCHAR(20) NOT NULL DEFAULT 'ALL_AT_ONCE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_challenges_creator FOREIGN KEY (creator_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY,
    challenge_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    day_number INT NOT NULL,
    max_score INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tasks_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (id),
    CONSTRAINT uk_tasks_challenge_day UNIQUE (challenge_id, day_number)
);

CREATE TABLE IF NOT EXISTS user_challenges (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    challenge_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_score INT NOT NULL DEFAULT 0,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_challenges_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_challenges_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (id),
    CONSTRAINT uk_user_challenges_user_challenge UNIQUE (user_id, challenge_id)
);

CREATE TABLE IF NOT EXISTS media (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    storage_provider VARCHAR(20) NOT NULL,
    file_key VARCHAR(500) NOT NULL,
    file_url TEXT,
    file_type VARCHAR(50),
    file_size BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_media_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS submissions (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    description TEXT,
    media_id UUID,
    status VARCHAR(20) NOT NULL,
    score INT,
    reviewer_id UUID,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reject_reason VARCHAR(500),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_submissions_task FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT fk_submissions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_submissions_media FOREIGN KEY (media_id) REFERENCES media (id),
    CONSTRAINT fk_submissions_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id),
    CONSTRAINT uk_submissions_task_user UNIQUE (task_id, user_id)
);

CREATE TABLE IF NOT EXISTS badges (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_badges_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS user_badges (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    badge_id UUID NOT NULL,
    earned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_badges_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_badges_badge FOREIGN KEY (badge_id) REFERENCES badges (id),
    CONSTRAINT uk_user_badges_user_badge UNIQUE (user_id, badge_id)
);

CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);

CREATE INDEX IF NOT EXISTS idx_challenges_creator_id ON challenges (creator_id);

CREATE INDEX IF NOT EXISTS idx_challenges_status ON challenges (status);

CREATE INDEX IF NOT EXISTS idx_tasks_challenge_id ON tasks (challenge_id);

CREATE INDEX IF NOT EXISTS idx_user_challenges_challenge_id ON user_challenges (challenge_id);

CREATE INDEX IF NOT EXISTS idx_submissions_user_id ON submissions (user_id);

CREATE INDEX IF NOT EXISTS idx_submissions_status ON submissions (status);