---
title: API d’administration
description: Endpoints protégés pour contenu, publication, fichiers, traduction, jobs et audit.
sidebar:
  order: 4
---
## Owner et snapshots

<div class="architecture-frame">
  <img src="/diagrams/admin-concurrency.svg" alt="Chaîne d’une mutation administrateur et concurrence optimiste." />
  <div class="architecture-caption">Session, CSRF, ETag, transaction et gestion des conflits.</div>
</div>

`/manager` gère les propriétaires. `/manager/{ownerId}/versions` gère les snapshots éditoriaux : lecture, création, clonage, modification, activation, suppression, profil, timeline et projets.

## Publication

`/manager/{ownerId}/versions/{versionId}/publication` expose autosave metadata, ready, publish, schedule, cancel schedule et rollback. Les préconditions de révision sont obligatoires pour les opérations sensibles.

## Fiabilité

- `/manager/{ownerId}/jobs` : lister, annuler, retry.
- `/manager/{ownerId}/events` : lister et relancer les événements outbox morts.
- `/manager/{ownerId}/publication-audit` : consulter l’audit.
- endpoints de diff et preview pour comparer/rendre les snapshots sans les publier.

## Autres capacités

- `/uploads/**` : upload et accès contrôlé aux fichiers.
- `/api/translations/**` : santé du provider, catalogue, preview, auto et sauvegarde.
- `/api/items-visibility` : visibilité des éléments du front.
- `/manager/analytics/**` : résumé et événements analytics.
- `/csrf` : jeton CSRF pour les appels protégés.

L’accès à l’OpenAPI et à Prometheus est lui-même protégé par le rôle administrateur.
