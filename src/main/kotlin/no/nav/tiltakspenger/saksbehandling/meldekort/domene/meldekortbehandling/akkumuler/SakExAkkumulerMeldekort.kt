package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.akkumuler

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingAvbrutt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldeperiodebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldeperiodebehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldeperiodebehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilUtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.LocalDateTime

/**
 * Resultatet av [Sak.akkumulerMeldekort].
 * Skilletsene styrer hvordan behandlingen skal persisteres.
 */
sealed interface AkkumulertMeldekort {
    val sak: Sak
    val behandling: Meldekortbehandling

    /** En ny meldekortbehandling med [Meldekortbehandling.skalAkkumulereMeldekort] satt ble opprettet. */
    data class NyBehandling(
        override val sak: Sak,
        override val behandling: MeldekortUnderBehandling,
    ) : AkkumulertMeldekort

    /**
     * Meldeperioden ble lagt til en eksisterende behandling, eller dagene i en eksisterende meldeperiodebehandling ble erstattet.
     * Beregning, simulering og utbetalingskontroll er nullstilt.
     */
    data class NullstiltBehandling(
        override val sak: Sak,
        override val behandling: Meldekortbehandling,
    ) : AkkumulertMeldekort

    /**
     * Brukers meldekort ble knyttet til en eksisterende meldeperiodebehandling uten at dagene ble endret.
     * Beregning og simulering er beholdt.
     */
    data class KunKnyttetTilBehandling(
        override val sak: Sak,
        override val behandling: Meldekortbehandling,
    ) : AkkumulertMeldekort
}

/**
 * Akkumulerer meldeperioden til et brukers meldekort som ikke kan behandles automatisk inn i en manuell meldekortbehandling, slik at saksbehandler kan behandle den.
 *
 * Dersom kjeden allerede er omfattet av en åpen behandling knyttes brukers meldekort til den eksisterende meldeperiodebehandlingen.
 * Ellers legges meldeperioden til sakens åpne behandling med [Meldekortbehandling.skalAkkumulereMeldekort] satt, eller en ny slik behandling opprettes.
 */
suspend fun Sak.akkumulerMeldekort(
    brukersMeldekort: BrukersMeldekort,
    hentNavkontor: suspend (fnr: Fnr) -> Navkontor,
    clock: Clock,
): AkkumulertMeldekort {
    val kjedeId = brukersMeldekort.kjedeId
    val tidspunkt = nå(clock)

    meldekortbehandlinger.hentÅpenBehandlingForKjede(kjedeId)?.let { åpenBehandling ->
        val (oppdatertBehandling, erNullstilt) = åpenBehandling.knyttTilBrukersMeldekort(brukersMeldekort, tidspunkt)
        val oppdatertSak = oppdaterMeldekortbehandling(oppdatertBehandling)
        return if (erNullstilt) {
            AkkumulertMeldekort.NullstiltBehandling(oppdatertSak, oppdatertBehandling)
        } else {
            AkkumulertMeldekort.KunKnyttetTilBehandling(oppdatertSak, oppdatertBehandling)
        }
    }

    val type = if (meldekortbehandlinger.hentIkkeAvbrutteBehandlingerForKjede(kjedeId).isEmpty()) {
        MeldeperiodebehandlingType.FØRSTE_BEHANDLING
    } else {
        MeldeperiodebehandlingType.KORRIGERING
    }

    meldekortbehandlinger.åpneMeldekortbehandlinger
        .filterIsInstance<MeldekortUnderBehandling>()
        .firstOrNull { it.skalAkkumulereMeldekort }
        ?.let { akkumulerendeBehandling ->
            val oppdatertBehandling = akkumulerendeBehandling.leggTilMeldeperiode(
                brukersMeldekort.tilMeldeperiodebehandlingForAkkumulering(type, akkumulerendeBehandling.id),
                tidspunkt,
            )
            return AkkumulertMeldekort.NullstiltBehandling(
                oppdaterMeldekortbehandling(oppdatertBehandling),
                oppdatertBehandling,
            )
        }

    val meldekortId = MeldekortId.random()
    val nyBehandling = MeldekortUnderBehandling(
        id = meldekortId,
        sakId = id,
        saksnummer = saksnummer,
        fnr = fnr,
        opprettet = tidspunkt,
        navkontor = hentNavkontor(fnr),
        saksbehandler = null,
        begrunnelse = null,
        attesteringer = Attesteringer.empty(),
        sendtTilBeslutning = null,
        simulering = null,
        utbetalingskontroll = null,
        status = MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
        sistEndret = tidspunkt,
        fritekstTilVedtaksbrev = null,
        skalSendeVedtaksbrev = true,
        meldeperioder = Meldeperiodebehandlinger(
            meldeperioder = nonEmptyListOf(brukersMeldekort.tilMeldeperiodebehandlingForAkkumulering(type, meldekortId)),
            beregning = null,
        ),
        ventestatus = Ventestatus(),
        klagebehandling = null,
        skalAkkumulereMeldekort = true,
    )
    return AkkumulertMeldekort.NyBehandling(leggTilMeldekortbehandling(nyBehandling), nyBehandling)
}

/**
 * Knytter et brukers meldekort til meldeperiodebehandlingen for kjeden i en åpen behandling.
 * Dagene erstattes med brukers siste innsending kun dersom ingen saksbehandler har tatt behandlingen ennå - ellers ville vi overskrevet en påbegynt utfylling.
 *
 * @return Behandlingen og hvorvidt beregning, simulering og utbetalingskontroll ble nullstilt.
 */
private fun Meldekortbehandling.knyttTilBrukersMeldekort(
    brukersMeldekort: BrukersMeldekort,
    tidspunkt: LocalDateTime,
): Pair<Meldekortbehandling, Boolean> {
    val gjeldendeMeldeperiodebehandling = meldeperioder.first { it.kjedeId == brukersMeldekort.kjedeId }

    // Idempotens - brukers meldekort er allerede knyttet til behandlingen.
    if (gjeldendeMeldeperiodebehandling.brukersMeldekort.any { it.id == brukersMeldekort.id }) {
        return this to false
    }

    val erstattDager = saksbehandler == null

    val oppdatertMeldeperiodebehandling = gjeldendeMeldeperiodebehandling.copy(
        dager = if (erstattDager) brukersMeldekort.tilUtfyltMeldeperiodeForAkkumulering() else gjeldendeMeldeperiodebehandling.dager,
        brukersMeldekort = gjeldendeMeldeperiodebehandling.brukersMeldekort + brukersMeldekort,
    )
    val oppdaterteMeldeperioder = meldeperioder.map {
        if (it.kjedeId == brukersMeldekort.kjedeId) oppdatertMeldeperiodebehandling else it
    }.toNonEmptyListOrThrow()

    return when (this) {
        is MeldekortUnderBehandling -> {
            if (erstattDager) {
                copy(
                    meldeperioder = Meldeperiodebehandlinger(oppdaterteMeldeperioder, null),
                    simulering = null,
                    utbetalingskontroll = null,
                    sistEndret = tidspunkt,
                ) to true
            } else {
                copy(
                    meldeperioder = Meldeperiodebehandlinger(oppdaterteMeldeperioder, meldeperioder.beregning),
                    sistEndret = tidspunkt,
                ) to false
            }
        }

        // Manuelt behandlede meldekort har alltid en saksbehandler, så dagene erstattes aldri og beregningen er fortsatt gyldig.
        is MeldekortbehandlingManuell -> {
            copy(
                meldeperioder = Meldeperiodebehandlinger(oppdaterteMeldeperioder, meldeperioder.beregning),
                sistEndret = tidspunkt,
            ) to false
        }

        is MeldekortBehandletAutomatisk -> throw IllegalStateException("Automatisk meldekortbehandling skal alltid ansees som avsluttet")

        is MeldekortbehandlingAvbrutt -> throw IllegalStateException("Avbrutt meldekortbehandling skal alltid ansees som avsluttet")
    }
}

private fun MeldekortUnderBehandling.leggTilMeldeperiode(
    meldeperiodebehandling: Meldeperiodebehandling,
    tidspunkt: LocalDateTime,
): MeldekortUnderBehandling {
    require(meldeperioder.none { it.kjedeId == meldeperiodebehandling.kjedeId }) {
        "Meldekortbehandling $id omfatter allerede meldeperiodekjede ${meldeperiodebehandling.kjedeId}"
    }

    return copy(
        meldeperioder = Meldeperiodebehandlinger(
            meldeperioder = (meldeperioder + meldeperiodebehandling)
                .sortedBy { it.periode.fraOgMed }
                .toNonEmptyListOrThrow(),
            beregning = null,
        ),
        simulering = null,
        utbetalingskontroll = null,
        sistEndret = tidspunkt,
    )
}

private fun BrukersMeldekort.tilMeldeperiodebehandlingForAkkumulering(
    type: MeldeperiodebehandlingType,
    meldekortbehandlingId: MeldekortId,
): Meldeperiodebehandling {
    return Meldeperiodebehandling(
        dager = tilUtfyltMeldeperiodeForAkkumulering(),
        brukersMeldekort = listOf(this),
        type = type,
        meldekortbehandlingId = meldekortbehandlingId,
    )
}

/**
 * Brukers innsending som utfylling.
 * Faller tilbake på en blank utfylling dersom innsendingen ikke kan brukes direkte, f.eks. ved for mange registrerte dager - brukers meldekort knyttes likevel til behandlingen.
 */
private fun BrukersMeldekort.tilUtfyltMeldeperiodeForAkkumulering(): UtfyltMeldeperiode {
    return if (antallDagerRegistrert <= meldeperiode.maksAntallDagerForMeldeperiode) {
        tilUtfyltMeldeperiode()
    } else {
        meldeperiode.tilUtfyltMeldeperiode()
    }
}
