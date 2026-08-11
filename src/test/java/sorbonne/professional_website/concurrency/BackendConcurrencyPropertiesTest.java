package sorbonne.professional_website.concurrency;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackendConcurrencyPropertiesTest {

    @Test
    void zeroValuesResolveToBoundedMachineAwareDefaults() {
        BackendConcurrencyProperties properties = new BackendConcurrencyProperties(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        assertThat(properties.cpuCoreSize()).isBetween(1, 8);
        assertThat(properties.cpuMaxSize()).isGreaterThanOrEqualTo(properties.cpuCoreSize());
        assertThat(properties.cpuQueueCapacity()).isEqualTo(128);
        assertThat(properties.ioMaxSize()).isGreaterThanOrEqualTo(properties.ioCoreSize());
        assertThat(properties.virtualIoMaxConcurrency()).isBetween(8, 64);
        assertThat(properties.analyticsQueueCapacity()).isEqualTo(512);
        assertThat(properties.analyticsBatchSize()).isEqualTo(32);
    }

    @Test
    void explicitLimitsArePreservedButMaxNeverDropsBelowCore() {
        BackendConcurrencyProperties properties = new BackendConcurrencyProperties(6, 4, 64, 3, 2, 48, 24, 1, 1, 16, 100, 10, 200);

        assertThat(properties.cpuCoreSize()).isEqualTo(6);
        assertThat(properties.cpuMaxSize()).isEqualTo(6);
        assertThat(properties.ioCoreSize()).isEqualTo(3);
        assertThat(properties.ioMaxSize()).isEqualTo(3);
        assertThat(properties.virtualIoMaxConcurrency()).isEqualTo(24);
    }
}
