---
title: Analytics
description: Ingestion publique, rate limiting, batch writer et agrégations admin.
sidebar:
  order: 11
---
<div class="architecture-frame">
  <img src="/diagrams/analytics-mission-control.svg" alt="Pipeline analytics et consommation par Mission Control." />
  <div class="architecture-caption">Pipeline analytics et consommation par Mission Control.</div>
</div>


## Ingestion

Le frontend poste des événements normalisés vers `/analytics/events`. L’identité visiteur/session est hachée avant persistence avec un secret serveur. `AnalyticsRateLimiter` limite le volume accepté par source afin de protéger le backend.

## Queue et batch

`AnalyticsIngestionPipeline` place les événements dans une queue bornée. `AnalyticsBatchWriter` les écrit par batch, ce qui découple le chemin HTTP de l’écriture individuelle de chaque événement.

## Rétention

Un job de rétention supprime les données plus anciennes que la politique configurée. Les endpoints manager fournissent résumé et consultation détaillée.
