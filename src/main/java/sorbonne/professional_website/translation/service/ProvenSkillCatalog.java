package sorbonne.professional_website.translation.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ProvenSkillCatalog {

    public record Definition(
            String id,
            String label,
            String shortLabel,
            String category,
            String description,
            List<String> terms
    ) {
        public Map<String, String> translatableFields() {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("label", label);
            fields.put("shortLabel", shortLabel);
            fields.put("category", category);
            fields.put("description", description);
            return fields;
        }
    }

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition(
                    "backend-architecture",
                    "Backend & architecture",
                    "Backend",
                    "Backend",
                    "API REST, règles métier, persistance, versioning et conception de services maintenables.",
                    List.of("java", "spring", "spring boot", "api", "backend", "jpa", "hibernate", "flyway", "maven", "validation", "architecture")
            ),
            new Definition(
                    "data-pipelines",
                    "Data pipelines",
                    "Data",
                    "Data",
                    "Collecte, ingestion, transformation, stockage PostgreSQL et exploitation de données volumineuses.",
                    List.of("data", "ais", "pipeline", "ingestion", "csv", "postgresql", "python", "systemd", "batch", "export")
            ),
            new Definition(
                    "frontend-product",
                    "Interfaces produit",
                    "Frontend",
                    "Frontend",
                    "Interfaces React orientées usage, filtres, modales, interactions et lisibilité recruteur.",
                    List.of("react", "mantine", "tailwind", "frontend", "ui", "ux", "web", "gsap", "vite", "interface")
            ),
            new Definition(
                    "graphics-performance",
                    "Performance graphique",
                    "Graphique",
                    "Graphique",
                    "Rendu natif, visualisation, interactions temps réel et séparation stricte UI / moteur.",
                    List.of("opengl", "jogl", "jni", "javafx", "graph", "graphe", "performance", "rendu", "native", "c")
            ),
            new Definition(
                    "devops-deployment",
                    "DevOps & déploiement",
                    "DevOps",
                    "DevOps",
                    "Dockerisation, environnements reproductibles, exposition cloud, stockage fichiers et supervision.",
                    List.of("docker", "kubernetes", "cloudflare", "render", "neon", "minio", "redis", "hpa", "ingress", "cloudinary", "systemd")
            ),
            new Definition(
                    "software-quality",
                    "Qualité logicielle",
                    "Qualité",
                    "Qualité",
                    "Structuration, tests, robustesse, documentation, maintenabilité et gestion des cas limites.",
                    List.of("test", "tests", "documentation", "mvc", "robuste", "qualité", "maintenable", "validation", "refactor", "séparation", "architecture")
            )
    );

    public List<Definition> definitions() {
        return DEFINITIONS;
    }

    public Optional<Definition> find(String id) {
        return DEFINITIONS.stream().filter(item -> item.id().equals(id)).findFirst();
    }
}
