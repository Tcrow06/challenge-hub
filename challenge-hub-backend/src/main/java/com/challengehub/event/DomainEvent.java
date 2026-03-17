package com.challengehub.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public abstract class DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;

    protected DomainEvent() {
        this(UUID.randomUUID(), Instant.now());
    }

    protected DomainEvent(UUID eventId, Instant occurredAt) {
        this.eventId = Objects.requireNonNull(eventId);
        this.occurredAt = Objects.requireNonNull(occurredAt);
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
