---
title: Stockage de fichiers
description: Abstraction StorageService, filesystem local, Cloudinary et politique d’upload.
sidebar:
  order: 13
---
<div class="architecture-frame">
  <img src="/diagrams/storage-flow.svg" alt="Chemin d’un upload admin jusqu’à l’URL publique." />
  <div class="architecture-caption">Chemin d’un upload admin jusqu’à l’URL publique.</div>
</div>


## Abstraction

`StorageService` définit le contrat commun. `FileSystemStorageService` sert au développement/local ; `CloudinaryStorageService` est activé par configuration pour le stockage distant.

## Validation

`UploadPolicy` centralise les limites de taille et règles de sécurité. `FilenameUtils` normalise les noms. Le contrôleur d’upload est protégé par la session admin.

## Cloudinary

Le SDK serveur reçoit cloud name, API key, secret et dossier. Les secrets ne sortent pas du backend. Les entités ne stockent que les URLs nécessaires à l’affichage.
