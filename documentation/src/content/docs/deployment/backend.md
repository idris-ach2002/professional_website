---
title: Déploiement Backend
description: Docker, Render, datasource, services externes et health probes.
sidebar:
  order: 4
---
## Image

Le Dockerfile compile avec le Maven Wrapper puis copie uniquement le JAR dans une image runtime. Le conteneur crée le dossier d’upload, passe à un utilisateur non privilégié et démarre `java -jar`.

## Variables essentielles

La production fournit datasource PostgreSQL, identité admin, CORS/frontend origin, secret analytics, provider de stockage et credentials Cloudinary, paramètres LibreTranslate et limites de concurrence.

## Render

Le workflow backend exécute `clean verify`, publie le JAR comme artifact puis peut appeler `RENDER_DEPLOY_HOOK_URL` sur la branche principale. Render reconstruit alors le conteneur depuis le dépôt.

## Probes

`/actuator/health` doit répondre sans nécessiter la session admin. Les métriques Prometheus restent protégées.
