---
title: Publication
description: Machine d’état éditorial, validation, planification, rollback et audit.
sidebar:
  order: 5
---
<div class="architecture-frame">
  <img src="/diagrams/publication-pipeline.svg" alt="Pipeline de publication avec jobs et outbox." />
  <div class="architecture-caption">Pipeline de publication avec jobs et outbox.</div>
</div>


## États

Le snapshot éditorial suit les états `DRAFT`, `READY`, `SCHEDULED`, `PUBLISHING`, `PUBLISHED`, `SUPERSEDED` et `FAILED`. Ces états sont métier : ils décrivent le cycle de publication actuel et ne correspondent pas à une numérotation de l’application.

## Publication immédiate

`PublicationService.publishNow` vérifie la révision attendue, accepte une clé d’idempotence, valide la santé du snapshot et publie sous verrou propriétaire. Les autres snapshots publiés sont marqués superseded afin qu’un seul contenu soit actif.

## Publication planifiée

Une date future crée un `BackgroundJob` de type publication. Le job runner réappelle le service au moment prévu. Les échecs sont enregistrés, le statut passe à `FAILED` et un événement de panne est placé dans l’outbox.

## Rollback éditorial

Le rollback clone un snapshot source vers un nouveau snapshot, le valide puis le publie. Cela préserve l’audit et évite une mutation silencieuse de l’historique éditorial existant.

## Audit et idempotence

Chaque opération majeure enregistre un audit avec action, acteur, corrélation et états avant/après. L’outbox empêche de perdre les événements si la transaction métier réussit mais qu’un traitement secondaire doit être repris plus tard.
