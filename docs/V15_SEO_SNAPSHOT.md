# V15 — Snapshot SEO public

Le backend expose désormais :

```text
GET /website/default/seo-snapshot
```

La réponse contient :

```json
{
  "generatedAt": "2026-08-07T00:00:00Z",
  "fr": { "locale": "fr" },
  "en": { "locale": "en" }
}
```

Les deux variantes sont construites à partir du même owner chargé une seule fois. Le frontend utilise cette réponse pendant le build afin de produire les pages HTML statiques FR/EN depuis les données réellement publiées en base.
