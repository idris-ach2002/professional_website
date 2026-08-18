---
title: Variables Backend
description: Catalogue des variables de datasource, sécurité, stockage, concurrence et observabilité.
sidebar:
  order: 2
---
## Règle générale

Les valeurs ci-dessous appartiennent à l’environnement du processus Spring Boot. Les credentials réels ne doivent jamais être commités. Les fichiers d’exemple servent uniquement de catalogue de clés.

## Serveur et datasource

| Variable | Rôle |
|---|---|
| `PORT` | port HTTP fourni par la plateforme |
| `SPRING_DATASOURCE_URL` | URL JDBC PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | utilisateur PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | secret PostgreSQL |
| `DB_POOL_MAX_SIZE` | plafond Hikari |
| `DB_POOL_MIN_IDLE` | connexions idle minimales |
| `DB_POOL_CONNECTION_TIMEOUT_MS` | attente d’une connexion |
| `DB_POOL_VALIDATION_TIMEOUT_MS` | validation de connexion |
| `DB_POOL_IDLE_TIMEOUT_MS` | durée idle |
| `DB_POOL_MAX_LIFETIME_MS` | durée de vie maximale |
| `DB_POOL_KEEPALIVE_TIME_MS` | keepalive |

## JPA et Hibernate

`JPA_DDL_AUTO`, `JPA_SHOW_SQL`, `HIBERNATE_FORMAT_SQL`, `HIBERNATE_HIGHLIGHT_SQL`, `HIBERNATE_GENERATE_STATISTICS`, `HIBERNATE_BATCH_FETCH_SIZE` et `HIBERNATE_JDBC_BATCH_SIZE` contrôlent validation, logs et batching. En production, le schéma doit rester sous autorité Flyway.

## Sécurité et frontend

| Variable | Rôle |
|---|---|
| `PORTFOLIO_ADMIN_USERNAME` | compte administrateur |
| `PORTFOLIO_ADMIN_PASSWORD` | secret administrateur |
| `APP_CORS_ALLOWED_ORIGIN(S)` | allowlist CORS |
| `APP_FRONTEND_ALLOWED_ORIGINS` | origines autorisées pour les redirections/flows frontend |
| `APP_FRONTEND_ORIGIN` | origine frontend de référence |
| `SESSION_TIMEOUT` | durée de session |

## Storage

`STORAGE_PROVIDER`, `UPLOAD_DIR`, `APP_UPLOAD_MAX_BYTES`, `MAX_FILE_SIZE`, `MAX_REQUEST_SIZE`, `MAX_SWALLOW_SIZE`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` et `CLOUDINARY_FOLDER` pilotent les médias.

## Traduction

`LIBRETRANSLATE_ENABLED`, `LIBRETRANSLATE_BASE_URL` et `LIBRETRANSLATE_TIMEOUT` pilotent l’intégration serveur. L’image de conteneur utilisée en local est configurée séparément par Compose.

## Analytics et exécuteurs

`APP_ANALYTICS_HASH_SECRET`, `APP_ANALYTICS_QUEUE_CAPACITY`, `APP_ANALYTICS_BATCH_SIZE`, `APP_ANALYTICS_FLUSH_INTERVAL_MS`, `APP_ANALYTICS_MAX_EVENTS_PER_MINUTE`, `APP_ANALYTICS_RETENTION_DAYS`, `APP_VIRTUAL_IO_MAX_CONCURRENCY`, ainsi que les paramètres de maintenance, bornent les travaux asynchrones.

## Secrets

Aucune de ces valeurs secrètes ne doit être copiée dans des variables `VITE_*`. Le navigateur ne doit connaître que l’URL publique du backend et les URLs publiques de médias.
