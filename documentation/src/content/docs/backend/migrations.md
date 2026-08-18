---
title: Flyway et schéma
description: Gestion explicite du schéma PostgreSQL et règles de migration.
---
## Autorité de schéma

Flyway applique les migrations SQL au démarrage avant que l’application n’expose son contrat métier. Hibernate valide ensuite que les entités correspondent au schéma attendu. Cette séquence évite que la persistence modifie implicitement la base en production.

## Discipline

Une migration doit être additive ou explicitement planifiée pour les changements destructifs. Les contraintes d’unicité, index et foreign keys font partie du contrat de cohérence, au même titre que les annotations JPA.

## Tests

Les tests de migration démarrent PostgreSQL avec Testcontainers et vérifient que Flyway peut construire une base compatible avec l’application. Les tests d’intégration utilisent ensuite le même moteur relationnel afin que transactions, verrous et requêtes ne soient pas validés uniquement sur une base en mémoire.

## Déploiement

Avant une release backend, `clean verify` doit être vert. Au démarrage Render, une erreur Flyway doit empêcher l’application de se déclarer saine : une API sur un schéma partiellement migré n’est pas un état acceptable.
