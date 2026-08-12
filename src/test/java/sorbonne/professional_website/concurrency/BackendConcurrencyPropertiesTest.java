package sorbonne.professional_website.concurrency;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackendConcurrencyPropertiesTest {

    @Test
    void zeroValuesResolveToSmallBoundedDefaults() {
        BackendConcurrencyProperties properties = new BackendConcurrencyProperties(0, 0, 0, 0, 0, 0, 0);

        assertThat(properties.virtualIoMaxConcurrency()).isBetween(8, 32);
        assertThat(properties.maintenanceCoreSize()).isEqualTo(1);
        assertThat(properties.maintenanceMaxSize()).isGreaterThanOrEqualTo(properties.maintenanceCoreSize());
        assertThat(properties.maintenanceQueueCapacity()).isEqualTo(64);
        assertThat(properties.analyticsQueueCapacity()).isEqualTo(512);
        assertThat(properties.analyticsBatchSize()).isEqualTo(32);
        assertThat(properties.analyticsFlushIntervalMs()).isEqualTo(750L);
    }

    @Test
    void explicitLimitsArePreservedButMaintenanceMaxNeverDropsBelowCore() {
        BackendConcurrencyProperties properties = new BackendConcurrencyProperties(24, 3, 2, 48, 100, 10, 200);

        assertThat(properties.virtualIoMaxConcurrency()).isEqualTo(24);
        assertThat(properties.maintenanceCoreSize()).isEqualTo(3);
        assertThat(properties.maintenanceMaxSize()).isEqualTo(3);
        assertThat(properties.maintenanceQueueCapacity()).isEqualTo(48);
        assertThat(properties.analyticsQueueCapacity()).isEqualTo(100);
        assertThat(properties.analyticsBatchSize()).isEqualTo(10);
        assertThat(properties.analyticsFlushIntervalMs()).isEqualTo(200L);
    }
}
