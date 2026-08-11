package sorbonne.professional_website.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import sorbonne.professional_website.dto.request.WebsiteVersionRequestDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;
import sorbonne.professional_website.service.WebsiteVersionService;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WebsiteVersionConcurrencyIntegrationTest {

    @Autowired OwnerRepository ownerRepository;
    @Autowired WebsiteVersionRepository versionRepository;
    @Autowired WebsiteVersionService versionService;

    private Long ownerId;

    @BeforeEach
    void setUp() {
        ownerRepository.deleteAll();
        Owner owner = ownerRepository.saveAndFlush(Owner.builder()
                .name("ACHABOU")
                .firstName("Idris")
                .age(24)
                .address("Paris")
                .active(true)
                .build());
        ownerId = owner.getOwnerId();
    }

    @Test
    void simultaneousActivationStillLeavesExactlyOneActiveVersion() throws Exception {
        var first = versionService.createVersion(ownerId, request("v1", true));
        var second = versionService.createVersion(ownerId, request("v2", false));
        var third = versionService.createVersion(ownerId, request("v3", false));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = pool.submit(() -> awaitAndActivate(start, second.id()));
            Future<?> b = pool.submit(() -> awaitAndActivate(start, third.id()));
            start.countDown();
            a.get();
            b.get();
        } finally {
            pool.shutdownNow();
        }

        List<?> versions = versionRepository.findByOwnerOwnerIdOrderByCreatedAtDesc(ownerId);
        long activeCount = versionRepository.findByOwnerOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .filter(version -> Boolean.TRUE.equals(version.getActive()))
                .count();
        assertThat(versions).hasSize(3);
        assertThat(activeCount).isEqualTo(1);
        assertThat(versionRepository.findByOwnerOwnerIdAndActiveTrue(ownerId)).isPresent();
        assertThat(first.id()).isNotNull();
    }

    private void awaitAndActivate(CountDownLatch start, Long versionId) {
        try {
            start.await();
            versionService.activateVersion(ownerId, versionId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static WebsiteVersionRequestDTO request(String tag, boolean active) {
        return new WebsiteVersionRequestDTO(tag, tag, null, active, true, null, null, null);
    }
}
