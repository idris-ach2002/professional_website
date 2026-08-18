---
title: Traduction et localisation
description: Store de traductions, hash source, LibreTranslate et assemblage public.
sidebar:
  order: 12
---
<div class="architecture-frame">
  <img src="/diagrams/localization-flow.svg" alt="Provider privé, persistence et application des traductions." />
  <div class="architecture-caption">Provider privé, persistence et application des traductions.</div>
</div>


## Modèle

`ContentTranslation` identifie un contenu par type, clé, locale et nom de champ. Chaque entrée stocke texte traduit, hash de la source et statut. La contrainte unique empêche les doublons d’un même champ logique.

## Provider

`LibreTranslateClient` n’est utilisé que côté serveur. Les fonctions de preview et auto-translate peuvent appeler le provider, tandis que la sauvegarde persiste le résultat dans PostgreSQL.

## Lecture publique

`PortfolioLocalizationService` applique les traductions au moment de construire le DTO public. Les traductions dont le hash source ne correspond plus peuvent être considérées obsolètes selon le service de store, évitant de servir un texte traduit contre une source modifiée.
