# Chainguard-image fra Navs speil: https://sikkerhet.nav.no/docs/verktoy/chainguard-dockerimages/
# Bevisst uten digest: Chainguard backporter ikke fikser til eldre patch-versjoner, og Dependabot når ikke speilet.
# Major-taggen gir nyeste bygg av Java 25 ved hvert image-bygg.
FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25

ENV TZ='Europe/Oslo'
ENV LC_ALL='nb_NO.UTF-8'
ENV LANG='nb_NO.UTF-8'

WORKDIR /app

# --chmod=0755: jars må være lesbare for USER nobody.
# Uten dette arver de filrettigheter fra (stale) GHA Gradle-cache, som kan
# være 0600 (kun eier) → nobody får ikke lest → crashloop. Ikke fjern.
COPY --chmod=0755 build/install/tiltakspenger-saksbehandling-api/lib/*.jar /app/lib/

USER nobody

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.tiltakspenger.saksbehandling.AppKt"]
