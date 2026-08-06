# Tests et CI du backend

## Prérequis

- Java 21 ;
- Docker actif, car PostgreSQL est créé par Testcontainers ;
- aucune variable Aiven n'est nécessaire.

## Commandes

```bash
./mvnw test
./mvnw clean verify
```

`clean verify` exécute les tests, les tests d'intégration PostgreSQL/Flyway, puis génère le rapport JaCoCo dans `target/site/jacoco`.

## Périmètre initial

- démarrage du contexte Spring avec le profil `test` ;
- PostgreSQL 16 isolé et jetable ;
- exécution réelle des migrations Flyway sur une base vide ;
- normalisation des locales ;
- traduction publiée, fallback français et traduction obsolète ;
- API publique ;
- service de publication du portfolio ;
- format commun des erreurs ;
- identifiant `X-Request-ID` ;
- réponse sûre pour les erreurs inattendues.

## Secret GitHub pour le déploiement

- `RENDER_DEPLOY_HOOK_URL`

Sans ce secret, les tests et la production du JAR fonctionnent ; le déploiement est ignoré.
