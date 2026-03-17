package com.challengehub.event;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TestEventListener {

    private final LinkedBlockingQueue<TestEvent> receivedEvents = new LinkedBlockingQueue<>();

    @EventListener
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
