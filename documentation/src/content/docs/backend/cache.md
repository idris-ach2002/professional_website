---
title: Cache public
description: Caffeine, clés localisées et invalidation à la publication.
sidebar:
  order: 7
---
## Caches

Le backend déclare des caches pour le portfolio public, la liste publique, les projets publics et le snapshot SEO. Le nombre d’entrées et l’expiration sont bornés.

## Clés

Les clés incluent `ownerId`, slug et locale selon le cas. Le snapshot par défaut utilise une clé distincte. `sync = true` sur les lectures `@Cacheable` réduit le stampede lorsqu’une clé froide est demandée simultanément.

## Invalidation

Les mutations publient un événement de changement. L’invalidation ne dépend donc pas uniquement du TTL. La publication via outbox finit également par notifier le publisher de changement, ce qui restaure la cohérence après traitement asynchrone.
