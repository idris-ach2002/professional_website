# LibreTranslate infrastructure

The local `docker-compose.yml` starts LibreTranslate on the private `portfolio-network` without publishing port `5000` on the host. Only the Spring Boot container can call it through `http://libretranslate:5000`.

For Render, create LibreTranslate as a **Private Service** from the official image `libretranslate/libretranslate:v1.9.6`, in the same region as the backend. Configure:

- `LT_LOAD_ONLY=en,fr`
- `LT_UPDATE_MODELS=true`
- `LT_DISABLE_WEB_UI=true`
- `LT_DISABLE_FILES_TRANSLATION=true`
- `ARGOS_CHUNK_TYPE=MINISBD`

Attach a persistent disk to `/home/libretranslate/.local` so language models are not downloaded on every deploy. Set the backend variable `LIBRETRANSLATE_BASE_URL` to the private URL supplied by Render.

Do not expose the LibreTranslate service publicly. Public portfolio requests never call it; only authenticated `/api/translations/**` administration endpoints do.


## Limitation du plan gratuit Render

La configuration recommandée en production utilise un Private Service et un disque persistant. Sur Render, le disque persistant nécessite une instance payante et les web services Free ne peuvent pas recevoir de trafic privé. Le développement local avec Docker Compose reste gratuit ; le coût éventuel concerne uniquement l’hébergement permanent.
