---
title: Tests Backend
description: Tests unitaires, HTTP, PostgreSQL, concurrence et couverture.
sidebar:
  order: 4
---
## Unitaires et composants

Les services métier, mappers, validations, upload policy, traduction, publication, jobs et sécurité possèdent des tests ciblés. Les contrôleurs utilisent les outils de test Spring pour valider status, headers et payloads.

## Intégration PostgreSQL

Les tests qui dépendent de contraintes SQL, migrations ou concurrence utilisent PostgreSQL via Testcontainers. Le but est de vérifier le comportement réel de la base plutôt qu’une approximation H2.

## Concurrence

Les scénarios concurrents orchestrent les threads avec des barrières et vérifient l’invariant final en base : un seul snapshot actif, révisions cohérentes, pas de perte de mise à jour silencieuse.

## Couverture

JaCoCo est exécuté pendant `verify` avec un seuil global. Les rapports Surefire et JaCoCo sont publiés par GitHub Actions.
