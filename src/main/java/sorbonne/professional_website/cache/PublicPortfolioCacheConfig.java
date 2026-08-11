package sorbonne.professional_website.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class PublicPortfolioCacheConfig {
    public static final String WEBSITE_CACHE = "publicWebsite";
    public static final String WEBSITE_LIST_CACHE = "publicWebsiteList";
    public static final String PROJECT_CACHE = "publicProject";
    public static final String SEO_CACHE = "publicSeoSnapshot";
}
