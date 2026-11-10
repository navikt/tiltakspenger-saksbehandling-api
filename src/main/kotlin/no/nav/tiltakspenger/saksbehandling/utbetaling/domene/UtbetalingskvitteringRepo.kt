package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.felles.Kafkametadata
import java.time.LocalDateTime

/** Innboksen for statusmeldinger om utbetalinger, lagret rått før de behandles. */
interface UtbetalingskvitteringRepo {
    /** @return false når meldingen med samme [Kafkametadata] allerede er lagret. */
    fun lagreNy(key: String?, value: String?, kafkametadata: Kafkametadata, mottatt: LocalDateTime): Boolean

    /**
     * Henter ID-ene for behandlelige kvitteringer i topic-, partisjons- og offset-rekkefølge.
     * En ubehandlet kvittering blokkerer høyere offset for samme nøkkel i samme topic og partisjon, også under backoff.
     * Kafka gir ingen rekkefølge på tvers av partisjoner.
     */
    fun hentUbehandledeIder(nå: LocalDateTime, limit: Int): List<UtbetalingskvitteringId>

    fun hent(id: UtbetalingskvitteringId, context: TransactionContext): RåUtbetalingskvittering

    fun markerBehandlet(
        id: UtbetalingskvitteringId,
        behandlet: LocalDateTime,
        kobling: Utbetalingskobling?,
        context: TransactionContext,
    )

    fun lagreFeil(
        id: UtbetalingskvitteringId,
        feil: String,
        forbigående: Boolean,
        forsøkshistorikk: Forsøkshistorikk,
        kobling: Utbetalingskobling?,
    )

    fun antallPermanenteFeil(): Long

    fun antallForbigåendeFeil(eldreEnn: LocalDateTime): Long
}
