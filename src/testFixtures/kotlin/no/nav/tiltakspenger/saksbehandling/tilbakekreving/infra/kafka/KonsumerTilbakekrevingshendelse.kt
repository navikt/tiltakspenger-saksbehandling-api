package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingHendelseRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import java.time.Clock

/**
 * Testhjelper rundt [TilbakekrevingConsumer.consume].
 *
 * Testene bryr seg ikke om loggingen ved deserialiseringsfeil.
 * Verdien settes derfor her i testkoden, og ikke som default i produksjonssignaturen, se «Ingen defaults i prod for testenes skyld» i AGENTS-backend.md.
 */
fun konsumerTilbakekrevingshendelse(
    key: String,
    value: String?,
    tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
    clock: Clock,
): TilbakekrevinghendelseId? = TilbakekrevingConsumer.consume(
    key = key,
    value = value,
    tilbakekrevingHendelseRepo = tilbakekrevingHendelseRepo,
    clock = clock,
    loggDeserialiseringsfeilSomError = true,
)
