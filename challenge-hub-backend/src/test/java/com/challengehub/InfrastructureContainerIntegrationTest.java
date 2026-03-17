package com.challengehub;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.challengehub.support.AbstractContainerIntegrationTest;

class InfrastructureContainerIntegrationTest extends AbstractContainerIntegrationTest {

    @Test
    void containersShouldBeRunning() {
        assertThat(POSTGRES.isRunning()).isTrue();
        assertThat(MONGODB.isRunning()).isTrue();
        assertThat(REDIS.isRunning()).isTrue();
    }
}
