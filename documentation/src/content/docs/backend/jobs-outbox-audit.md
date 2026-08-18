---
title: Jobs, Outbox et Audit
description: Fiabilité asynchrone, retries et traçabilité des publications.
sidebar:
  order: 10
---
## Background jobs

`BackgroundJob` persiste type, statut, progression, priorité, tentatives, échéance, heartbeat et corrélation. Les types couvrent publication, traduction, backup, import, indexation, agrégation analytics et maintenance.

## Outbox

`OutboxEvent` persiste l’événement dans la même base que la transaction métier. Le dispatcher récupère les événements dus par lots, les passe en traitement, appelle les handlers, puis marque succès ou échec. Les tentatives épuisées deviennent `DEAD` et peuvent être relancées depuis l’administration.

## Récupération

Le dispatcher récupère les événements restés en traitement au-delà d’un cutoff afin qu’un arrêt du processus ne bloque pas définitivement la file.

## Audit

`PublicationAuditEntry` enregistre les actions de publication avec actor, correlation ID et snapshots JSON avant/après. Cette trace est indépendante des logs techniques.
