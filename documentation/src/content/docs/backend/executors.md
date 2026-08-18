---
title: Exécuteurs et concurrence
description: Bornage des travaux CPU, I/O, maintenance et traitement asynchrone.
---
## Objectif

Le backend ne confond pas concurrence disponible et concurrence illimitée. Les lanes de travail sont bornées afin de protéger PostgreSQL, les providers externes et la mémoire du processus.

## Virtual threads

La lane I/O peut exploiter les virtual threads Java tout en conservant une limite de concurrence applicative via `APP_VIRTUAL_IO_MAX_CONCURRENCY`. La valeur automatique dépend des processeurs disponibles, mais le plafond empêche les appels I/O d’épuiser les ressources externes.

## Maintenance

Les traitements de maintenance disposent de tailles et d’une queue distinctes. Les jobs de publication, l’outbox et les tâches d’entretien ne doivent pas monopoliser le chemin de requête HTTP.

## Analytics

L’ingestion analytics utilise une queue et des batches configurables. La capacité, la taille des lots et l’intervalle de flush sont explicitement bornés ; un rate limit protège également l’endpoint public.

## Observabilité

Mission Control et les metrics d’exécuteurs doivent permettre de distinguer saturation de queue, pression DB et panne externe. L’augmentation d’un pool n’est pas un correctif par défaut : elle doit respecter la capacité du maillon aval.
