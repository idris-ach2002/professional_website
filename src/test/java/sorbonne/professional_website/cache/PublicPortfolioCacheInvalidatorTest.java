package sorbonne.professional_website.cache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class PublicPortfolioCacheInvalidatorTest {

    @Test
    void portfolioChangeClearsEveryPublicCache() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager(
                PublicPortfolioCacheConfig.WEBSITE_CACHE,
                PublicPortfolioCacheConfig.WEBSITE_LIST_CACHE,
                PublicPortfolioCacheConfig.PROJECT_CACHE,
                PublicPortfolioCacheConfig.SEO_CACHE
        );
        manager.getCache(PublicPortfolioCacheConfig.WEBSITE_CACHE).put("owner", "cached");
        manager.getCache(PublicPortfolioCacheConfig.PROJECT_CACHE).put("project", "cached");

        new PublicPortfolioCacheInvalidator(manager)
                .afterPortfolioChanged(new PortfolioChangedEvent(1L, "test"));

        assertThat(manager.getCache(PublicPortfolioCacheConfig.WEBSITE_CACHE).get("owner")).isNull();
        assertThat(manager.getCache(PublicPortfolioCacheConfig.PROJECT_CACHE).get("project")).isNull();
    }
}
