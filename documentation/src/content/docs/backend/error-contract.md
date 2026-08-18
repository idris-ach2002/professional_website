---
title: Erreurs et Request ID
description: Gestion centralisée des erreurs HTTP et corrélation opérationnelle.
---
## Erreurs structurées

La couche `exception` traduit les exceptions connues en réponses HTTP cohérentes plutôt que de laisser chaque contrôleur inventer son format. Les erreurs de validation, absence de ressource, authentification et concurrence disposent ainsi d’un statut et d’un message interprétables par le frontend.

## Request ID

Un identifiant de requête traverse la chaîne HTTP et peut être renvoyé dans les réponses d’erreur. Le frontend admin le conserve pour permettre une recherche directe dans les logs serveur.

## Concurrence

Une précondition de révision obsolète n’est pas une erreur serveur générique : elle signale que le client doit recharger l’état. Les tags d’entité et `contentRevision` permettent de détecter cette condition avant d’écraser silencieusement une mutation concurrente.

## Sécurité

Les réponses d’erreur ne doivent pas exposer stack traces, secrets, SQL ou détails de provider. Le log serveur garde le contexte nécessaire tandis que le contrat HTTP reste minimal et exploitable.
