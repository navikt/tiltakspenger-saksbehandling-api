FROM gcr.io/distroless/java21-debian12

ENV TZ='Europe/Oslo'
ENV LC_ALL='nb_NO.UTF-8'
ENV LANG='nb_NO.UTF-8'

WORKDIR /app

COPY build/install/tiltakspenger-saksbehandling-api/lib/*.jar .

USER nobody

CMD ["app.jar"]
