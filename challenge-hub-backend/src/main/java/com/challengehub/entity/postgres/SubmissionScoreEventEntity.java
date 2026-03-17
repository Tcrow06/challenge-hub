package com.challengehub.entity.postgres;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "submission_score_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_submission_score_events_submission", columnNames = { "submission_id" })
})
public class SubmissionScoreEventEntity extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private SubmissionEntity submission;
}