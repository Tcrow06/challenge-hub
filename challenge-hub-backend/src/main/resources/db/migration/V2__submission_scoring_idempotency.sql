CREATE TABLE IF NOT EXISTS submission_score_events (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_submission_score_events_submission FOREIGN KEY (submission_id) REFERENCES submissions (id),
    CONSTRAINT uk_submission_score_events_submission UNIQUE (submission_id)
);

CREATE INDEX IF NOT EXISTS idx_submission_score_events_submission_id ON submission_score_events (submission_id);