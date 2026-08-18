---
title: Configuration Spring
description: Propriétés applicatives, datasource, cache, uploads, analytics et intégrations.
---
## Sources

La configuration principale vit dans `application.yaml` et les fichiers de profil. Les valeurs sensibles ou dépendantes de la plateforme sont externalisées par variables d’environnement. Les objets de configuration Spring traduisent ensuite ces valeurs en composants typés.

## Groupes principaux

| Groupe | Responsabilité |
|---|---|
| Datasource/Hikari | URL JDBC, credentials et pool borné |
| JPA/Hibernate | validation du schéma, batching, fetch et diagnostics SQL |
| Flyway | application ordonnée des migrations |
| Security | compte administrateur, CORS, origine frontend et session |
| Cache | caches Caffeine des lectures publiques |
| Upload/Storage | taille, répertoire local ou Cloudinary |
| Analytics | queue, batch, rate limit, retention et secret de hash |
| Concurrency | lane virtual-thread et bornes des exécutants |
| Translation | activation, URL privée et timeout LibreTranslate |
| Observability | Actuator, Prometheus et metrics runtime |

## Production

La production doit préférer `JPA_DDL_AUTO=validate`. Le schéma appartient aux migrations, tandis que les credentials et capacités de pool appartiennent à l’environnement de la plateforme.
