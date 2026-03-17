package com.challengehub.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class TestEvent extends DomainEvent {

    private final String name;

    public TestEvent(String name) {
        super();
        this.name = Objects.requireNonNull(name);
    }

    public TestEvent(UUID eventId, Instant occurredAt, String name) {
        super(eventId, occurredAt);
        this.name = Objects.requireNonNull(name);
    }

    public String getName() {
        return name;
    }
}
