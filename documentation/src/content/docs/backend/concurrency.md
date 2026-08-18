---
title: Concurrence et exécution bornée
description: Optimistic locking, virtual threads bornés, queues et backpressure.
sidebar:
  order: 9
---
## Concurrence métier

`@Version` sur les agrégats principaux protège les mises à jour concurrentes. `contentRevision` fournit une précondition métier plus explicite pour les opérations éditoriales. Les contrôleurs exigent un tag ou une révision attendue lorsque le risque d’écrasement existe.

## Exécution asynchrone

`BoundedVirtualThreadExecutor` offre une voie I/O fondée sur virtual threads mais avec concurrence maximale bornée. Les tâches de maintenance et analytics utilisent également des capacités configurables. Le système préfère la backpressure et l’état de file observables à une création illimitée de tâches.

## Files

La queue analytics est bornée et flushée par batch. Les jobs et l’outbox sont persistés en PostgreSQL : ils survivent aux redémarrages et peuvent être inspectés depuis Mission Control.
