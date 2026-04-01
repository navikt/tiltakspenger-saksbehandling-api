package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.beregning.beregnMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldeperiodebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldeperiodebehandlinger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock

suspend fun Sak.oppdaterMeldekort(
    kommando: OppdaterMeldekortbehandlingKommando,
    simuler: (suspend (Meldekortbehandling) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>),
    clock: Clock,
): Either<KanIkkeOppdatereMeldekortbehandling, Triple<Sak, MeldekortUnderBehandling, SimuleringMedMetadata?>> {
    val meldekort = this.meldekortbehandlinger.hentMeldekortbehandling(kommando.meldekortId) as MeldekortUnderBehandling

    val dager = kommando.meldeperioder.map {
        val meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjede(it.kjedeId)
        it.tilUtfyltMeldeperiode(meldeperiode)
    }

    val beregning = this.beregnMeldekort(
        meldekortIdSomBeregnes = kommando.meldekortId,
        meldeperioderSomBeregnes = dager,
        beregningstidspunkt = nå(clock),
    )

    return meldekort.oppdater(
        kommando = kommando,
        oppdatertePerioder = Meldeperiodebehandlinger(
            meldeperioder = dager.map {
                Meldeperiodebehandling(
                    dager = it,
                    brukersMeldekort = null,
                )
            },
            beregning = beregning,
        ),
        simuler = simuler,
        clock = clock,
    ).map { (meldekortbehandling, simulering) ->
        Triple(
            this.oppdaterMeldekortbehandling(meldekortbehandling),
            meldekortbehandling,
            simulering,
        )
    }
}
