package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Either
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

    /** Utelater saker med utbetalingssperre, se [sperrUtbetalinger]. */
    fun hentForUtsjekk(limit: Int = 10): List<VedtattUtbetaling>

    fun oppdaterUtbetalingsstatus(
        utbetalingId: UtbetalingId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext? = null,
    )

    /** Gjelder bare saker på [Oppdragsversjon.V2], siden status for [Oppdragsversjon.V3] kommer på Kafka. */
    fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int = 10): List<UtbetalingDetSkalHentesStatusFor>

    fun hentOppdragsversjon(
        sakId: SakId,
        context: TransactionContext? = null,
    ): Oppdragsversjon

    fun hentOppdragstilstand(sakId: SakId): Oppdragstilstand

    /** Setter [Oppdragsversjon.V3], [Oppdragsmigrering.MIGRERT_HOS_HEL_VED] og [tidspunkt] på en sak som står [Oppdragsmigrering.SPERRET]. */
    fun oppdaterTilOppdragsversjon3(
        sakId: SakId,
        tidspunkt: LocalDateTime,
        context: TransactionContext? = null,
    ): Either<OppdragsmigreringenStårIEnAnnenTilstand, Unit>

    /**
     * Setter [Oppdragsmigrering.SPERRET] på en sak som ikke er migrert, slik at [hentForUtsjekk] ikke returnerer utbetalingene i den.
     * Låser sakraden til transaksjonen er ferdig.
     *
     * @return `true` når saken allerede var sperret.
     */
    fun sperrUtbetalinger(
        sakId: SakId,
        context: TransactionContext,
    ): Boolean

    /** Låser sakraden til transaksjonen er ferdig, slik at versjonen og migreringen ikke kan endres mens en utbetaling sendes. */
    fun låsOppdragstilstand(
        sakId: SakId,
        context: TransactionContext,
    ): Oppdragstilstand

    fun hentSakerMigrertHosHelVed(): List<SakId>

    /** Setter [Oppdragsmigrering.FERDIG] på en sak som står [Oppdragsmigrering.MIGRERT_HOS_HEL_VED]. */
    fun opphevUtbetalingssperre(sakId: SakId): Either<OppdragsmigreringenStårIEnAnnenTilstand, Unit>

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
