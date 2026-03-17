package com.challengehub.event;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.challengehub.config.EventAsyncConfig;

class EventInfrastructureTest {

    @Test
    void shouldPublishAndReceiveDomainEvent() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(EventAsyncConfig.class, EventPublisher.class, TestEventListener.class);
            context.registerBean(PlatformTransactionManager.class, TestTransactionManager::new);
            context.refresh();

            EventPublisher eventPublisher = context.getBean(EventPublisher.class);
            TestEventListener listener = context.getBean(TestEventListener.class);
            TransactionTemplate transactionTemplate = new TransactionTemplate(
                context.getBean(PlatformTransactionManager.class));

            listener.clear();

            TestEvent event = new TestEvent("event-infra-ready");
            transactionTemplate.executeWithoutResult(status -> eventPublisher.publish(event));

            TestEvent received = listener.awaitEvent(Duration.ofSeconds(2));

            assertThat(received).isNotNull();
            assertThat(received.getEventId()).isEqualTo(event.getEventId());
            assertThat(received.getOccurredAt()).isEqualTo(event.getOccurredAt());
            assertThat(received.getName()).isEqualTo("event-infra-ready");
        }
    }

    private static final class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
