const BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";
const AUTH_TOKEN = process.env.API_TOKEN ?? null;
const RUN_PROD = process.env.RUN_PROD === "true";

if (BASE_URL.includes("prod") && !RUN_PROD) {
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

function createOwnerPayload(suffix) {
    return {
        name: `TEST_OWNER_${suffix}`,
        firstName: "Smoke",
        age: 23,
        active: true,
        address: "Paris, France",
        contacts: [
            {
                type: "EMAIL",
                value: `smoke.test.${suffix}@example.com`
            },
            {
                type: "GITHUB",
                value: "https://github.com/test/smoke"
            },
            {
                type: "LINKEDIN",
                value: "https://linkedin.com/in/smoke-test"
            }
        ],
        versionTag: "v1",
        versionLabel: "Version initiale smoke test",
        versionDescription: "Version créée automatiquement par le smoke test.",
        versionPublished: true,
        prof: {
            title: "Développeur Full Stack",
            subtitle: "Java / Spring Boot / React",
            headline: "Profil généré automatiquement pour tester l’API.",
            shortDescription: "Smoke test portfolio.",
            description: "Donnée temporaire utilisée pour vérifier le fonctionnement du backend.",
            location: "Paris",
            availability: "Test uniquement",
            profileImageUrl: "https://example.com/profile.jpg",
            logoUrl: "https://example.com/logo.png",
            cvUrl: "https://example.com/cv.pdf",
            portfolioUrl: "https://example.com"
        },
        timeline: {
            title: "Parcours smoke test",
            description: "Timeline générée automatiquement.",
            experiences: [
                {
                    category: "SCHOOL",
                    title: "Formation test",
                    organization: "Université test",
                    location: "Paris",
                    summary: "Résumé test",
                    description: "Description test",
                    startDate: "2025-09-01",
                    endDate: "2027-09-01",
                    currentPosition: true,
                    imageUrl: "https://example.com/school.png",
                    websiteUrl: "https://example.com",
                    skills: ["Java", "Spring Boot", "React"],
                    displayOrder: 1
                }
            ]
        },
        projects: [
            {
                title: "Projet smoke test v1",
                subtitle: "Spring Boot / React",
                shortDescription: "Projet généré automatiquement.",
                description: "Projet temporaire utilisé pour vérifier l’insertion.",
                status: "IN_PROGRESS",
                startDate: "2026-01-01",
                endDate: null,
                imageUrl: "https://example.com/project.png",
                demoUrl: "https://example.com/demo",
                githubUrl: "https://github.com/test/project",
                documentationUrl: "https://example.com/docs",
                stacks: ["Java", "Spring Boot", "React"],
                features: ["Création owner", "Version active", "Smoke test"],
                links: [
                    {
                        type: "GITHUB",
                        label: "Code source",
                        url: "https://github.com/test/project"
                    }
                ],
                featured: true,
                published: true,
                displayOrder: 1,
                websiteVersionId: null
            }
        ]
    };
}

function createWebsiteVersionPayload() {
    return {
        versionTag: "v2",
        label: "Version smoke test v2",
        description: "Deuxième version créée automatiquement.",
        active: false,
        published: true,
        prof: {
            title: "Développeur logiciel",
            subtitle: "Java / Architecture / Backend",
            headline: "Profil v2 généré automatiquement.",
            shortDescription: "Smoke test version 2.",
            description: "Version utilisée pour tester la bascule active.",
            location: "Paris",
            availability: "Test uniquement",
            profileImageUrl: "https://example.com/profile-v2.jpg",
            logoUrl: "https://example.com/logo-v2.png",
            cvUrl: "https://example.com/cv-v2.pdf",
            portfolioUrl: "https://example.com/v2"
        },
        timeline: {
            title: "Timeline v2",
            description: "Timeline de la version 2.",
            experiences: [
                {
                    category: "INTERNSHIP",
                    title: "Stage test",
                    organization: "Organisation test",
                    location: "Paris",
                    summary: "Résumé stage test",
                    description: "Description stage test",
                    startDate: "2025-04-01",
                    endDate: "2025-06-30",
                    currentPosition: false,
                    imageUrl: "https://example.com/internship.png",
                    websiteUrl: "https://example.com",
                    skills: ["Java", "PostgreSQL", "Linux"],
                    displayOrder: 1
                }
            ]
        },
        projects: [
            {
                title: "Projet smoke test v2",
                subtitle: "Backend / Data",
                shortDescription: "Projet v2 généré automatiquement.",
                description: "Projet temporaire utilisé pour tester une deuxième version.",
                status: "COMPLETED",
                startDate: "2025-04-01",
                endDate: "2025-06-30",
                imageUrl: "https://example.com/project-v2.png",
                demoUrl: "https://example.com/demo-v2",
                githubUrl: "https://github.com/test/project-v2",
                documentationUrl: "https://example.com/docs-v2",
                stacks: ["Java", "PostgreSQL", "Python"],
                features: ["Versioning", "Activation", "Historique"],
                links: [
                    {
                        type: "GITHUB",
                        label: "Code source",
                        url: "https://github.com/test/project-v2"
                    }
                ],
                featured: true,
                published: true,
                displayOrder: 1,
                websiteVersionId: null
            }
        ]
    };
}

async function main() {
    const suffix = uniqueSuffix();

    console.log(`Base URL : ${BASE_URL}`);
    console.log(`Création owner smoke test : TEST_OWNER_${suffix}`);

    const createdOwner = await request("POST", "/manager", createOwnerPayload(suffix));

    let ownerId = createdOwner?.ownerId ?? createdOwner?.id;

    if (!ownerId) {
        const owners = await request("GET", "/manager");
        const owner = Array.isArray(owners)
            ? owners.find(o => o.name === `TEST_OWNER_${suffix}`)
            : null;

        ownerId = owner?.ownerId ?? owner?.id;
    }

    assert(ownerId, "Impossible de récupérer ownerId après création");

    console.log(`Owner créé : ${ownerId}`);

    const versionsAfterOwnerCreation = await request("GET", `/manager/${ownerId}/versions`);

    assert(
        Array.isArray(versionsAfterOwnerCreation),
        "GET /manager/{ownerId}/versions doit retourner une liste"
    );

    assert(
        versionsAfterOwnerCreation.length === 1,
        "Après création owner, il doit y avoir exactement une version"
    );

    const v1 = versionsAfterOwnerCreation[0];

    assert(v1.active === true, "La première version doit être active");
    assert(v1.versionTag === "v1", "La première version doit avoir versionTag = v1");

    const activeV1 = await request("GET", `/manager/${ownerId}/versions/active`);

    assert(activeV1.active === true, "GET /active doit retourner une version active");
    assert(activeV1.versionTag === "v1", "La version active initiale doit être v1");

    console.log("Création de la deuxième version");

    const createdV2 = await request(
        "POST",
        `/manager/${ownerId}/versions`,
        createWebsiteVersionPayload()
    );

    const version2Id = createdV2?.id ?? createdV2?.websiteVersionId;

    assert(version2Id, "Impossible de récupérer l'id de la version v2");

    const versionsBeforeSwitch = await request("GET", `/manager/${ownerId}/versions`);

    assert(
        versionsBeforeSwitch.length === 2,
        "Après création v2, il doit y avoir deux versions"
    );

    const activeBeforeSwitch = versionsBeforeSwitch.filter(v => v.active === true);

    assert(
        activeBeforeSwitch.length === 1,
        "Avant bascule, il doit y avoir exactement une version active"
    );

    assert(
        activeBeforeSwitch[0].versionTag === "v1",
        "Avant bascule, v1 doit être active"
    );

    console.log(`Activation de la version v2 : ${version2Id}`);

    const activatedV2 = await request(
        "PUT",
        `/manager/${ownerId}/versions/${version2Id}/activate`
    );

    assert(activatedV2.active === true, "La version activée doit avoir active = true");
    assert(activatedV2.versionTag === "v2", "La version activée doit être v2");

    const versionsAfterSwitch = await request("GET", `/manager/${ownerId}/versions`);

    const activeAfterSwitch = versionsAfterSwitch.filter(v => v.active === true);

    assert(
        activeAfterSwitch.length === 1,
        "Après bascule, il doit rester exactement une seule version active"
    );

    assert(
        activeAfterSwitch[0].versionTag === "v2",
        "Après bascule, v2 doit être la version active"
    );

    const activeV2 = await request("GET", `/manager/${ownerId}/versions/active`);

    assert(activeV2.versionTag === "v2", "GET /active doit retourner v2 après activation");

    console.log("Test endpoint public");

    const publicWebsite = await request("GET", `/website/${ownerId}`);

    assert(publicWebsite, "GET /website/{ownerId} doit retourner une réponse");

    console.log("Smoke test terminé avec succès");
    console.log({
        ownerId,
        activeVersion: activeV2.versionTag,
        activeVersionId: activeV2.id ?? activeV2.websiteVersionId
    });
}

main().catch(error => {
    console.error("Erreur inattendue pendant le smoke test");
    console.error(error);
    process.exit(1);
});