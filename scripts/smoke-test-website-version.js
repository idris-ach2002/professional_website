const BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";
const AUTH_TOKEN = process.env.API_TOKEN ?? null;
const RUN_PROD = process.env.RUN_PROD === "true";

const MOCK_USERS = Number(process.env.MOCK_USERS ?? 6);
const MOCK_VERSIONS = Number(process.env.MOCK_VERSIONS ?? 4);
const MOCK_PROJECTS = Number(process.env.MOCK_PROJECTS ?? 12);
const MOCK_EXPERIENCES = Number(process.env.MOCK_EXPERIENCES ?? 8);
const TEST_DEFAULT_WEBSITE = process.env.TEST_DEFAULT_WEBSITE === "true";

if ((BASE_URL.includes("prod") || BASE_URL.includes("production")) && !RUN_PROD) {
    console.error("Refus de lancer le test sur une URL de production sans RUN_PROD=true");
    process.exit(1);
}

const headers = {
    "Content-Type": "application/json",
    ...(AUTH_TOKEN ? { Authorization: `Bearer ${AUTH_TOKEN}` } : {})
};

function uniqueSuffix() {
    return new Date().toISOString().replace(/[-:.TZ]/g, "");
}

async function request(method, path, body = undefined) {
    const response = await fetch(`${BASE_URL}${path}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined
    });

    const text = await response.text();
    let data = null;

    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        data = text;
    }

    if (!response.ok) {
        console.error(`Erreur HTTP ${response.status} sur ${method} ${path}`);
        console.error(data);
        process.exit(1);
    }

    return data;
}

function assert(condition, message) {
    if (!condition) {
        console.error(`Échec test : ${message}`);
        process.exit(1);
    }
}

function pick(array, index) {
    return array[index % array.length];
}

function slugify(value) {
    return value
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/(^-|-$)+/g, "");
}

function buildImageUrl(type, ownerIndex, itemIndex, versionNumber = 1) {
    return `https://example.com/mock/${type}/owner-${ownerIndex}/v${versionNumber}/item-${itemIndex}.png`;
}

const PERSONAS = [
    {
        firstName: "Idris",
        name: "ACHABOU",
        title: "Développeur Full Stack",
        domain: "Software Engineering",
        location: "Paris, France",
        mainStacks: ["Java", "Spring Boot", "React", "PostgreSQL", "Docker", "Tailwind CSS", "Mantine", "GSAP"]
    },
    {
        firstName: "Lina",
        name: "MARTIN",
        title: "Data Engineer",
        domain: "Data Platform",
        location: "Lyon, France",
        mainStacks: ["Python", "FastAPI", "PostgreSQL", "Airflow", "Kafka", "dbt", "Docker", "SQL"]
    },
    {
        firstName: "Nassim",
        name: "BENALI",
        title: "Backend Engineer",
        domain: "Backend Architecture",
        location: "Marseille, France",
        mainStacks: ["Java", "Kotlin", "Spring Boot", "Redis", "PostgreSQL", "RabbitMQ", "Docker", "JUnit"]
    },
    {
        firstName: "Camille",
        name: "DURAND",
        title: "Frontend Developer",
        domain: "Frontend Engineering",
        location: "Bordeaux, France",
        mainStacks: ["React", "TypeScript", "Next.js", "Tailwind CSS", "Mantine", "GSAP", "Vite", "Framer Motion"]
    },
    {
        firstName: "Sarah",
        name: "MOREAU",
        title: "DevOps Engineer",
        domain: "Cloud Infrastructure",
        location: "Nantes, France",
        mainStacks: ["Docker", "Kubernetes", "GitHub Actions", "Terraform", "Linux", "AWS", "Prometheus", "Grafana"]
    },
    {
        firstName: "Yanis",
        name: "ROUSSEAU",
        title: "AI Engineer",
        domain: "Artificial Intelligence",
        location: "Toulouse, France",
        mainStacks: ["Python", "PyTorch", "FastAPI", "PostgreSQL", "Vector DB", "Docker", "LangChain", "NumPy"]
    },
    {
        firstName: "Emma",
        name: "LEFEVRE",
        title: "Product Engineer",
        domain: "Product Development",
        location: "Lille, France",
        mainStacks: ["React", "Node.js", "PostgreSQL", "Prisma", "Docker", "UX", "Analytics", "TypeScript"]
    },
    {
        firstName: "Mehdi",
        name: "KARIM",
        title: "Software Architect",
        domain: "Architecture Logicielle",
        location: "Strasbourg, France",
        mainStacks: ["Java", "Spring Cloud", "Kafka", "PostgreSQL", "DDD", "Clean Architecture", "Docker", "Kubernetes"]
    }
];

const EXPERIENCE_THEMES = [
    ["SCHOOL", "Master Informatique STL", "Sorbonne Université", "Formation avancée en ingénierie logicielle.", ["Java", "Architecture logicielle", "Conception", "Systèmes concurrents"]],
    ["INTERNSHIP", "Stage développement logiciel / data", "LITIS", "Développement d’une chaîne de collecte et exploitation de données.", ["Java", "PostgreSQL", "Python", "Linux", "Symfony"]],
    ["ALTERNANCE", "Alternance développement backend", "NovaTech Systems", "Développement d’API métier et refonte d’architecture backend.", ["Spring Boot", "REST API", "PostgreSQL", "Docker", "Git"]],
    ["CDD", "Développeur frontend", "Blue Ocean Studio", "Création d’interfaces web modernes et animées.", ["React", "TypeScript", "Tailwind CSS", "GSAP", "Vite"]],
    ["CDI", "Assistant logistique", "La Belle Vie", "Gestion opérationnelle de commandes, stocks et expéditions.", ["Organisation", "Coordination", "Contrôle qualité", "Gestion des stocks"]],
    ["VOLUNTEERING", "Projet associatif numérique", "Code Solidaire", "Aide à la création d’outils numériques pour une association.", ["Communication", "No-code", "Frontend", "Support utilisateur"]],
    ["SCHOOL", "Licence Informatique", "Université Le Havre Normandie", "Formation généraliste en informatique, algorithmique et développement.", ["Algorithmique", "Java", "OCaml", "SQL", "Compilation"]],
    ["INTERNSHIP", "Projet de recherche logiciel", "Laboratoire académique", "Expérimentation technique autour de la qualité logicielle.", ["Tests", "Refactoring", "Documentation", "Benchmark"]],
    ["CDD", "Mission DevOps", "CloudWorks", "Industrialisation de déploiements et supervision applicative.", ["Docker", "CI/CD", "Linux", "Monitoring", "Scripts Bash"]],
    ["CDI", "Projet produit SaaS", "ScaleUp Factory", "Participation à la construction d’un produit SaaS B2B.", ["Product", "API", "React", "Analytics", "Tests"]]
];

const PROJECT_THEMES = [
    ["Portfolio professionnel dynamique", "Spring Boot / React", "Site portfolio alimenté par un backend administrable.", ["Java", "Spring Boot", "React", "PostgreSQL", "Tailwind CSS"], ["Gestion dynamique du contenu", "Version active unique", "Historique des versions", "Mode administrateur", "Rendu public optimisé"]],
    ["Pipeline de données AIS", "Java / PostgreSQL / Python", "Chaîne de collecte et d’exploitation de données maritimes.", ["Java", "Python", "PostgreSQL", "Linux", "systemd"], ["Connexion TCP robuste", "Archivage de données", "Export CSV", "Filtrage spatio-temporel", "Supervision service Linux"]],
    ["Visualisation de graphes", "JavaFX / OpenGL / JNI", "Application de rendu de graphes avec moteur graphique natif.", ["Java", "JavaFX", "OpenGL", "C", "JNI"], ["Rendu natif OpenGL", "Interopérabilité Java / C", "Architecture MVC", "Contrôle caméra", "Optimisation graphique"]],
    ["Plateforme e-commerce modulaire", "Spring Boot / PostgreSQL", "API de gestion produits, commandes, utilisateurs et paiements.", ["Java", "Spring Boot", "PostgreSQL", "Docker", "JWT"], ["Authentification JWT", "Gestion commandes", "Catalogue produit", "Pagination", "Validation DTO"]],
    ["Dashboard analytique", "React / TypeScript / Charts", "Interface de suivi d’indicateurs métier avec visualisations.", ["React", "TypeScript", "Vite", "Mantine", "Chart.js"], ["Filtres dynamiques", "Graphiques interactifs", "Export de données", "Responsive design", "Tableaux paginés"]],
    ["Système de notification temps réel", "WebSocket / Redis", "Service de notifications instantanées pour applications web.", ["Java", "Spring Boot", "WebSocket", "Redis", "Docker"], ["Notifications temps réel", "Canaux utilisateurs", "Persistance minimale", "Reconnexion automatique", "Architecture scalable"]],
    ["Moteur de recherche interne", "Backend / Indexation", "Recherche full-text sur documents, profils et projets.", ["Java", "PostgreSQL", "Hibernate Search", "REST API"], ["Recherche textuelle", "Filtres combinés", "Score de pertinence", "Indexation automatique", "API REST"]],
    ["Application de gestion RH", "Full Stack", "Gestion des collaborateurs, contrats, absences et documents.", ["Spring Boot", "React", "PostgreSQL", "Docker"], ["Gestion utilisateurs", "Gestion documents", "Workflow validation", "Historique des actions", "Exports administratifs"]],
    ["Service de fichiers", "Storage / Upload", "API de stockage local ou cloud pour documents et médias.", ["Java", "Spring Boot", "Multipart", "Local Storage"], ["Upload sécurisé", "Téléchargement contrôlé", "Validation MIME", "Nommage unique", "Gestion des métadonnées"]],
    ["API de réservation", "Spring Boot / PostgreSQL", "Système de réservation avec disponibilités et créneaux.", ["Java", "Spring Boot", "PostgreSQL", "Validation"], ["Gestion créneaux", "Contrôle conflits", "Annulation", "Historique utilisateur", "Règles métier transactionnelles"]],
    ["Client mobile prototype", "React Native / API", "Application mobile connectée à une API métier.", ["React Native", "TypeScript", "REST API", "Expo"], ["Connexion API", "Navigation mobile", "Formulaires", "Cache local", "Design responsive"]],
    ["Système de monitoring", "Logs / Metrics", "Collecte d’informations techniques sur des services backend.", ["Java", "Actuator", "Prometheus", "Grafana", "Docker"], ["Health checks", "Métriques applicatives", "Logs structurés", "Alerting", "Dashboard technique"]],
    ["Orchestrateur de tâches", "Scheduler / Workers", "Service de planification de tâches asynchrones.", ["Java", "Spring Boot", "Quartz", "PostgreSQL", "Docker"], ["Planification récurrente", "Gestion des retries", "Statuts d’exécution", "Logs métier", "Back-office de supervision"]],
    ["Application de facturation", "Billing / PDF", "Gestion de devis, factures et exports PDF.", ["Java", "Spring Boot", "PostgreSQL", "PDFBox", "React"], ["Création de factures", "Export PDF", "Numérotation automatique", "Calcul TVA", "Historique client"]]
];

function buildProfile(persona, ownerIndex, versionNumber, suffix) {
    return {
        title: versionNumber === 1 ? persona.title : `${persona.title} — Version ${versionNumber}`,
        subtitle: persona.mainStacks.slice(0, 4).join(" / "),
        headline: `${persona.firstName} ${persona.name}, profil orienté ${persona.domain}.`,
        shortDescription: `Portfolio généré automatiquement pour tester la version ${versionNumber}.`,
        description:
            `Mock complet pour ${persona.firstName} ${persona.name}. Cette version met en avant un profil ${persona.domain}, ` +
            `plusieurs expériences, des projets détaillés, des compétences techniques et des liens externes. ` +
            `Elle sert à tester le rendu frontend, la profondeur des DTO et la logique de version active.`,
        location: persona.location,
        availability: versionNumber === 1 ? "Disponible pour opportunités professionnelles" : `Version ${versionNumber} utilisée pour tester l’historique du site`,
        profileImageUrl: buildImageUrl("profiles", ownerIndex, 1, versionNumber),
        logoUrl: buildImageUrl("logos", ownerIndex, 1, versionNumber),
        cvUrl: `https://example.com/mock/cv/${slugify(persona.firstName)}-${slugify(persona.name)}-v${versionNumber}.pdf`,
        portfolioUrl: `https://example.com/mock/portfolio/${slugify(persona.firstName)}-${slugify(persona.name)}-v${versionNumber}-${suffix}`
    };
}

function buildContacts(persona, ownerIndex, suffix) {
    const base = `${slugify(persona.firstName)}.${slugify(persona.name)}.${suffix}.${ownerIndex}`;

    return [
        { type: "EMAIL", value: `${base}@example.com` },
        { type: "GITHUB", value: `https://github.com/${base}` },
        { type: "LINKEDIN", value: `https://www.linkedin.com/in/${base}` },
        { type: "PORTFOLIO", value: `https://portfolio.example.com/${base}` }
    ];
}

function buildExperiences(persona, ownerIndex, versionNumber, count) {
    return Array.from({ length: count }, (_, index) => {
        const [category, title, organization, summary, skills] = pick(EXPERIENCE_THEMES, index + ownerIndex + versionNumber);
        const year = 2020 + ((index + versionNumber) % 5);
        const startMonth = String(((index + 1) % 12) + 1).padStart(2, "0");
        const currentPosition = index === 0 && versionNumber >= 2;

        return {
            category,
            title: `${title} — ${persona.domain}`,
            organization,
            location: pick(["Paris", "Lyon", "Marseille", "Nantes", "Bordeaux", "Toulouse", "Lille", "Strasbourg"], ownerIndex + index),
            summary,
            description:
                `${summary} Missions principales : analyse du besoin, conception, développement, tests, documentation, ` +
                `amélioration continue et collaboration avec l’équipe. Cette expérience est générée pour charger fortement ` +
                `la timeline et vérifier l’affichage côté frontend.`,
            startDate: `${year}-${startMonth}-01`,
            endDate: currentPosition ? null : `${year + 1}-${String(((index + 4) % 12) + 1).padStart(2, "0")}-28`,
            currentPosition,
            imageUrl: buildImageUrl("experiences", ownerIndex, index + 1, versionNumber),
            websiteUrl: `https://example.com/mock/experience/${ownerIndex}-${versionNumber}-${index + 1}`,
            skills: [...new Set([...skills, ...persona.mainStacks.slice(0, 4), "Communication", "Documentation", "Tests", "Git"])],
            displayOrder: index + 1
        };
    });
}

function buildProjects(persona, ownerIndex, versionNumber, count) {
    return Array.from({ length: count }, (_, index) => {
        const [title, subtitle, summary, stacks, features] = pick(PROJECT_THEMES, index + ownerIndex + versionNumber);
        const projectSlug = slugify(`${persona.firstName}-${persona.name}-${title}-v${versionNumber}-${index + 1}`);
        const startMonth = String(((index + 2) % 12) + 1).padStart(2, "0");

        return {
            title: `${title} — ${persona.firstName} ${persona.name} V${versionNumber}.${index + 1}`,
            subtitle,
            shortDescription: summary,
            description:
                `${summary} Projet généré automatiquement pour tester un affichage riche. Il contient plusieurs stacks, ` +
                `features, liens, images, statuts et ordres d’affichage. Objectif : simuler un portfolio réel avec beaucoup ` +
                `de contenu et vérifier que le backend supporte correctement les versions multiples du site.`,
            status: pick(["COMPLETED", "IN_PROGRESS"], index + versionNumber),
            startDate: `${2022 + (index % 4)}-${startMonth}-01`,
            endDate: index % 3 === 0 ? null : `${2024 + (index % 3)}-${String(((index + 6) % 12) + 1).padStart(2, "0")}-28`,
            imageUrl: buildImageUrl("projects", ownerIndex, index + 1, versionNumber),
            demoUrl: `https://example.com/demo/${projectSlug}`,
            githubUrl: `https://github.com/mock/${projectSlug}`,
            documentationUrl: `https://example.com/docs/${projectSlug}`,
            stacks: [...new Set([...stacks, ...persona.mainStacks, "REST API", "Git", "CI/CD"])],
            features: [...features, "DTO validation", "Mapping manuel", "Gestion des erreurs", "Tests API", "Données mockées", "Responsive design"],
            links: [
                { type: "GITHUB", label: "Code source", url: `https://github.com/mock/${projectSlug}` },
                { type: "DEMO", label: "Démo", url: `https://example.com/demo/${projectSlug}` }
            ],
            featured: index < 4,
            published: true,
            displayOrder: index + 1,
            websiteVersionId: null
        };
    });
}

function createOwnerPayload(ownerIndex, suffix) {
    const persona = pick(PERSONAS, ownerIndex);
    const versionNumber = 1;

    return {
        name: `${persona.name}_MOCK_${suffix}_${ownerIndex}`,
        firstName: persona.firstName,
        age: 22 + ownerIndex,
        active: true,
        address: persona.location,
        contacts: buildContacts(persona, ownerIndex, suffix),
        versionTag: "v1",
        versionLabel: "Version initiale complète",
        versionDescription: "Première version générée automatiquement avec un gros volume de données.",
        versionPublished: true,
        prof: buildProfile(persona, ownerIndex, versionNumber, suffix),
        timeline: {
            title: `Parcours complet — ${persona.firstName} ${persona.name}`,
            description: `Timeline complète générée automatiquement pour ${persona.firstName}. Elle contient plusieurs formations, expériences professionnelles et missions techniques.`,
            experiences: buildExperiences(persona, ownerIndex, versionNumber, MOCK_EXPERIENCES)
        },
        projects: buildProjects(persona, ownerIndex, versionNumber, MOCK_PROJECTS)
    };
}

function createWebsiteVersionPayload(ownerIndex, versionNumber, suffix) {
    const persona = pick(PERSONAS, ownerIndex);

    return {
        versionTag: `v${versionNumber}`,
        label: `Version ${versionNumber} — ${persona.domain}`,
        description:
            `Version ${versionNumber} générée automatiquement pour tester l’historique du site, la bascule active ` +
            `et le rendu avec beaucoup de contenu.`,
        active: false,
        published: true,
        prof: buildProfile(persona, ownerIndex, versionNumber, suffix),
        timeline: {
            title: `Timeline version ${versionNumber} — ${persona.domain}`,
            description: `Timeline enrichie pour la version ${versionNumber}. Elle permet de vérifier que chaque version possède bien son propre contenu.`,
            experiences: buildExperiences(persona, ownerIndex, versionNumber, MOCK_EXPERIENCES + versionNumber)
        },
        projects: buildProjects(persona, ownerIndex, versionNumber, MOCK_PROJECTS + versionNumber)
    };
}

async function findCreatedOwnerId(ownerName) {
    const owners = await request("GET", "/manager");

    if (!Array.isArray(owners)) {
        throw new Error("GET /manager doit retourner une liste pour retrouver le owner créé");
    }

    const owner = owners.find(o => o.name === ownerName);

    if (!owner) {
        throw new Error(`Owner introuvable après création : ${ownerName}`);
    }

    return owner.ownerId ?? owner.id;
}

function extractVersionId(version) {
    return version?.id ?? version?.websiteVersionId;
}

async function createOwnerWithVersions(ownerIndex, suffix) {
    const ownerPayload = createOwnerPayload(ownerIndex, suffix);

    console.log(`Création owner ${ownerIndex + 1}/${MOCK_USERS} : ${ownerPayload.firstName} ${ownerPayload.name}`);

    const createdOwnerResponse = await request("POST", "/manager", ownerPayload);

    let ownerId = createdOwnerResponse?.ownerId ?? createdOwnerResponse?.id;

    if (!ownerId) {
        ownerId = await findCreatedOwnerId(ownerPayload.name);
    }

    assert(ownerId, `ownerId introuvable pour ${ownerPayload.name}`);

    const ownerResult = {
        ownerId,
        name: ownerPayload.name,
        versions: []
    };

    const initialVersions = await request("GET", `/manager/${ownerId}/versions`);

    assert(Array.isArray(initialVersions), `GET /manager/${ownerId}/versions doit retourner une liste`);
    assert(initialVersions.length >= 1, `Owner ${ownerId} doit avoir au moins une version après création`);

    const v1 = initialVersions.find(v => v.versionTag === "v1") ?? initialVersions[0];
    const v1Id = extractVersionId(v1);

    assert(v1Id, `versionId introuvable pour la version initiale de owner ${ownerId}`);
    assert(v1.active === true, `La version initiale de owner ${ownerId} doit être active`);

    ownerResult.versions.push({ id: v1Id, tag: v1.versionTag, active: v1.active });

    for (let versionNumber = 2; versionNumber <= MOCK_VERSIONS; versionNumber++) {
        const versionPayload = createWebsiteVersionPayload(ownerIndex, versionNumber, suffix);

        console.log(`Création version ${versionPayload.versionTag} pour owner ${ownerId}`);

        const createdVersion = await request("POST", `/manager/${ownerId}/versions`, versionPayload);
        const versionId = extractVersionId(createdVersion);

        assert(versionId, `versionId introuvable pour owner ${ownerId}, version ${versionPayload.versionTag}`);

        ownerResult.versions.push({ id: versionId, tag: versionPayload.versionTag, active: false });
    }

    const versionsBeforeActivation = await request("GET", `/manager/${ownerId}/versions`);
    const activeBeforeActivation = versionsBeforeActivation.filter(v => v.active === true);

    assert(activeBeforeActivation.length === 1, `Avant bascule, owner ${ownerId} doit avoir exactement une version active`);

    const lastVersion = ownerResult.versions[ownerResult.versions.length - 1];

    console.log(`Activation de la dernière version pour owner ${ownerId} : ${lastVersion.tag}`);

    await request("PUT", `/manager/${ownerId}/versions/${lastVersion.id}/activate`);

    const versionsAfterActivation = await request("GET", `/manager/${ownerId}/versions`);
    const activeVersions = versionsAfterActivation.filter(v => v.active === true);

    assert(activeVersions.length === 1, `Owner ${ownerId} doit avoir exactement une seule version active`);
    assert(activeVersions[0].versionTag === lastVersion.tag, `La version active attendue pour owner ${ownerId} est ${lastVersion.tag}`);

    const activeVersion = await request("GET", `/manager/${ownerId}/versions/active`);

    assert(activeVersion.versionTag === lastVersion.tag, `GET /active doit retourner ${lastVersion.tag} pour owner ${ownerId}`);

    const publicWebsite = await request("GET", `/website/${ownerId}`);

    assert(publicWebsite, `GET /website/${ownerId} doit retourner un site public`);

    ownerResult.activeVersion = {
        id: extractVersionId(activeVersion),
        tag: activeVersion.versionTag
    };

    return ownerResult;
}

async function seedLargeMockDataset() {
    const suffix = uniqueSuffix();
    const createdOwners = [];

    console.log(`Base URL : ${BASE_URL}`);
    console.log("Seed large mock dataset");
    console.log({ MOCK_USERS, MOCK_VERSIONS, MOCK_PROJECTS, MOCK_EXPERIENCES, TEST_DEFAULT_WEBSITE });

    for (let ownerIndex = 0; ownerIndex < MOCK_USERS; ownerIndex++) {
        const ownerResult = await createOwnerWithVersions(ownerIndex, suffix);
        createdOwners.push(ownerResult);
    }

    if (TEST_DEFAULT_WEBSITE) {
        console.log("Test endpoint public par défaut : GET /website");
        const defaultWebsite = await request("GET", "/website");
        assert(defaultWebsite, "GET /website doit retourner un site public par défaut");
    }

    console.log("Large mock dataset créé avec succès");
    console.log(JSON.stringify(createdOwners, null, 2));

    return createdOwners;
}

async function main() {
    await seedLargeMockDataset();
}

main().catch(error => {
    console.error("Erreur inattendue pendant le seed mock");
    console.error(error);
    process.exit(1);
});