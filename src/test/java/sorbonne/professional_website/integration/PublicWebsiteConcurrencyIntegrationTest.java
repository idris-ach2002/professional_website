package sorbonne.professional_website.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.service.WebsiteService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PublicWebsiteConcurrencyIntegrationTest {

    @Autowired OwnerRepository ownerRepository;
    @Autowired WebsiteService websiteService;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        ownerRepository.deleteAll();
        Owner owner = Owner.builder()
                .name("ACHABOU")
                .firstName("Idris")
                .age(24)
                .address("Paris")
                .active(true)
                .build();
        WebsiteVersion version = WebsiteVersion.builder()
                .versionTag("v1")
                .label("Production")
                .active(true)
                .published(true)
                .owner(owner)
                .build();
        owner.getWebsiteVersions().add(version);
        ownerRepository.saveAndFlush(owner);
    }

    @Test
    void concurrentColdReadsReturnTheSamePublicSnapshotWithoutErrors() throws Exception {
        int requests = 40;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> futures = new ArrayList<>(requests);
            for (int index = 0; index < requests; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return websiteService.getFirstOwner("fr").firstName();
                }));
            }
            start.countDown();
            for (Future<String> future : futures) {
                assertThat(future.get()).isEqualTo("Idris");
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
