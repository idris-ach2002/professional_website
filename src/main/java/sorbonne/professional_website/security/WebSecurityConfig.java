package sorbonne.professional_website.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
class WebSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UrlBasedCorsConfigurationSource corsConfigurationSource,
            FrontendRedirectService frontendRedirectService
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests((requests) -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()

                        // CORS preflight. Obligatoire pour les requêtes cross-origin.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Pages publiques / ressources statiques.
                        .requestMatchers("/", "/login", "/error", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/assets/**", "/webjars/**").permitAll()

                        // Admin HTML : Spring bloque ici si non connecté, puis /admin est redirigé vers le front.
                        .requestMatchers(HttpMethod.GET, "/admin").authenticated()
                        .requestMatchers("/admin/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/csrf").authenticated()

                        // Public portfolio read endpoints used by the front-office.
                        .requestMatchers(HttpMethod.GET, "/website").permitAll()
                        .requestMatchers(HttpMethod.GET, "/website/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/files/**").permitAll()

                        // Toute l'API manager est protégée, y compris GET /manager.
                        // Sinon le front ne peut pas détecter correctement l'état non authentifié.
                        .requestMatchers("/manager", "/manager/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .requestMatchers("/uploads", "/uploads/**").hasRole("ADMIN")

                        // Fail closed.
                        .anyRequest().denyAll()
                )
                .formLogin((form) -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            String requestedRedirect = request.getParameter("redirect");
                            response.sendRedirect(frontendRedirectService.resolveRedirect(requestedRedirect));
                        })
                        .failureHandler((request, response, exception) -> {
                            String requestedRedirect = request.getParameter("redirect");
                            String targetUrl = frontendRedirectService.resolveRedirect(requestedRedirect);
                            String encodedTargetUrl = java.net.URLEncoder.encode(
                                    targetUrl,
                                    java.nio.charset.StandardCharsets.UTF_8
                            );
                            response.sendRedirect("/login?error&redirect=" + encodedTargetUrl);
                        })
                        .permitAll()
                )
                .logout((logout) -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        List<String> origins = parseOrigins(allowedOrigins);

        if (origins.isEmpty()) {
            throw new IllegalStateException("Missing app.cors.allowed-origins / APP_CORS_ALLOWED_ORIGINS");
        }

        CorsConfiguration configuration = new CorsConfiguration();

        // Origines distantes autorisées uniquement.
        // Exemples : http://localhost:5173, http://localhost:4173, Cloudflare Workers.
        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-CSRF-TOKEN"
        ));

        configuration.setExposedHeaders(List.of(
                "Location"
        ));

        // Obligatoire car le front appelle l'API avec credentials: "include".
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${portfolio.security.admin.username}") String username,
            @Value("${portfolio.security.admin.password}") String password
    ) {
        UserDetails admin = User.withUsername(username)
                .password(encoder.encode(password))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    private static List<String> parseOrigins(String rawOrigins) {
        if (rawOrigins == null || rawOrigins.isBlank()) {
            return List.of();
        }

        return List.of(rawOrigins.split(","))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(WebSecurityConfig::stripTrailingSlash)
                .toList();
    }

    private static String stripTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/") && trimmed.length() > 1) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
