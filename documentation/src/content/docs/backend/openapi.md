---
title: OpenAPI
description: Exposition contrôlée du contrat API pour l’administration et le développement.
---
## Génération

Springdoc construit le document OpenAPI à partir des contrôleurs et schémas DTO. L’objectif est de rendre le contrat HTTP inspectable sans dupliquer manuellement les signatures dans un fichier statique.

## Accès

`/v3/api-docs/**` est protégé par le rôle administrateur. La documentation API contient des informations utiles au développement mais n’est pas nécessaire à la lecture publique du portfolio.

## Usage

OpenAPI complète la documentation narrative :

- le site Starlight explique les flux, responsabilités, préconditions et raisons d’architecture ;
- OpenAPI décrit les opérations HTTP et schémas exposés par Spring.

Les deux doivent rester cohérents avec les contrôleurs réellement compilés.
