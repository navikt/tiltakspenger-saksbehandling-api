# Distroless har ingen semver-tagger — versjonen ligger i repo-navnet (java25-debian13) — så taggen er `latest`.
# Digesten er det som faktisk kjører; taggen er det Dependabot følger, og gir PR når `latest` flyttes.
FROM gcr.io/distroless/java25-debian13:latest@sha256:817363ac3b3efab113afa288fe4d4d0fba6daaf02e59ae8813e7573dbc556212

ENV TZ='Europe/Oslo'
ENV LC_ALL='nb_NO.UTF-8'
ENV LANG='nb_NO.UTF-8'

WORKDIR /app

# --chmod=0755: jars må være lesbare for USER nobody i distroless.
# Uten dette arver de filrettigheter fra (stale) GHA Gradle-cache, som kan
# være 0600 (kun eier) → nobody får ikke lest → crashloop. Ikke fjern.
COPY --chmod=0755 build/install/tiltakspenger-saksbehandling-api/lib/*.jar /app/lib/

USER nobody

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.tiltakspenger.saksbehandling.AppKt"]
