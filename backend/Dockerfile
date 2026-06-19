# Multi-Stage-Build fuer die Render-Gratis-Demo.
#
# Stage 1 baut das Quarkus-Fast-Jar mit dem committeten Maven-Wrapper — so
# funktioniert "Deploy aus dem Repo" ohne lokales Maven/JDK. Tests laufen hier
# bewusst nicht (die uebernimmt die CI), damit der Deploy-Build schlank bleibt.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Render nutzt fuer Docker-Builds das Repo-Root als Build-Context (ignoriert
# dockerContext), daher explizit aus backend/ kopieren statt "COPY . .".
COPY backend/ ./
# Das Base-Image bringt Maven mit -> kein Wrapper noetig (vermeidet ./mvnw-Probleme
# mit Datei-Modi / CRLF im Docker-Build).
RUN mvn -B package -DskipTests

# Stage 2: schlankes JRE-Runtime-Image mit nur dem fertigen quarkus-app.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/quarkus-app/ ./

ENV QUARKUS_PROFILE=prod
# Render gibt den Port dynamisch via $PORT vor; lokal Fallback auf 8080.
CMD ["sh", "-c", "java -Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=${PORT:-8080} -jar quarkus-run.jar"]
