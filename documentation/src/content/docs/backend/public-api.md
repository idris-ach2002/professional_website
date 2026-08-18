---
title: API publique
description: Endpoints accessibles sans session et contrats de lecture du portfolio.
sidebar:
  order: 3
---
## Portfolio

<div class="architecture-frame">
  <img src="/diagrams/backend-route-map.svg" alt="Carte des routes publiques et protégées du backend." />
  <div class="architecture-caption">Classification Spring Security de la surface HTTP.</div>
</div>

| Méthode | Route | Usage |
|---|---|---|
| GET | `/website` | Liste des portfolios publics. |
| GET | `/website/default` | Portfolio public par défaut, localisable. |
| GET | `/website/default/seo-snapshot` | Snapshot bilingue pour build SEO. |
| GET | `/website/{ownerId}` | Portfolio public d’un owner. |
| GET | `/website/default/projects/{projectSlug}` | Étude de cas publique. |
| GET | `/website/{ownerId}/projects/{projectSlug}` | Étude de cas d’un owner. |
| GET | `/website/items-visibility` | Configuration publique de visibilité UI. |

## Ingestion et engineering

| Méthode | Route | Usage |
|---|---|---|
| POST | `/analytics/events` | Événement analytics public validé et limité. |
| GET | `/api/engineering/mission-control` | Snapshot technique du backend. |
| GET | `/api/engineering/mission-control/queue` | Page d’une file technique. |
| GET | `/api/engineering/performance/history` | Historique de télémétrie. |
| POST | `/api/engineering/performance/samples` | Échantillon runtime frontend. |
| GET | `/actuator/health` | Santé du service. |

## Cache

Les endpoints `/website/**` s’appuient sur Caffeine. Une publication ou modification pertinente déclenche une invalidation plutôt qu’une attente d’expiration comme seul mécanisme de cohérence.
