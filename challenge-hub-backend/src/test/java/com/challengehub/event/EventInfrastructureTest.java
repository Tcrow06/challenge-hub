package com.challengehub.event;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class EventInfrastructureTest {

    @Test
    void shouldPublishAndReceiveDomainEvent() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                "com.challengehub.event")) {
            EventPublisher eventPublisher = context.getBean(EventPublisher.class);
            TestEventListener listener = context.getBean(TestEventListener.class);

            listener.clear();

            TestEvent event = new TestEvent("event-infra-ready");
            eventPublisher.publish(event);

            TestEvent received = listener.awaitEvent(Duration.ofSeconds(2));

            assertThat(received).isNotNull();
            assertThat(received.getEventId()).isEqualTo(event.getEventId());
            assertThat(received.getOccurredAt()).isEqualTo(event.getOccurredAt());
            assertThat(received.getName()).isEqualTo("event-infra-ready");
        }
    }
}
