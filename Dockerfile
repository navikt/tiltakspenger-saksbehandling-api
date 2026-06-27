FROM gcr.io/distroless/java25-debian13

ENV TZ='Europe/Oslo'
ENV LC_ALL='nb_NO.UTF-8'
ENV LANG='nb_NO.UTF-8'

WORKDIR /app

COPY --chmod=0755 build/install/tiltakspenger-saksbehandling-api/lib/*.jar /app/lib/

USER nobody

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.tiltakspenger.saksbehandling.AppKt"]
