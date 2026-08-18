---
title: Modèle de domaine
description: Owner, snapshot éditorial, profil, timeline, expériences, projets et tables de support.
sidebar:
  order: 2
---
<div class="architecture-frame">
  <img src="/diagrams/data-model.svg" alt="Relations principales du modèle de données." />
  <div class="architecture-caption">Relations principales du modèle de données.</div>
</div>


## Agrégat éditorial

`Owner` représente l’identité du portfolio et possède les contacts ainsi qu’une collection de `WebsiteVersion`. `WebsiteVersion` est le snapshot éditorial complet : profil, timeline et projets. Une seule représentation active publiée est utilisée par la lecture publique.

`Profile` contient les textes principaux, localisation, disponibilité et URLs de médias. `Timeline` regroupe des `Experience`. `Project` appartient directement au snapshot éditorial et contient statut, ordre, contenu d’étude de cas, stacks, features et liens.

## Révisions

`Owner` et `WebsiteVersion` portent un `@Version` JPA pour la concurrence optimiste. Le snapshot ajoute `contentRevision`, révision fonctionnelle utilisée par les préconditions de publication et les ETags exposés à l’administration.

## Tables de support

Analytics, traductions, visibilité front, jobs, outbox, audit de publication et échantillons runtime sont persistés dans des tables dédiées afin de ne pas surcharger l’agrégat éditorial.
