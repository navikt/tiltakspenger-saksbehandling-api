package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.utbetalingskontroll

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.beregning.beregnMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock

/**
 * Beregner og simulerer meldekortbehandlingen på nytt slik saken ser ut nå, og setter resultatet som [Utbetalingskontroll] på behandlingen.
 * Kjøres når behandlingen sendes videre i flyten, altså til beslutter og ved iverksettelse.
 *
 * Flere meldekortbehandlinger kan være åpne på samme sak samtidig, og de påvirker hverandre gjennom de vedtatte meldeperiodeberegningene.
 * Blir en annen behandling iverksatt mellom saksbehandlers simulering og neste steg, er tallene saksbehandler så på ikke lenger de som ville blitt utbetalt.
 * [no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling] sammenligner kontrollen med simuleringen på behandlingen og stopper flyten ved avvik.
 *
 * Behandlingen persisteres ikke her; det er kallerens ansvar.
 */
suspend fun Sak.oppdaterUtbetalingskontrollForMeldekort(
    meldekortId: MeldekortId,
    simuler: suspend (Meldekortbehandling, Beregning) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>,
    clock: Clock,
): Either<KunneIkkeSimulere, Pair<Sak, Meldekortbehandling>> {
    val meldekortbehandling = this.hentMeldekortbehandling(meldekortId)!!

    val kontrollberegning = this.beregnMeldekort(
        meldekortIdSomBeregnes = meldekortbehandling.id,
        meldeperioderSomBeregnes = meldekortbehandling.meldeperioder.meldeperioder.map { it.dager },
        beregningstidspunkt = nå(clock),
    )

    val kontrollsimulering = simuler(meldekortbehandling, kontrollberegning).getOrElse { return it.left() }

    val oppdatertBehandling = meldekortbehandling.oppdaterUtbetalingskontroll(
        oppdatertKontroll = Utbetalingskontroll(
            beregning = kontrollberegning,
            simulering = kontrollsimulering.simulering,
        ),
        clock = clock,
    )

    return (this.oppdaterMeldekortbehandling(oppdatertBehandling) to oppdatertBehandling).right()
}
