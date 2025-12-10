package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.sak.Sak

fun Sak.validerOpprettMeldekortbehandling(kjedeId: MeldeperiodeKjedeId): Either<ValiderOpprettMeldekortbehandlingFeil, Unit> {
    val meldeperiodekjede = this.meldeperiodeKjeder.hentMeldeperiodekjedeForKjedeId(kjedeId)!!
    val meldeperiode = meldeperiodekjede.hentSisteMeldeperiode()

    if (meldeperiode.ingenDagerGirRett) {
        return ValiderOpprettMeldekortbehandlingFeil.INGEN_DAGER_GIR_RETT.left()
    }

    val åpenBehandling = this.meldekortbehandlinger.åpenMeldekortBehandling

    if (åpenBehandling != null) {
        /**
         *  Det er et gyldig valg å gjenopprette en behandling som har blitt lagt tilbake på samme kjede.
         *  Denne vil ha status KLAR_TIL_BEHANDLING
         *
         *  Vi tillater ikke å faktisk opprette en ny behandling dersom det finnes en åpen behandling.
         *  */
        if (åpenBehandling.kjedeId != kjedeId || åpenBehandling.status != MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING) {
            return ValiderOpprettMeldekortbehandlingFeil.HAR_ÅPEN_BEHANDLING.left()
        }
    }

    val erFørsteBehandlingPåSaken = this.meldekortbehandlinger.isEmpty()
    val erFørsteMeldeperiodeMedRettPåSaken = meldeperiode == this.meldeperiodeKjeder
        .meldeperiodeKjederMedRett.first()
        .hentSisteMeldeperiode()

    if (erFørsteBehandlingPåSaken && !erFørsteMeldeperiodeMedRettPåSaken) {
        return ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_FØRSTE_KJEDE.left()
    }

    this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjedeMedRett(kjedeId)?.also { foregåendeMeldeperiodekjede ->
        this.meldekortbehandlinger.hentMeldekortBehandlingerForKjede(foregåendeMeldeperiodekjede.kjedeId)
            .also { behandlinger ->
                if (behandlinger.none { it.status == GODKJENT || it.status == AUTOMATISK_BEHANDLET }) {
                    return ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_NESTE_KJEDE.left()
                }
            }
    }

    return Unit.right()
}

enum class ValiderOpprettMeldekortbehandlingFeil {
    HAR_ÅPEN_BEHANDLING,
    MÅ_BEHANDLE_FØRSTE_KJEDE,
    MÅ_BEHANDLE_NESTE_KJEDE,
    INGEN_DAGER_GIR_RETT,
}
