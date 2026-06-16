FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# La génération CV compile du LaTeX côté backend.
# Render exécute cette image Docker en production : les binaires LaTeX doivent donc
# être présents dans l'image, pas seulement installés sur la machine locale.
# Packages nécessaires au template actuel :
# - latexmk / pdflatex : compilation PDF
# - tcolorbox, tikz, tabularx, enumitem, hyperref, microtype...
# - fontawesome5 : icônes du CV
# - babel french : typographie française
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        latexmk \
        texlive-latex-base \
        texlive-latex-recommended \
        texlive-latex-extra \
        texlive-fonts-recommended \
        texlive-fonts-extra \
        texlive-pictures \
        texlive-lang-french \
    && latexmk -version \
    && pdflatex --version | head -n 1 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENV CV_LATEX_COMPILER="latexmk"
ENV CV_LATEX_TIMEOUT_SECONDS="45"
ENV CV_STORE_LATEX_SOURCE="true"
ENV TEXMFVAR="/tmp/texmf-var"
ENV TEXMFCONFIG="/tmp/texmf-config"
ENV TEXMFHOME="/tmp/texmf-home"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
