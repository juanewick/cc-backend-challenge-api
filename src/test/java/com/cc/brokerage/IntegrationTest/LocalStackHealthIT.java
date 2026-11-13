package com.cc.brokerage.IntegrationTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStackHealthIT extends IntegrationTestBase {

    @Test
    void localstackContainerIsRunning() {
        assertThat(LOCALSTACK.isRunning()).isTrue();
        assertThat(LOCALSTACK.getEndpoint()).isNotNull();
    }
}
