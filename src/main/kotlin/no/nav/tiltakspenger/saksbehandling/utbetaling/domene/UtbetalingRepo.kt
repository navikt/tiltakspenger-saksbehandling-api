package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import java.time.LocalDateTime

enum class KvitteringsstatusLagring {
    Lagret,
    IgnorertFordiUtbetalingenErTerminal,
    FantIkkeUtbetaling,
}

interface UtbetalingRepo {
    fun hent(utbetalingId: UtbetalingId, context: TransactionContext): VedtattUtbetaling?

    fun finnUtbetalingerForKvitteringsreferanser(
        referanser: Set<Utbetalingsreferanse>,
        context: TransactionContext,
    ): List<UtbetalingsreferanseTreff>

    fun antallFeiledeUtbetalingerPåVersjon3(): Long

    /** Omfatter også utbetalinger som ikke er sendt, og utbetalinger som aldri har fått en kvittering. */
    fun antallUtbetalingerUtenOkPåVersjon3(eldreEnn: LocalDateTime): Long

    /**
     * Krever en transaksjon fordi markeringen skal rulles tilbake dersom iverksettingen mot helved feiler.
     */
    fun markerSendtTilUtbetaling(
        utbetalingId: UtbetalingId,
        tidspunkt: LocalDateTime,
        context: TransactionContext,
    )

    /**
     * Lagrer requesten vi sendte til helved og svaret vi fikk, enten kallet lyktes eller feilet.
     */
    fun lagreUtbetalingsrespons(
        utbetalingId: UtbetalingId,
        utbetalingsrespons: UtbetalingResponse,
        context: TransactionContext? = null,
    )

    fun hentUtbetalingJson(utbetalingId: UtbetalingId): String?

    /**
     * Lagrer request-strengen hvis den mangler, og returnerer den faktiske lagrede strengen.
     * Returnerer `null` dersom utbetalingen ikke lenger finnes.
     */
    fun lagreUtbetalingJsonHvisMangler(
        utbetalingId: UtbetalingId,
        request: String,
        context: TransactionContext,
    ): String?

    fun hentForUtsjekk(limit: Int = 10): List<VedtattUtbetaling>

    fun oppdaterUtbetalingsstatus(
        utbetalingId: UtbetalingId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext? = null,
    )

    /** Gjelder bare saker på [Oppdragsversjon.V2], siden status for [Oppdragsversjon.V3] kommer på Kafka. */
    fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int = 10): List<UtbetalingDetSkalHentesStatusFor>

    fun hentOppdragsversjon(sakId: SakId): Oppdragsversjon

    /**
     * Lagrer status fra en kvittering på Kafka.
     * Oppdaterer bare utbetalinger i saker på [Oppdragsversjon.V3].
     * En status som kommer etter en terminal status, blir ignorert.
     */
    fun lagreKvitteringsstatus(
        utbetalingId: UtbetalingId,
        status: Utbetalingsstatus,
        context: TransactionContext? = null,
    ): KvitteringsstatusLagring
}
