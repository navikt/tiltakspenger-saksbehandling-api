package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.ports.TilbakekrevingHendelseRepo
import java.time.Clock

/**
 * Testhjelper rundt [TilbakekrevingConsumer.consume].
 *
 * De aller fleste testene bryr seg ikke om [erDev], som kun styrer loggnivået ved deserialiseringsfeil.
 * Defaulten hører hjemme her, i testkoden, og ikke i produksjonssignaturen - se «Ingen defaults i prod for testenes skyld» i AGENTS-backend.md.
 * Testene som faktisk bryr seg, sender inn verdien selv.
 */
internal fun konsumerTilbakekrevingshendelse(
    key: String,
    value: String?,
    tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
    clock: Clock,
    erDev: Boolean = false,
): TilbakekrevinghendelseId? = TilbakekrevingConsumer.consume(
    key = key,
    value = value,
    tilbakekrevingHendelseRepo = tilbakekrevingHendelseRepo,
    clock = clock,
    erDev = erDev,
)
