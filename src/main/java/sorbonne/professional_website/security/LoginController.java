package sorbonne.professional_website.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class LoginController {

    @GetMapping("/login")
    String login(Authentication authentication, CsrfToken csrfToken) {
        if (isAuthenticated(authentication)) {
            return "redirect:/manager";
        }

        // Force la création du token CSRF avant le rendu Thymeleaf.
        // Évite : Cannot create a session after the response has been committed.
        csrfToken.getToken();

        return "login";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}