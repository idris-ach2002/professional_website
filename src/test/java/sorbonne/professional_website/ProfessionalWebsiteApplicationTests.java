package sorbonne.professional_website;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProfessionalWebsiteApplicationTests {

    @Test
    void contextLoadsWithIsolatedPostgresql() {
        // Le démarrage du contexte valide la configuration Spring et le mapping JPA
        // sur une vraie base PostgreSQL jetable, sans utiliser Aiven.
    }
}
