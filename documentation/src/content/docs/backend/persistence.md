---
title: Persistence PostgreSQL
description: JPA, repositories, Flyway, fetch plans et contraintes relationnelles.
sidebar:
  order: 6
---
## Hibernate/JPA

`open-in-view` est désactivé : les services doivent charger ce dont les contrôleurs ont besoin dans la transaction. Les repositories utilisent des requêtes dédiées pour les graphes publics et les écritures sous verrou lorsque la cohérence l’exige.

## Flyway

Flyway est activé et constitue la source d’évolution du schéma. Hibernate valide le schéma en production au lieu de le créer automatiquement. Les migrations couvrent le cœur éditorial, analytics, traductions, concurrence, publication fiable, observabilité et visibilité frontend.

## Performance

Le backend configure batch fetch et JDBC batching. Des index ciblent les recherches de snapshot public, projets ordonnés, événements analytics, traductions, jobs/outbox dus et échantillons runtime.

## Collections

Contacts, compétences, stacks, features et liens utilisent des tables relationnelles/collections adaptées. Les listes ne sont pas encodées en texte opaque lorsqu’elles doivent être requêtables ou conservées avec ordre.
