---
title: Sécurité Backend
description: SecurityFilterChain, session, CORS, CSRF, rôles et réponses sensibles.
sidebar:
  order: 8
---
<div class="architecture-frame">
  <img src="/diagrams/security-boundaries.svg" alt="Routes publiques, rôle administrateur et données sensibles." />
  <div class="architecture-caption">Routes publiques, rôle administrateur et données sensibles.</div>
</div>


## Politique par défaut

`anyRequest().denyAll()` ferme toute route qui n’a pas été explicitement classée. Les preflights CORS sont autorisés ; les routes statiques/login et les lectures publiques sont ensuite listées explicitement.

## Rôle administrateur

`/manager/**`, `/api/**` administratives et `/uploads/**` nécessitent `ROLE_ADMIN`. `/v3/api-docs/**` et `/actuator/prometheus` sont également protégés.

## CSRF

Spring Security gère CSRF pour les mutations de session. Les deux endpoints d’ingestion qui doivent être publics sont explicitement ignorés. Le frontend récupère `/csrf` après authentification avant une écriture.

## CORS et redirections

`UrlBasedCorsConfigurationSource` est construit à partir d’une allowlist d’origines. Les redirections post-login sont validées séparément pour éviter les open redirects.
