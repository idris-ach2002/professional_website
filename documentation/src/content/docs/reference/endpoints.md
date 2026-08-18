---
title: Catalogue des endpoints
description: Méthodes HTTP, familles de routes et politique d’accès du backend.
sidebar:
  order: 1
---
## Carte de sécurité

<div class="architecture-frame">
  <img src="/diagrams/backend-route-map.svg" alt="Surface HTTP publique et administrée." />
  <div class="architecture-caption">Les routes non classées sont refusées par défaut.</div>
</div>

## Entrées générales

| Méthode | Route | Accès | Rôle |
|---|---|---|---|
| `GET` | `/` | Public | entrée serveur / redirection contrôlée |
| `GET` | `/login` | Public | formulaire de connexion |
| `POST` | `/login` | Public | traitement Spring Security |
| `POST` | `/logout` | Session | déconnexion |
| `GET` | `/csrf` | Session | jeton CSRF utilisé par l’administration |
| `GET` | `/actuator/health` | Public | probe de santé |
| `GET` | `/actuator/prometheus` | `ADMIN` | métriques Prometheus |
| `GET` | `/v3/api-docs/**` | `ADMIN` | contrat OpenAPI |

## Portfolio public

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/website` | liste/entrée publique selon contrôleur |
| `GET` | `/website/default?locale=...` | snapshot public du owner par défaut |
| `GET` | `/website/default/seo-snapshot` | snapshot utilisé pour le rendu statique/SEO |
| `GET` | `/website/{ownerId}` | snapshot public d’un owner |
| `GET` | `/website/default/projects/{projectSlug}` | projet public par slug |
| `GET` | `/website/{ownerId}/projects/{projectSlug}` | projet public owner + slug |
| `GET` | `/website/items-visibility` | visibilité publique des éléments |

## Ingestion et engineering

| Méthode | Route | Accès | Rôle |
|---|---|---|---|
| `POST` | `/analytics/events` | Public contrôlé | ingestion analytics |
| `GET` | `/api/engineering/mission-control` | Public selon SecurityFilterChain | snapshot engineering |
| `GET` | `/api/engineering/mission-control/queue` | Public selon SecurityFilterChain | vue queue/runtime |
| `GET` | `/api/engineering/performance/history` | Public selon SecurityFilterChain | historique de télémétrie |
| `POST` | `/api/engineering/performance/samples` | Public contrôlé | échantillons runtime navigateur |

## Owner administrateur

| Méthode | Route | Rôle |
|---|---|---|
| `POST` | `/manager` | créer un owner |
| `GET` | `/manager` | lister les owners |
| `GET` | `/manager/{ownerId}` | lire un owner |
| `PUT` | `/manager/{ownerId}` | modifier un owner avec préconditions |
| `DELETE` | `/manager/{ownerId}` | supprimer un owner |

## Snapshots `WebsiteVersion`

Base : `/manager/{ownerId}/versions`

| Méthode | Suffixe | Rôle |
|---|---|---|
| `GET` | `/` | lister les snapshots |
| `GET` | `/active` | snapshot actif |
| `GET` | `/{versionId}` | lire un snapshot |
| `GET` | `/{versionId}/health` | santé éditoriale |
| `GET` | `/{versionId}/publish-validation` | validation avant publication |
| `PUT` | `/{versionId}/activate-validated` | activation après validation |
| `POST` | `/{versionId}/backup/export` | export métier |
| `POST` | `/backup/restore` | restauration métier |
| `POST` | `/` | créer un snapshot |
| `POST` | `/from/{sourceVersionId}` | créer depuis une source |
| `PUT` | `/{versionId}` | modifier les métadonnées |
| `PUT` | `/{versionId}/activate` | activer selon contrat admin |
| `DELETE` | `/{versionId}` | supprimer |
| `PUT` | `/{versionId}/profile` | modifier le profil |
| `PUT` | `/{versionId}/timeline` | modifier la timeline |
| `POST` | `/{versionId}/projects` | créer un projet |
| `GET` | `/{versionId}/projects` | lister les projets |
| `GET` | `/{versionId}/projects/{projectId}` | lire un projet |
| `PUT` | `/{versionId}/projects/{projectId}` | modifier un projet |
| `DELETE` | `/{versionId}/projects/{projectId}` | supprimer un projet |
| `GET` | `/{fromVersionId}/diff/{toVersionId}` | comparer deux snapshots métier |
| `GET` | `/{versionId}/preview` | DTO de prévisualisation |

## Publication

Base : `/manager/{ownerId}/versions/{versionId}/publication`

| Méthode | Suffixe | Rôle |
|---|---|---|
| `PUT` | `/draft-metadata` | métadonnées de brouillon |
| `PUT` | `/ready` | déclarer prêt après règles métier |
| `PUT` | `/publish` | publication immédiate |
| `PUT` | `/schedule` | planification |
| `DELETE` | `/schedule` | annulation de planification |
| `POST` | `/rollback` | rollback éditorial vers un snapshot cloné |

## Fiabilité et audit

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/manager/{ownerId}/jobs` | jobs persistés |
| `PUT` | `/manager/{ownerId}/jobs/{jobId}/cancel` | annuler |
| `PUT` | `/manager/{ownerId}/jobs/{jobId}/retry` | rejouer |
| `GET` | `/manager/{ownerId}/events` | événements outbox |
| `PUT` | `/manager/{ownerId}/events/{eventId}/retry` | rejouer un événement |
| `GET` | `/manager/{ownerId}/publication-audit` | audit de publication |
| `GET` | `/manager/analytics/summary` | agrégats analytics |
| `GET` | `/manager/analytics/events` | événements analytics |

## Traduction, visibilité et fichiers

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/api/translations/provider/health` | santé provider |
| `POST` | `/api/translations/preview` | preview traduction |
| `GET` | `/api/translations/catalog` | catalogue |
| `POST` | `/api/translations/{contentType}/{contentKey}/auto` | traduction automatique |
| `GET` | `/api/translations/{contentType}/{contentKey}` | lire traductions |
| `PUT` | `/api/translations/{contentType}/{contentKey}` | modifier traductions |
| `GET` | `/api/items-visibility` | lire visibilité admin |
| `PUT` | `/api/items-visibility` | modifier visibilité |
| `GET` | `/uploads/` | contrat/listing selon provider |
| `GET` | `/uploads/files/{filename}` | servir un fichier local lorsque applicable |
| `POST` | `/uploads/` | upload protégé |

Toutes les opérations administratives sont soumises à la `SecurityFilterChain`; les mutations de session utilisent CSRF sauf exemptions publiques explicitement déclarées.
