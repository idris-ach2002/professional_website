---
title: Architecture Backend
description: Couches Spring Boot, modules métier, persistence et intégrations.
sidebar:
  order: 1
---
## Vue modulaire


<div class="architecture-frame">
  <img src="/diagrams/backend-modules.svg" alt="Controllers, services, reliability, persistence et services externes." />
  <div class="architecture-caption">Controllers, services, reliability, persistence et services externes.</div>
</div>


Le backend suit une architecture Spring Boot modulaire par capacité métier plutôt qu’un seul paquet service/controller géant. Les domaines analytics, publication, events, jobs, translation, upload, visibility et engineering possèdent leurs propres contrôleurs, services, DTO ou repositories lorsque nécessaire.

## Couches transversales

- **Security** : filtre Spring Security, CSRF, CORS, login, redirections, no-store.
- **Exception** : erreurs API structurées et request ID.
- **Concurrency** : exécuteur borné, virtual threads pour I/O, tags d’entité.
- **Cache** : Caffeine pour les lectures publiques et invalidation événementielle.
- **Observability** : Actuator, Prometheus, profiler de routes et metrics d’exécuteurs.
- **Configuration** : Jackson, OpenAPI, cache public et propriétés typées.

## Transactions

Les services de lecture publique sont `readOnly`. Les modifications de snapshots, publication, jobs et outbox utilisent des transactions explicites. La concurrence optimiste JPA et les préconditions métier protègent les révisions.
