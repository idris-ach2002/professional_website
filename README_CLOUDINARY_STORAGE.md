# Stockage des fichiers : local ou Cloudinary

Le backend garde les mêmes endpoints utilisés par le front :

- `POST /uploads/` pour uploader un fichier depuis le panel admin ;
- `GET /uploads/files/{filename}` pour lire le fichier.

Le provider est choisi par variable d'environnement.

## Développement local / Docker

```env
STORAGE_PROVIDER=local
UPLOAD_DIR=/app/uploads
```

Avec Docker Compose, les fichiers restent dans le volume Docker `uploads-data`.

## Render Free + Cloudinary Free

Render Free n'a pas de disque persistant. Pour éviter de perdre les fichiers après un redémarrage ou un redeploy, il faut utiliser Cloudinary.

Variables à mettre côté Render :

```env
STORAGE_PROVIDER=cloudinary
CLOUDINARY_CLOUD_NAME=xxxxx
CLOUDINARY_API_KEY=xxxxx
CLOUDINARY_API_SECRET=xxxxx
CLOUDINARY_FOLDER=portfolio
```

Ne jamais mettre `CLOUDINARY_API_SECRET` dans le front.

## Comportement

- Le front continue à envoyer les fichiers au backend.
- Le backend envoie ensuite le fichier vers Cloudinary.
- Le backend renvoie une URL `/uploads/files/{filename}` utilisable par le portfolio.
- En local, cette URL lit dans le dossier local.
- En prod, cette URL pointe vers le fichier Cloudinary via le backend.

## PDF sur Cloudinary

Sur certains comptes Cloudinary gratuits, la livraison directe des PDF peut demander une activation de sécurité dans les paramètres du compte. Les images ne sont pas concernées.
