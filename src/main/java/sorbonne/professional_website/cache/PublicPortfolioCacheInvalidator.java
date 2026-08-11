package sorbonne.professional_website.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PublicPortfolioCacheInvalidator {

    private final CacheManager cacheManager;

    public PublicPortfolioCacheInvalidator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void afterPortfolioChanged(PortfolioChangedEvent event) {
        clear(PublicPortfolioCacheConfig.WEBSITE_CACHE);
        clear(PublicPortfolioCacheConfig.WEBSITE_LIST_CACHE);
        clear(PublicPortfolioCacheConfig.PROJECT_CACHE);
        clear(PublicPortfolioCacheConfig.SEO_CACHE);
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.clear();
    }
}
