package sorbonne.professional_website.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class LoginController {

    private final FrontendRedirectService frontendRedirectService;

    LoginController(FrontendRedirectService frontendRedirectService) {
        this.frontendRedirectService = frontendRedirectService;
    }

    @GetMapping("/login")
    String login(
            Authentication authentication,
            CsrfToken csrfToken,
            @RequestParam(value = "redirect", required = false) String redirect,
            Model model
    ) {
        String targetUrl = frontendRedirectService.resolveRedirect(redirect);

        if (isAuthenticated(authentication)) {
            return "redirect:" + targetUrl;
        }

        // Force la création du token CSRF avant le rendu Thymeleaf.
        // Évite : Cannot create a session after the response has been committed.
        csrfToken.getToken();

        model.addAttribute("redirect", targetUrl);

        return "login";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
