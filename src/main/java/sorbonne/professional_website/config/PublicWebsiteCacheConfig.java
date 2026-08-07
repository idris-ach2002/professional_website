package sorbonne.professional_website.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class PublicWebsiteCacheConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> publicWebsiteEtagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/website/*");
        registration.setName("publicWebsiteEtagFilter");
        registration.setOrder(20);
        return registration;
    }
}
