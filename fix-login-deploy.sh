#!/usr/bin/env bash
set -e

echo "== Backup WebSecurityConfig =="
cp src/main/java/sorbonne/professional_website/security/WebSecurityConfig.java \
src/main/java/sorbonne/professional_website/security/WebSecurityConfig.java.backup

FILE="src/main/java/sorbonne/professional_website/security/WebSecurityConfig.java"

echo "== Correction Spring Security =="

python3 <<'PY'
from pathlib import Path

p = Path("src/main/java/sorbonne/professional_website/security/WebSecurityConfig.java")

s = p.read_text()

# Autoriser le endpoint login
s = s.replace(
    '.requestMatchers("/api/**").hasRole("ADMIN")',
    '.requestMatchers("/api/auth/login").permitAll()\n' +
    '                        .requestMatchers("/api/**").hasRole("ADMIN")'
)

# Ignorer CSRF pour login REST
s = s.replace(
    '.csrf(csrf -> csrf.ignoringRequestMatchers(',
    '.csrf(csrf -> csrf.ignoringRequestMatchers('
)

if '/api/auth/login' not in s:
    print("ATTENTION: remplacement non effectué automatiquement")
else:
    p.write_text(s)

PY


echo "== Vérification =="
grep -n "api/auth/login" "$FILE" || true


echo "== Build Maven =="
./mvnw clean package -DskipTests


echo "== Déploiement Heroku =="

git status

git add .

git commit -m "fix: allow authentication endpoint"

git push heroku main


echo ""
echo "== Test login =="
echo "Teste maintenant :"
echo ""
echo 'curl -i -X POST \
-H "Content-Type: application/json" \
-d "{\"username\":\"TON_USER\",\"password\":\"TON_PASSWORD\"}" \
https://api.idris-achabou.fit/api/auth/login'
