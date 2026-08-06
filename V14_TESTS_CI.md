# V14 — Tests et fiabilité backend

## Ajouts

- profil Spring `test` indépendant d'Aiven ;
- PostgreSQL 16 jetable avec Testcontainers ;
- test réel des migrations Flyway sur une base vide ;
- tests de traduction, fallback français et détection des traductions obsolètes ;
- tests du service et de l'API publique ;
- tests de sécurité de l'API Admin ;
- contrat d'erreur JSON commun ;
- gestion de `Exception.class` sans exposition des détails internes ;
- corrélation `X-Request-ID` et ajout dans les logs MDC ;
- couverture JaCoCo ;
- pipeline GitHub Actions : tests, package, artefacts, puis déploiement Render conditionnel.

## Validation locale

```bash
sudo systemctl start docker
./mvnw clean verify
```

La validation finale exige `BUILD SUCCESS`.
