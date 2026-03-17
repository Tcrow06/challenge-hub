package com.challengehub.event;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TestEventListener {

    private final LinkedBlockingQueue<TestEvent> receivedEvents = new LinkedBlockingQueue<>();

    @Async("domainEventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTestEvent(TestEvent event) {
        receivedEvents.offer(event);
    }

    public TestEvent awaitEvent(Duration timeout) throws InterruptedException {
        return receivedEvents.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void clear() {
        receivedEvents.clear();
    }
}
