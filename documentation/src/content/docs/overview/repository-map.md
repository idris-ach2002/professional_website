---
title: Carte des dépôts
description: Où trouver les responsabilités principales dans les deux projets.
sidebar:
  order: 3
---
## Dépôt backend Spring Boot

La documentation ne duplique pas chaque fichier ; elle regroupe les sources par responsabilité et indique les points d’entrée qui structurent le système.

### Backend

- `src/main/java/.../controller` : API portfolio et administration.
- `service` : services cœur pour lecture publique et snapshots éditoriaux.
- `publication`, `jobs`, `events`, `audit` : pipeline de publication fiable.
- `analytics` et `engineering` : ingestion et observabilité.
- `translation` : localisation persistée et provider privé.
- `upload` : abstraction de stockage local/Cloudinary.
- `security` : session, CORS, CSRF et redirections.
- `repository`, `entity`, `dto`, `mapper` : persistence et contrats de données.
- `src/main/resources/db/migration` : évolution Flyway du schéma.
- `infra/` et `docker-compose.yml` : environnement local et services privés.
- `.github/workflows/` : vérification et déploiement.

## Dépôt complémentaire

Le frontend React n’est pas une boîte noire. Les pages **Front ↔ Back** documentent les endpoints effectivement consommés, les credentials, les erreurs et les flux de publication. La carte globale relie les modules des deux dépôts sans imposer de couplage de build entre eux.
