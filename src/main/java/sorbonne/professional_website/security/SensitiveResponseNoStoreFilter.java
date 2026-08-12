package sorbonne.professional_website.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Prevents authenticated/admin responses from being retained by browser,
 * proxy or shared caches. Public portfolio responses keep their explicit
 * cache policy and are intentionally not touched here.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SensitiveResponseNoStoreFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isSensitivePath(request.getRequestURI())) {
            response.setHeader("Cache-Control", "no-store, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0L);
        }
        filterChain.doFilter(request, response);
    }

    static boolean isSensitivePath(String path) {
        if (path == null) return false;
        return path.equals("/manager")
                || path.startsWith("/manager/")
                || path.equals("/api")
                || path.startsWith("/api/")
                || path.equals("/csrf")
                || path.equals("/uploads")
                || path.equals("/uploads/");
    }
}
