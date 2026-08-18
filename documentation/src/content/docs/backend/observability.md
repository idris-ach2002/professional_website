---
title: Observabilité Backend
description: Actuator, Prometheus, profiler, Mission Control et échantillons runtime.
sidebar:
  order: 14
---
## Actuator

<div class="architecture-frame">
  <img src="/diagrams/observability-path.svg" alt="Chaîne d’observabilité du navigateur au backend et Mission Control." />
  <div class="architecture-caption">Request IDs, metrics, persistence et surfaces d’exploitation.</div>
</div>

`health` est public pour les probes ; Prometheus est protégé. Les métriques HTTP configurent des histogrammes et SLOs afin de rendre les distributions de latence observables.

## Profilage de route

`BackendRouteProfilerFilter` et `BackendRouteProfiler` collectent des informations sur les appels. Les endpoints engineering peuvent exposer `Server-Timing` et une trace de composants consommée par Mission Control frontend.

## MissionControlService

Le snapshot agrège système d’exploitation, CPU, mémoire, JVM, stockage, état PostgreSQL, caches Caffeine, queue analytics, jobs, outbox et publications.


<div class="architecture-frame">
  <img src="/diagrams/analytics-mission-control.svg" alt="Observabilité corrélée frontend et backend." />
  <div class="architecture-caption">Observabilité corrélée frontend et backend.</div>
</div>
