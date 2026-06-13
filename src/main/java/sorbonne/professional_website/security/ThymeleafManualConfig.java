package sorbonne.professional_website.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class ThymeleafManualConfig {

    @Bean
    public ClassLoaderTemplateResolver thymeleafTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();

        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        return resolver;
    }

    @Bean
    public SpringTemplateEngine thymeleafTemplateEngine(
            ClassLoaderTemplateResolver thymeleafTemplateResolver
    ) {
        SpringTemplateEngine engine = new SpringTemplateEngine();

        engine.setTemplateResolver(thymeleafTemplateResolver);
        engine.addDialect(new SpringSecurityDialect());

        return engine;
    }

    @Bean
    public ThymeleafViewResolver thymeleafViewResolver(
            SpringTemplateEngine thymeleafTemplateEngine
    ) {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();

        resolver.setTemplateEngine(thymeleafTemplateEngine);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(1);

        return resolver;
    }
}