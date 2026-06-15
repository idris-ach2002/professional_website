package sorbonne.professional_website.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class HomeController {

    private final FrontendRedirectService frontendRedirectService;

    HomeController(FrontendRedirectService frontendRedirectService) {
        this.frontendRedirectService = frontendRedirectService;
    }

    @GetMapping("/")
    String home() {
        return "redirect:" + frontendRedirectService.frontendHomeUrl();
    }

    @GetMapping({"/admin", "/admin/"})
    String admin() {
        return "redirect:" + frontendRedirectService.defaultAdminUrl();
    }
}
