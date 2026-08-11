package sorbonne.professional_website.analytics.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.analytics.entity.AnalyticsEvent;
import sorbonne.professional_website.analytics.repository.AnalyticsEventRepository;

import java.util.List;

@Service
public class AnalyticsBatchWriter {

    private final AnalyticsEventRepository repository;

    public AnalyticsBatchWriter(AnalyticsEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void write(List<AnalyticsEvent> batch) {
        if (batch == null || batch.isEmpty()) return;
        repository.saveAll(batch);
        repository.flush();
    }
}
