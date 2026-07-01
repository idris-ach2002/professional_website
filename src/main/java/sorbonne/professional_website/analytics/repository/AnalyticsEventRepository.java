package sorbonne.professional_website.analytics.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.analytics.entity.AnalyticsEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    List<AnalyticsEvent> findByCreatedAtBetweenOrderByCreatedAtDesc(
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<AnalyticsEvent> findByCreatedAtBetweenOrderByCreatedAtDesc(
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );
}
