package sorbonne.professional_website.analytics.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sorbonne.professional_website.analytics.entity.AnalyticsEvent;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    List<AnalyticsEvent> findByCreatedAtBetweenOrderByCreatedAtDesc(
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    @Query(value = """
        SELECT
            COUNT(*) AS "totalEvents",
            COUNT(*) FILTER (WHERE lower(event_type) = 'page_view') AS "pageViews",
            COUNT(DISTINCT visitor_id_hash) AS "uniqueVisitors",
            COUNT(DISTINCT session_id_hash) AS "sessions",
            COUNT(*) FILTER (WHERE lower(event_type) = 'cv_click') AS "cvClicks",
            COUNT(*) FILTER (WHERE lower(event_type) = 'github_click') AS "githubClicks",
            COUNT(*) FILTER (WHERE lower(event_type) = 'linkedin_click') AS "linkedinClicks",
            COUNT(*) FILTER (WHERE lower(event_type) = 'project_view') AS "projectViews"
        FROM analytics_event
        WHERE created_at BETWEEN :from AND :to
        """, nativeQuery = true)
    AnalyticsTotalsView aggregateTotals(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
        SELECT
            (created_at AT TIME ZONE 'Europe/Paris')::date AS day,
            COUNT(*) FILTER (WHERE lower(event_type) = 'page_view') AS "pageViews",
            COUNT(DISTINCT visitor_id_hash) AS "uniqueVisitors"
        FROM analytics_event
        WHERE created_at BETWEEN :from AND :to
        GROUP BY day
        ORDER BY day ASC
        """, nativeQuery = true)
    List<DailyMetricView> aggregateDaily(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
        SELECT COALESCE(NULLIF(BTRIM(page_path), ''), 'Non renseigné') AS label, COUNT(*) AS value
        FROM analytics_event WHERE created_at BETWEEN :from AND :to
        GROUP BY label ORDER BY value DESC LIMIT :limit
        """, nativeQuery = true)
    List<MetricCountView> topPages(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, @Param("limit") int limit);

    @Query(value = """
        SELECT COALESCE(NULLIF(BTRIM(project_slug), ''), 'Non renseigné') AS label, COUNT(*) AS value
        FROM analytics_event WHERE created_at BETWEEN :from AND :to
        GROUP BY label ORDER BY value DESC LIMIT :limit
        """, nativeQuery = true)
    List<MetricCountView> topProjects(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, @Param("limit") int limit);

    @Query(value = """
        SELECT COALESCE(NULLIF(BTRIM(source), ''), 'Non renseigné') AS label, COUNT(*) AS value
        FROM analytics_event WHERE created_at BETWEEN :from AND :to
        GROUP BY label ORDER BY value DESC LIMIT :limit
        """, nativeQuery = true)
    List<MetricCountView> topSources(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, @Param("limit") int limit);

    @Query(value = """
        SELECT COALESCE(NULLIF(BTRIM(device_type), ''), 'Non renseigné') AS label, COUNT(*) AS value
        FROM analytics_event WHERE created_at BETWEEN :from AND :to
        GROUP BY label ORDER BY value DESC LIMIT :limit
        """, nativeQuery = true)
    List<MetricCountView> topDevices(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, @Param("limit") int limit);

    @Query(value = """
        SELECT COALESCE(NULLIF(BTRIM(browser), ''), 'Non renseigné') AS label, COUNT(*) AS value
        FROM analytics_event WHERE created_at BETWEEN :from AND :to
        GROUP BY label ORDER BY value DESC LIMIT :limit
        """, nativeQuery = true)
    List<MetricCountView> topBrowsers(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, @Param("limit") int limit);

    @Query(value = """
        SELECT COALESCE(NULLIF(BTRIM(recruiter_code), ''), 'Non renseigné') AS label, COUNT(*) AS value
        FROM analytics_event WHERE created_at BETWEEN :from AND :to
        GROUP BY label ORDER BY value DESC LIMIT :limit
        """, nativeQuery = true)
    List<MetricCountView> topRecruiters(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, @Param("limit") int limit);

    interface AnalyticsTotalsView {
        long getTotalEvents();
        long getPageViews();
        long getUniqueVisitors();
        long getSessions();
        long getCvClicks();
        long getGithubClicks();
        long getLinkedinClicks();
        long getProjectViews();
    }

    interface DailyMetricView {
        LocalDate getDay();
        long getPageViews();
        long getUniqueVisitors();
    }

    interface MetricCountView {
        String getLabel();
        long getValue();
    }
}
